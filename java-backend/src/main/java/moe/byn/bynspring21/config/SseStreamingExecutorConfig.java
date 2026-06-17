package moe.byn.bynspring21.config;

import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.entity.tool.BochaWebSearch;
import moe.byn.bynspring21.entity.tool.MemoryTools;
import moe.byn.bynspring21.entity.tool.PyodideCodeRunner;
import moe.byn.bynspring21.entity.tool.RagKnowledgeBaseTool;
import moe.byn.bynspring21.utils.ReasoningContentHelper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Configuration
public class SseStreamingExecutorConfig {

    private static final AtomicInteger STREAMING_COUNTER = new AtomicInteger(0);
    private static final AtomicInteger LIGHT_COUNTER = new AtomicInteger(0);

    @Bean
    @Qualifier("sseStreamingExecutor")
    public ExecutorService sseStreamingExecutor() {
        return new ThreadPoolExecutor(
                2, 10,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                r -> {
                    Thread t = new Thread(r, "sse-stream-" + STREAMING_COUNTER.incrementAndGet());
                    t.setDaemon(false);
                    return t;
                },
                (r, executor) -> {
                    log.error("sseStreamingExecutor 拒绝任务，队列已满");
                    throw new java.util.concurrent.RejectedExecutionException("SSE 流式任务队列已满，请稍后重试");
                }
        ) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                if (t != null) {
                    log.warn("sseStreamingExecutor 任务异常退出", t);
                }
                // 安全网：清理所有工具 ThreadLocal（幂等操作）
                BochaWebSearch.clearContext();
                RagKnowledgeBaseTool.clearContext();
                PyodideCodeRunner.clearContext();
                MemoryTools.clearContext();
                ReasoningContentHelper.clear();
            }
        };
    }

    @Bean
    @Qualifier("sseLightTaskExecutor")
    public ExecutorService sseLightTaskExecutor() {
        return new ThreadPoolExecutor(
                1, 3,
                30L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(50),
                r -> {
                    Thread t = new Thread(r, "sse-light-" + LIGHT_COUNTER.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                },
                (r, executor) -> {
                    log.error("sseLightTaskExecutor 拒绝任务，队列已满");
                    throw new java.util.concurrent.RejectedExecutionException("SSE 轻量任务队列已满");
                }
        );
    }
}
