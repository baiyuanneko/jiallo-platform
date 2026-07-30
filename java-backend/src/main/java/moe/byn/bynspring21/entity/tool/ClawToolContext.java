package moe.byn.bynspring21.entity.tool;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Claw-like 工具上下文（userId + sessionId + 安全级别 + 审批模式 + SSE emitter），
 * 工具执行时按当前用户当前会话定位工作区目录并按级别放行路径，审批模式与 emitter 用于工具调用前的人工审批确认
 */
public class ClawToolContext {

    private static final ThreadLocal<String> userIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> sessionIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<Integer> levelHolder = new ThreadLocal<>();
    private static final ThreadLocal<Integer> approvalModeHolder = new ThreadLocal<>();
    private static final ThreadLocal<SseEmitter> emitterHolder = new ThreadLocal<>();

    private ClawToolContext() {
    }

    public static void setContext(String userId, String sessionId, Integer level, Integer approvalMode, SseEmitter emitter) {
        userIdHolder.set(userId);
        sessionIdHolder.set(sessionId);
        levelHolder.set(level);
        approvalModeHolder.set(approvalMode);
        emitterHolder.set(emitter);
    }

    public static void clearContext() {
        userIdHolder.remove();
        sessionIdHolder.remove();
        levelHolder.remove();
        approvalModeHolder.remove();
        emitterHolder.remove();
    }

    public static String getUserId() {
        return userIdHolder.get();
    }

    public static String getSessionId() {
        return sessionIdHolder.get();
    }

    public static Integer getLevel() {
        return levelHolder.get();
    }

    public static Integer getApprovalMode() {
        return approvalModeHolder.get();
    }

    public static SseEmitter getEmitter() {
        return emitterHolder.get();
    }
}
