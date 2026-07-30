package moe.byn.bynspring21.entity.tool;

/**
 * Claw-like 工具上下文（userId + sessionId + 安全级别），工具执行时按当前用户当前会话定位工作区目录并按级别放行路径
 */
public class ClawToolContext {

    private static final ThreadLocal<String> userIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> sessionIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<Integer> levelHolder = new ThreadLocal<>();

    private ClawToolContext() {
    }

    public static void setContext(String userId, String sessionId, Integer level) {
        userIdHolder.set(userId);
        sessionIdHolder.set(sessionId);
        levelHolder.set(level);
    }

    public static void clearContext() {
        userIdHolder.remove();
        sessionIdHolder.remove();
        levelHolder.remove();
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
}
