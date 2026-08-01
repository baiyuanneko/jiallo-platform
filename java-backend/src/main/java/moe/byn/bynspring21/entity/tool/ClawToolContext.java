package moe.byn.bynspring21.entity.tool;

import java.util.function.BiConsumer;

/**
 * Claw-like 工具上下文（userId + sessionId + 安全级别 + 审批模式 + LiveStream 事件发布器），
 * 工具执行时按当前用户当前会话定位工作区目录并按级别放行路径，审批模式与发布器用于工具调用前的人工审批确认
 */
public class ClawToolContext {

    private static final ThreadLocal<String> userIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> sessionIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<Integer> levelHolder = new ThreadLocal<>();
    private static final ThreadLocal<Integer> approvalModeHolder = new ThreadLocal<>();
    private static final ThreadLocal<BiConsumer<String, String>> publisherHolder = new ThreadLocal<>();

    private ClawToolContext() {
    }

    public static void setContext(String userId, String sessionId, Integer level, Integer approvalMode, BiConsumer<String, String> publisher) {
        userIdHolder.set(userId);
        sessionIdHolder.set(sessionId);
        levelHolder.set(level);
        approvalModeHolder.set(approvalMode);
        publisherHolder.set(publisher);
    }

    public static void clearContext() {
        userIdHolder.remove();
        sessionIdHolder.remove();
        levelHolder.remove();
        approvalModeHolder.remove();
        publisherHolder.remove();
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

    public static BiConsumer<String, String> getPublisher() {
        return publisherHolder.get();
    }
}
