package moe.byn.bynspring21.entity.tool;

import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.exception.BynBaseException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class PyodideResultPendingManager {

    private final ConcurrentHashMap<String, PendingEntry> pendingMap = new ConcurrentHashMap<>();

    public record PyodideResult(String stdout, String stderr, String error, Integer exitCode) {}

    public static class PendingEntry {
        private final String userId;
        private final CompletableFuture<PyodideResult> future;

        public PendingEntry(String userId, CompletableFuture<PyodideResult> future) {
            this.userId = userId;
            this.future = future;
        }

        public String getUserId() {
            return userId;
        }

        public CompletableFuture<PyodideResult> getFuture() {
            return future;
        }
    }

    /**
     * 创建一个 pending 条目，返回 requestId
     */
    public String createPending(String userId) {
        String requestId = UUID.randomUUID().toString();
        CompletableFuture<PyodideResult> future = new CompletableFuture<>();
        pendingMap.put(requestId, new PendingEntry(userId, future));
        log.info("Pyodide pending created: requestId={}, userId={}", requestId, userId);
        return requestId;
    }

    /**
     * 阻塞等待结果，超时后抛异常
     */
    public PyodideResult waitForResult(String requestId, long timeoutSeconds) {
        PendingEntry entry = pendingMap.get(requestId);
        if (entry == null) {
            throw new BynBaseException("无效的 requestId: " + requestId);
        }
        try {
            return entry.getFuture().get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new BynBaseException("Pyodide 代码执行超时（" + timeoutSeconds + "秒）");
        } catch (Exception e) {
            throw new BynBaseException("Pyodide 代码执行异常: " + e.getMessage());
        } finally {
            pendingMap.remove(requestId);
        }
    }

    /**
     * 前端提交结果，完成 future
     */
    public void completeResult(String requestId, String userId, PyodideResult result) {
        PendingEntry entry = pendingMap.get(requestId);
        if (entry == null) {
            throw new BynBaseException("无效的 requestId: " + requestId);
        }
        if (!entry.getUserId().equals(userId)) {
            throw new BynBaseException("requestId 与当前用户不匹配");
        }
        entry.getFuture().complete(result);
        log.info("Pyodide result completed: requestId={}, userId={}", requestId, userId);
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
