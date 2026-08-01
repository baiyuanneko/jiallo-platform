package moe.byn.bynspring21.entity.tool;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.entity.base.ClawApprovalMode;
import moe.byn.bynspring21.exception.ToolWaitTimeoutException;
import moe.byn.bynspring21.service.ClawApprovalJudgeService;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Claw-like 工具调用审批装饰器：按当前上下文审批模式决定是否放行工具执行。
 * BYPASS 直接执行；MANUAL 每次人工确认；AUTO 先由审批模型判断危险性，危险才人工确认。
 * 非 claw 上下文（上下文未设置）直接放行，不影响普通聊天工具
 */
@Slf4j
public class ClawApprovalToolCallback implements ToolCallback {

    /**
     * 人工审批等待超时时间（秒），超时中止整条对话流
     */
    private static final long APPROVAL_TIMEOUT_SECONDS = 300;

    /**
     * 审批被拒时返回给 LLM 的工具结果（超时不再走此路径，而是中止对话流）
     */
    private static final String REJECTED_RESULT = "错误：用户拒绝了该操作";

    private final ToolCallback delegate;

    private final ClawApprovalJudgeService judgeService;

    private final ClawApprovalPendingManager pendingManager;

    public ClawApprovalToolCallback(ToolCallback delegate, ClawApprovalJudgeService judgeService,
                                    ClawApprovalPendingManager pendingManager) {
        this.delegate = delegate;
        this.judgeService = judgeService;
        this.pendingManager = pendingManager;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    @Override
    public String call(String toolInput) {
        String notApprovedResult = checkApproval(toolInput);
        if (notApprovedResult != null) {
            return notApprovedResult;
        }
        return delegate.call(toolInput);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        String notApprovedResult = checkApproval(toolInput);
        if (notApprovedResult != null) {
            return notApprovedResult;
        }
        return delegate.call(toolInput, toolContext);
    }

    /**
     * 按审批模式判断本次工具调用是否放行。
     * 返回 null 表示放行；否则返回应作为工具结果交给 LLM 的文本（拒绝文案或超时存档文案）
     */
    private String checkApproval(String toolInput) {
        Integer approvalMode = ClawToolContext.getApprovalMode();
        String userId = ClawToolContext.getUserId();
        // 非 claw 上下文或绕过审批：直接放行
        if (approvalMode == null || userId == null
                || approvalMode == ClawApprovalMode.BYPASS.getCode()) {
            return null;
        }
        String toolName = getToolDefinition().name();
        boolean needManualApproval;
        if (approvalMode == ClawApprovalMode.MANUAL.getCode()) {
            needManualApproval = true;
        } else {
            // 自动审批：先由审批模型判断危险性，危险才人工确认
            needManualApproval = judgeService.isDangerous(toolName, toolInput);
        }
        if (!needManualApproval) {
            return null;
        }
        return requestManualApproval(toolName, toolInput, userId);
    }

    /**
     * 发起人工审批：发送 claw_tool_approval_request SSE 事件，阻塞等待前端回传结果。
     * 返回 null 表示批准放行；否则返回应作为工具结果的文本（拒绝/超时文案）
     */
    private String requestManualApproval(String toolName, String toolInput, String userId) {
        BiConsumer<String, String> publisher = ClawToolContext.getPublisher();
        if (publisher == null) {
            log.warn("Claw 人工审批上下文缺少 publisher，视为拒绝: toolName={}", toolName);
            return REJECTED_RESULT;
        }
        String requestId = pendingManager.createPending(userId);
        try {
            String payload = JSON.toJSONString(Map.of(
                    "requestId", requestId,
                    "name", toolName,
                    "arguments", toolInput
            ));
            // 通过 LiveStream publisher 走帧日志 + fanout，断开/恢复的客户端重放后仍可审批
            publisher.accept("claw_tool_approval_request", payload);
            boolean approved;
            try {
                approved = pendingManager.waitForApproval(requestId, APPROVAL_TIMEOUT_SECONDS);
            } catch (ToolWaitTimeoutException e) {
                // 等待超时：触发流中止回调，整条流随取消链路终止。
                // 不能 rethrow——Spring AI 不会吞工具异常，抛出会绕过取消链路直接 fail 流；
                // 改为把超时文案作为正常工具结果返回，该轮 tool call/result 配对完整落库，
                // 循环在下一轮 LLM 调用前被 cancelFlag 截停，LLM 不会看到这条文本
                Consumer<String> abortCallback = ClawToolContext.getAbortCallback();
                if (abortCallback != null) {
                    abortCallback.accept("审批等待超时，对话已终止");
                }
                return e.getMessage();
            }
            return approved ? null : REJECTED_RESULT;
        } finally {
            // waitForApproval 内部已移除，此处兜底确保异常路径不泄漏 pending 条目
            pendingManager.removePending(requestId);
        }
    }
}
