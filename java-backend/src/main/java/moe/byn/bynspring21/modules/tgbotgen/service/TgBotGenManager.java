package moe.byn.bynspring21.modules.tgbotgen.service;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.modules.tgbotgen.bot.TgBotGenBot;
import moe.byn.bynspring21.modules.tgbotgen.config.TgBotGenProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Telegram Bot 生命周期管理：随主应用启动；配置变更后可通过 reload() 热重载，无需重启应用。
 * <p>
 * 注册/停止 Bot 的过程中会同步访问 api.telegram.org（registerBot 内部会调 clearWebhook），
 * 网络不可达时这些调用会长时间阻塞，因此所有生命周期操作都在独立的单线程执行器上异步执行，
 * 永不阻塞 HTTP 请求线程或应用启动线程。单线程执行器保证多次 reload 串行生效。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TgBotGenManager implements ApplicationRunner {

    private final TgBotGenConfigService configService;
    private final TgBotGenProperties properties;

    private final ExecutorService lifecycleExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "tgbotgen-lifecycle");
        thread.setDaemon(true);
        return thread;
    });

    private volatile BotSession botSession;

    @Override
    public void run(ApplicationArguments args) {
        reload();
    }

    /**
     * 异步热重载：立即返回，实际的停止+启动在后台线程串行执行。
     */
    public void reload() {
        lifecycleExecutor.submit(() -> {
            stop();
            start();
        });
    }

    private void start() {
        if (!configService.isEnabled()) {
            log.info("Telegram Bot 未启用（sys_config: tgBotEnabled != true），跳过启动");
            return;
        }
        String token = configService.getToken();
        if (token.isEmpty()) {
            log.warn("Telegram Bot 已启用但未配置 tgBotToken，跳过启动");
            return;
        }
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            TgBotGenBot bot = new TgBotGenBot(properties.buildBotOptions(), token, configService);
            botSession = botsApi.registerBot(bot);
            log.info("Telegram Bot 已注册，长轮询会话已启动");
        } catch (TelegramApiException e) {
            log.error("Telegram Bot 启动失败", e);
            botSession = null;
        }
    }

    private void stop() {
        BotSession session = botSession;
        botSession = null;
        if (session != null) {
            try {
                session.stop();
                log.info("Telegram Bot 已停止");
            } catch (Exception e) {
                log.warn("停止 Telegram Bot 会话时出错", e);
            }
        }
    }

    @PreDestroy
    public void destroy() {
        lifecycleExecutor.shutdownNow();
        stop();
    }
}
