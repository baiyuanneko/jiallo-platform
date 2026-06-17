package moe.byn.bynspring21.entity.tool;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
public class PyodideCodeRunner {

    private static final long TIMEOUT_SECONDS = 120;

    private static final ThreadLocal<SseEmitter> emitterHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> userIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<PyodideResultPendingManager.PyodideResult> lastStructuredResult = new ThreadLocal<>();

    @Autowired
    private PyodideResultPendingManager pendingManager;

    public static void setContext(SseEmitter emitter, String userId) {
        emitterHolder.set(emitter);
        userIdHolder.set(userId);
    }

    public static void clearContext() {
        emitterHolder.remove();
        userIdHolder.remove();
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

        SseEmitter emitter = emitterHolder.get();
        String userId = userIdHolder.get();

        if (emitter == null || userId == null) {
            return "错误：Pyodide 执行上下文未初始化";
        }

        String requestId = pendingManager.createPending(userId);

        // 发送 tool_execution_request SSE 事件，前端收到后执行 Pyodide
        try {
            String payload = JSON.toJSONString(Map.of(
                    "requestId", requestId,
                    "code", code
            ));
            emitter.send(SseEmitter.event().name("tool_execution_request").data(payload));
        } catch (IOException e) {
            log.error("SSE 发送 tool_execution_request 失败", e);
            pendingManager.removePending(requestId);
            return "错误：无法发送代码到前端执行";
        }

        // 阻塞等待前端执行结果
        PyodideResultPendingManager.PyodideResult result = pendingManager.waitForResult(requestId, TIMEOUT_SECONDS);

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
