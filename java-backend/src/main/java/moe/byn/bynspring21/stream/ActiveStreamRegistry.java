package moe.byn.bynspring21.stream;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 内存中的“直播流注册表”：为 agentic chat SSE 流式对话提供只追加的帧重放日志与多订阅者 fanout，
 * 使客户端在断开（关闭标签页 / 刷新）后可以列出并恢复进行中的流。
 *
 * <p>{@link LiveStream} 的所有变更方法均在实例自身加锁，保证 attach 重放与 publish 实时推送之间无竞态。
 */
@Slf4j
@Component
public class ActiveStreamRegistry {

    private final ConcurrentHashMap<String, LiveStream> streams = new ConcurrentHashMap<>();

    public LiveStream register(String streamUniqueKey, String userId, String sessionId,
                               Integer agentType, String modelId, Integer modelType,
                               String userMessagePreview) {
        LiveStream liveStream = new LiveStream(streamUniqueKey, userId, sessionId, agentType,
                modelId, modelType, System.currentTimeMillis(), userMessagePreview);
        streams.put(streamUniqueKey, liveStream);
        return liveStream;
    }

    public LiveStream get(String streamUniqueKey) {
        if (streamUniqueKey == null) {
            return null;
        }
        return streams.get(streamUniqueKey);
    }

    public void remove(String streamUniqueKey) {
        if (streamUniqueKey == null) {
            return;
        }
        streams.remove(streamUniqueKey);
    }

    public List<LiveStream> listByUserId(String userId) {
        List<LiveStream> result = new ArrayList<>();
        for (LiveStream ls : streams.values()) {
            if (userId.equals(ls.getUserId()) && !ls.isFinished()) {
                result.add(ls);
            }
        }
        return result;
    }

    /**
     * 移除 startedAt 早于 15 分钟（与 SseEmitter 900_000ms 超时一致）的条目，避免泄漏。
     */
    @Scheduled(fixedDelay = 300_000)
    public void sweepStaleStreams() {
        long cutoff = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(15);
        Iterator<Map.Entry<String, LiveStream>> it = streams.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, LiveStream> entry = it.next();
            LiveStream ls = entry.getValue();
            if (ls.getStartedAt() < cutoff) {
                log.info("清理过期直播流: {}, startedAt={}", entry.getKey(), ls.getStartedAt());
                ls.completeStale();
                it.remove();
            }
        }
    }

    /**
     * 一条进行中的 SSE 直播流：只追加的帧日志 + 多订阅者 fanout + 终态管理。
     * 所有变更方法均在实例自身加锁（可重入），保证重放与实时推送无竞态。
     */
    public static final class LiveStream {

        private static final Logger log = LoggerFactory.getLogger(LiveStream.class);

        private final String streamUniqueKey;
        private final String userId;
        private final String sessionId;
        private final Integer agentType;
        private final String modelId;
        private final Integer modelType;
        private final long startedAt;
        private final String userMessagePreview;

        private final List<Frame> frames = new ArrayList<>();
        private final List<SseEmitter> subscribers = new ArrayList<>();
        private long nextSeq = 0;
        private volatile boolean finished = false;

        private static final int MAX_SUBSCRIBERS = 8;

        LiveStream(String streamUniqueKey, String userId, String sessionId, Integer agentType,
                   String modelId, Integer modelType, long startedAt, String userMessagePreview) {
            this.streamUniqueKey = streamUniqueKey;
            this.userId = userId;
            this.sessionId = sessionId;
            this.agentType = agentType;
            this.modelId = modelId;
            this.modelType = modelType;
            this.startedAt = startedAt;
            this.userMessagePreview = userMessagePreview;
        }

        public String getStreamUniqueKey() {
            return streamUniqueKey;
        }

        public String getUserId() {
            return userId;
        }

        public String getSessionId() {
            return sessionId;
        }

        public Integer getAgentType() {
            return agentType;
        }

        public String getModelId() {
            return modelId;
        }

        public Integer getModelType() {
            return modelType;
        }

        public long getStartedAt() {
            return startedAt;
        }

        public String getUserMessagePreview() {
            return userMessagePreview;
        }

        public boolean isFinished() {
            return finished;
        }

        /**
         * 附加新订阅者：按序重放所有已记录帧，再加入列表接收实时推送；流已结束则重放后直接 complete。
         * 超过订阅者上限时拒绝并 complete 该 emitter，防止无界增长导致内存泄漏。
         */
        public synchronized void attach(SseEmitter emitter) {
            for (Frame frame : frames) {
                try {
                    emitter.send(buildEvent(frame.name(), frame.data()));
                } catch (IOException e) {
                    log.warn("重放帧时订阅者已断开, streamUniqueKey={}, seq={}", streamUniqueKey, frame.seq(), e);
                    return;
                }
            }
            if (finished) {
                try {
                    emitter.complete();
                } catch (Exception e) {
                    log.warn("完成已结束流的订阅者时异常, streamUniqueKey={}", streamUniqueKey, e);
                }
                return;
            }
            if (subscribers.size() >= MAX_SUBSCRIBERS) {
                log.warn("订阅者数量已达上限，拒绝新订阅: streamUniqueKey={}, count={}", streamUniqueKey, subscribers.size());
                try {
                    emitter.complete();
                } catch (Exception e) {
                    log.warn("complete 拒绝订阅者时异常, streamUniqueKey={}", streamUniqueKey, e);
                }
                return;
            }
            subscribers.add(emitter);
        }

        /**
         * 追加一帧并 fanout 给所有订阅者。name 为 null 表示默认（无名）事件。
         * 任一订阅者 IOException 仅移除该订阅者，绝不影响其他订阅者或中断循环。
         */
        public synchronized void publish(String name, String data) {
            if (finished) {
                log.warn("流已结束，忽略后续发布, streamUniqueKey={}, name={}", streamUniqueKey, name);
                return;
            }
            long seq = nextSeq++;
            frames.add(new Frame(seq, name, data));
            Iterator<SseEmitter> it = subscribers.iterator();
            while (it.hasNext()) {
                SseEmitter sub = it.next();
                try {
                    sub.send(buildEvent(name, data));
                } catch (IOException e) {
                    log.warn("订阅者连接断开，移除, streamUniqueKey={}, seq={}", streamUniqueKey, seq, e);
                    it.remove();
                }
            }
        }

        public synchronized void finish(String name, String data) {
            if (finished) {
                return;
            }
            publish(name, data);
            finished = true;
            completeAndClearSubscribers();
        }

        public synchronized void fail(String message) {
            if (finished) {
                return;
            }
            publish("error", message);
            finished = true;
            completeAndClearSubscribers();
        }

        private synchronized void completeStale() {
            if (finished) {
                return;
            }
            publish("error", "流已超时清理");
            finished = true;
            completeAndClearSubscribers();
        }

        private void completeAndClearSubscribers() {
            for (SseEmitter sub : subscribers) {
                try {
                    sub.complete();
                } catch (Exception e) {
                    log.warn("complete 订阅者时异常, streamUniqueKey={}", streamUniqueKey, e);
                }
            }
            subscribers.clear();
        }

        private static SseEmitter.SseEventBuilder buildEvent(String name, String data) {
            SseEmitter.SseEventBuilder builder = SseEmitter.event();
            if (name != null) {
                builder.name(name);
            }
            return builder.data(data);
        }
    }

    public record Frame(long seq, String name, String data) {}
}
