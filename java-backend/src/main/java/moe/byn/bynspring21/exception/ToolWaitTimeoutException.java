package moe.byn.bynspring21.exception;

import java.io.Serial;

/**
 * 工具等待超时异常：Pyodide 执行结果 / Claw-like 人工审批等待前端回传超时。
 * 继承 BynBaseException，使其 message 被 Spring AI 作为工具结果文本持久化到对话历史；
 * 工具层捕获后触发流中止回调，整条流随之中断（与用户主动停止语义对等）。
 */
public class ToolWaitTimeoutException extends BynBaseException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ToolWaitTimeoutException(String msg) {
        super(msg);
    }
}
