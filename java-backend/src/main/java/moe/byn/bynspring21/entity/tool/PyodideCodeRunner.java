package moe.byn.bynspring21.entity.tool;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.exception.ToolWaitTimeoutException;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Slf4j
@Component
public class PyodideCodeRunner {

    private static final long TIMEOUT_SECONDS = 120;

    private static final ThreadLocal<BiConsumer<String, String>> publisherHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> userIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<Consumer<String>> abortCallbackHolder = new ThreadLocal<>();
    private static final ThreadLocal<PyodideResultPendingManager.PyodideResult> lastStructuredResult = new ThreadLocal<>();

    @Autowired
    private PyodideResultPendingManager pendingManager;

    public static void setContext(BiConsumer<String, String> publisher, String userId, Consumer<String> abortCallback) {
        publisherHolder.set(publisher);
        userIdHolder.set(userId);
        abortCallbackHolder.set(abortCallback);
    }

    public static void clearContext() {
        publisherHolder.remove();
        userIdHolder.remove();
        abortCallbackHolder.remove();
        lastStructuredResult.remove();
    }

    public static PyodideResultPendingManager.PyodideResult consumeLastStructuredResult() {
        PyodideResultPendingManager.PyodideResult result = lastStructuredResult.get();
        lastStructuredResult.remove();
        return result;
    }

    @Tool(description = "在浏览器 Pyodide 环境中执行 Python 代码并返回执行结果。" +
            "当需要运行 Python 代码进行计算、数据处理、数学运算等场景时使用此工具。" +
            "代码将在前端的 Pyodide (WebAssembly) 环境中执行。" +
            "Python 执行环境已挂载到用户本地工作区目录（工作路径为 /workspace），你可以通过文件路径直接读取和操作用户本地的文件。")
    public String pyodideCodeRunner(
            @ToolParam(description = "要执行的 Python 代码", required = true) String code) {

        BiConsumer<String, String> publisher = publisherHolder.get();
        String userId = userIdHolder.get();

        if (publisher == null || userId == null) {
            return "错误：Pyodide 执行上下文未初始化";
        }

        String requestId = pendingManager.createPending(userId);

        // 发送 tool_execution_request SSE 事件，前端收到后执行 Pyodide
        // 通过 LiveStream publisher 走帧日志 + fanout，断开/恢复的客户端重放后仍可应答
        String payload = JSON.toJSONString(Map.of(
                "requestId", requestId,
                "code", code
        ));
        publisher.accept("tool_execution_request", payload);

        // 阻塞等待前端执行结果；超时触发流中止回调（整条流随取消链路终止）。
        // 不能 rethrow——Spring AI 不会吞工具异常，抛出会绕过取消链路直接 fail 流；
        // 改为把超时文案作为正常工具结果返回，该轮 tool call/result 配对完整落库，
        // 循环在下一轮 LLM 调用前被 cancelFlag 截停，LLM 不会看到这条文本
        PyodideResultPendingManager.PyodideResult result;
        try {
            result = pendingManager.waitForResult(requestId, TIMEOUT_SECONDS);
        } catch (ToolWaitTimeoutException e) {
            Consumer<String> abortCallback = abortCallbackHolder.get();
            if (abortCallback != null) {
                abortCallback.accept("代码执行等待超时，对话已终止");
            }
            return e.getMessage();
        }

        // 存入 ThreadLocal 供 ObservableToolCallingManager 消费结构化结果
        lastStructuredResult.set(result);

        // 返回给 LLM 的文本
        StringBuilder sb = new StringBuilder();
        if (result.stdout() != null && !result.stdout().isEmpty()) {
            sb.append("stdout:\n").append(result.stdout());
        }
        if (result.stderr() != null && !result.stderr().isEmpty()) {
            if (!sb.isEmpty()) sb.append("\n");
            sb.append("stderr:\n").append(result.stderr());
        }
        if (result.error() != null && !result.error().isEmpty()) {
            if (!sb.isEmpty()) sb.append("\n");
            sb.append("error:\n").append(result.error());
        }
        if (result.exitCode() != null && result.exitCode() != 0) {
            if (!sb.isEmpty()) sb.append("\n");
            sb.append("exitCode: ").append(result.exitCode());
        }

        return sb.isEmpty() ? "代码执行完成（无输出）" : sb.toString();
    }
}
