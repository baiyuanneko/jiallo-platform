package moe.byn.bynspring21.modules.tgbotgen.bot;

import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.modules.tgbotgen.service.TgBotGenConfigService;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Telegram 长轮询 Bot。当前无实际业务功能：
 * 仅当消息文本包含 TEST_TRIGGER 时回复 TEST_REPLY（用于连通性验证）。
 */
@Slf4j
public class TgBotGenBot extends TelegramLongPollingBot {

    public static final String TEST_TRIGGER = "jiallo-tgbotgen-test";
    public static final String TEST_REPLY = "jiallo-tgbotgen-test-reply";

    private final TgBotGenConfigService configService;

    public TgBotGenBot(DefaultBotOptions options, String token, TgBotGenConfigService configService) {
        super(options, token);
        this.configService = configService;
    }

    /**
     * TelegramBotsApi.registerBot() 会在调用线程同步校验 getBotUsername() 非空，
     * 因此这里不能通过网络请求（getMe）获取用户名——网络不可达时会阻塞
     * admin 配置保存的请求线程或应用启动线程。长轮询本身不依赖用户名，返回固定占位值即可。
     */
    @Override
    public String getBotUsername() {
        return "jiallo-tgbotgen-bot";
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update == null || !update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }
        Message message = update.getMessage();
        Long chatId = message.getChatId();
        if (!configService.isChatAllowed(chatId)) {
            log.debug("ChatId [{}] 不在允许范围内，忽略消息", chatId);
            return;
        }
        if (message.getText().contains(TEST_TRIGGER)) {
            try {
                execute(new SendMessage(chatId.toString(), TEST_REPLY));
            } catch (TelegramApiException e) {
                log.error("回复 Telegram 消息失败, chatId={}", chatId, e);
            }
        }
    }
}
