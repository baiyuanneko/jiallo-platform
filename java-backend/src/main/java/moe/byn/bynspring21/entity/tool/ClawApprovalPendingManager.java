package moe.byn.bynspring21.entity.tool;

import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.exception.BynBaseException;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Claw-like 工具调用人工审批 pending 管理器：工具执行线程创建 pending 并阻塞等待，
 * 前端通过回传端点提交审批结果完成 future，超时视为拒绝
 */
@Slf4j
@Component
public class ClawApprovalPendingManager {

    private final ConcurrentHashMap<String, PendingEntry> pendingMap = new ConcurrentHashMap<>();

    public static class PendingEntry {
        private final String userId;
        private final CompletableFuture<Boolean> future;

        public PendingEntry(String userId, CompletableFuture<Boolean> future) {
            this.userId = userId;
            this.future = future;
        }

        public String getUserId() {
            return userId;
        }

        public CompletableFuture<Boolean> getFuture() {
            return future;
        }
    }

    /**
     * 创建一个 pending 条目，返回 requestId
     */
    public String createPending(String userId) {
        String requestId = UUID.randomUUID().toString();
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        pendingMap.put(requestId, new PendingEntry(userId, future));
        log.info("Claw approval pending created: requestId={}, userId={}", requestId, userId);
        return requestId;
    }

    /**
     * 阻塞等待审批结果，超时或异常均视为拒绝
     */
    public boolean waitForApproval(String requestId, long timeoutSeconds) {
        PendingEntry entry = pendingMap.get(requestId);
        if (entry == null) {
            throw new BynBaseException("无效的 requestId: " + requestId);
        }
        try {
            return Boolean.TRUE.equals(entry.getFuture().get(timeoutSeconds, TimeUnit.SECONDS));
        } catch (TimeoutException e) {
            log.warn("Claw approval 等待超时（{}秒），视为拒绝: requestId={}", timeoutSeconds, requestId);
            return false;
        } catch (Exception e) {
            log.warn("Claw approval 等待异常，视为拒绝: requestId={}, error={}", requestId, e.getMessage());
            return false;
        } finally {
            pendingMap.remove(requestId);
        }
    }

    /**
     * 前端提交审批结果，完成 future；userId 不匹配时直接抛异常，不影响原审批
     */
    public void completeApproval(String requestId, String userId, boolean approved) {
        PendingEntry entry = pendingMap.get(requestId);
        if (entry == null) {
            throw new BynBaseException("无效的 requestId: " + requestId);
        }
        if (!entry.getUserId().equals(userId)) {
            // 不匹配直接抛异常，不动 future，避免合法用户的审批被旁人判拒
            throw new BynBaseException("requestId 与当前用户不匹配");
        }
        entry.getFuture().complete(approved);
        log.info("Claw approval completed: requestId={}, userId={}, approved={}", requestId, userId, approved);
    }

    /**
     * 清理超时/错误的 pending 条目
     */
    public void removePending(String requestId) {
        PendingEntry entry = pendingMap.remove(requestId);
        if (entry != null) {
            entry.getFuture().cancel(true);
        }
    }
}
