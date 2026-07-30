package moe.byn.bynspring21.service;

/**
 * Claw-like 工具调用危险性评判服务（自动审批模式下由 LLM 审批模型判断）
 */
public interface ClawApprovalJudgeService {

    /**
     * 判断一次工具调用是否危险：危险返回 true（需人工确认），安全返回 false（直接执行）。
     * 未配置审批模型、LLM 调用异常或输出不可解析时均按危险处理（降级人工确认）
     */
    boolean isDangerous(String toolName, String arguments);
}
