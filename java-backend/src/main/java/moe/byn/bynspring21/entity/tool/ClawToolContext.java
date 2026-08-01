package moe.byn.bynspring21.entity.tool;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Claw-like 工具上下文（userId + 安全级别 + 审批模式 + LiveStream 事件发布器 + 流中止回调），
 * 工具执行时按当前用户定位工作区目录（用户级隔离）并按级别放行路径，审批模式与发布器用于工具调用前的人工审批确认，
 * 审批等待超时时通过中止回调中断整条对话流
 */
public class ClawToolContext {

    private static final ThreadLocal<String> userIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<Integer> levelHolder = new ThreadLocal<>();
    private static final ThreadLocal<Integer> approvalModeHolder = new ThreadLocal<>();
    private static final ThreadLocal<BiConsumer<String, String>> publisherHolder = new ThreadLocal<>();
    private static final ThreadLocal<Consumer<String>> abortCallbackHolder = new ThreadLocal<>();

    private ClawToolContext() {
    }

    public static void setContext(String userId, Integer level, Integer approvalMode,
                                  BiConsumer<String, String> publisher, Consumer<String> abortCallback) {
        userIdHolder.set(userId);
        levelHolder.set(level);
        approvalModeHolder.set(approvalMode);
        publisherHolder.set(publisher);
        abortCallbackHolder.set(abortCallback);
    }

    public static void clearContext() {
        userIdHolder.remove();
        levelHolder.remove();
        approvalModeHolder.remove();
        publisherHolder.remove();
        abortCallbackHolder.remove();
    }

    public static String getUserId() {
        return userIdHolder.get();
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

    public static Consumer<String> getAbortCallback() {
        return abortCallbackHolder.get();
    }
}
