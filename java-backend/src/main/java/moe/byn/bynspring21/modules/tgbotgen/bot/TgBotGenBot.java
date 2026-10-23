package moe.byn.bynspring21.modules.tgbotgen.bot;

import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.mapper.TgChatLogMapper;
import moe.byn.bynspring21.modules.tgbotgen.entity.TgChatLog;
import moe.byn.bynspring21.modules.tgbotgen.service.TgBotAgentService;
import moe.byn.bynspring21.modules.tgbotgen.service.TgBotGenConfigService;
import moe.byn.bynspring21.modules.tgbotgen.service.TgBotWebSearchService;
import moe.byn.bynspring21.modules.tgbotgen.service.TgChatLogService;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.DeleteMyCommands;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScope;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeAllGroupChats;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Telegram 长轮询 Bot。功能按后台开关启用：
 * <ul>
 *   <li>消息记录：通过 ChatId 过滤的消息（含事件/编辑消息及机器人自身回复）写入 tg_chat_log</li>
 *   <li>/websearch [--deepseek|--bocha] &lt;内容&gt;：网页搜索，默认 DeepSeek</li>
 *   <li>/chat &lt;内容&gt;（别名 /ai、/llm）：一次性智能体问答（联网搜索、阅读网页、日期时间）</li>
 *   <li>/rank：群活跃成员排行（依赖消息记录）</li>
 * </ul>
 * 长轮询回调串行执行，耗时命令统一交给有界线程池异步处理，避免阻塞后续消息的接收与记录。
 */
@Slf4j
public class TgBotGenBot extends TelegramLongPollingBot {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    /** 单条 Telegram 消息上限 4096 字符，留出余量 */
    private static final int MAX_MESSAGE_LENGTH = 4000;

    /** /websearch、/chat 每人每聊天的冷却时间 */
    private static final long COMMAND_COOLDOWN_MS = 15_000;

    private static final int COMMAND_POOL_SIZE = 3;

    private static final int COMMAND_QUEUE_CAPACITY = 10;

    private static final int RANK_ALL_LIMIT = 12;

    private static final int RANK_RECENT_LIMIT = 8;

    private static final int RANK_RECENT_DAYS = 14;

    private final TgBotGenConfigService.Settings settings;

    private final TgChatLogService chatLogService;

    private final TgBotWebSearchService webSearchService;

    private final TgBotAgentService agentService;

    private final ThreadPoolExecutor commandExecutor;

    /** key: command:chatId:senderId，value: 上次触发该命令的时间戳 */
    private final ConcurrentHashMap<String, Long> commandCooldowns = new ConcurrentHashMap<>();

    /** 通过 getMe 异步获取的真实用户名；获取前为空，此时带 @ 后缀的命令一律忽略 */
    private volatile String resolvedUsername;

    public TgBotGenBot(DefaultBotOptions options, String token, TgBotGenConfigService.Settings settings,
                       TgChatLogService chatLogService, TgBotWebSearchService webSearchService,
                       TgBotAgentService agentService) {
        super(options, token);
        this.settings = settings;
        this.chatLogService = chatLogService;
        this.webSearchService = webSearchService;
        this.agentService = agentService;
        AtomicInteger threadIndex = new AtomicInteger();
        this.commandExecutor = new ThreadPoolExecutor(COMMAND_POOL_SIZE, COMMAND_POOL_SIZE, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(COMMAND_QUEUE_CAPACITY), r -> {
            Thread thread = new Thread(r, "tgbotgen-command-" + threadIndex.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * TelegramBotsApi.registerBot() 会在调用线程同步校验 getBotUsername() 非空，
     * 因此这里不能通过网络请求（getMe）获取用户名——网络不可达时会阻塞
     * admin 配置保存的请求线程或应用启动线程。长轮询本身不依赖用户名，返回固定占位值即可；
     * 命令 @ 后缀匹配使用注册后异步获取的 resolvedUsername。
     */
    @Override
    public String getBotUsername() {
        return "jiallo-tgbotgen-bot";
    }

    public void setResolvedUsername(String username) {
        this.resolvedUsername = username;
    }

    /**
     * 按已启用功能注册 Telegram 命令菜单（覆盖 BotFather 中手动设置的命令），别名不进菜单：
     * 默认范围（私聊等）注册已启用的 /websearch、/chat 及始终可用的 /help、/about；
     * 所有群聊范围在开启活跃排行时额外注册 /rank —— 群聊范围优先级高于默认范围且会整体替代默认菜单，
     * 因此群聊菜单需包含全部命令；未开启排行时删除群聊范围，群聊回落到默认菜单。
     * 失败只记日志，不影响机器人运行
     */
    public void registerCommands() {
        List<BotCommand> baseCommands = new ArrayList<>();
        if (settings.webSearchEnabled()) {
            baseCommands.add(new BotCommand("websearch", "网络搜索（默认 DeepSeek，可加 --bocha）"));
        }
        if (settings.chatEnabled()) {
            baseCommands.add(new BotCommand("chat", "智能问答（可联网搜索、阅读网页）"));
        }
        boolean rankAvailable = settings.rankEnabled() && settings.chatLogEnabled();
        List<BotCommand> commonCommands = List.of(
                new BotCommand("help", "查看可用命令"),
                new BotCommand("about", "关于本机器人"));

        List<BotCommand> defaultCommands = new ArrayList<>(baseCommands);
        defaultCommands.addAll(commonCommands);
        applyCommands(defaultCommands, new BotCommandScopeDefault(), "默认");

        List<BotCommand> groupCommands = new ArrayList<>();
        if (rankAvailable) {
            groupCommands.addAll(baseCommands);
            groupCommands.add(new BotCommand("rank", "群活跃成员排行"));
            groupCommands.addAll(commonCommands);
        }
        applyCommands(groupCommands, new BotCommandScopeAllGroupChats(), "群聊");
    }

    /**
     * 命令列表非空时设置，为空时删除该范围的命令菜单
     */
    private void applyCommands(List<BotCommand> commands, BotCommandScope scope, String scopeName) {
        try {
            if (commands.isEmpty()) {
                execute(new DeleteMyCommands(scope, null));
            } else {
                execute(new SetMyCommands(commands, scope, null));
            }
            log.info("Telegram 命令菜单已更新（{}范围）: {}", scopeName,
                    commands.stream().map(c -> "/" + c.getCommand()).toList());
        } catch (TelegramApiException e) {
            log.warn("更新 Telegram 命令菜单失败（{}范围）", scopeName, e);
        }
    }

    /**
     * 停止命令线程池（机器人停止/热重载时调用）
     */
    public void shutdown() {
        commandExecutor.shutdownNow();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update == null) {
            return;
        }
        if (update.hasEditedMessage()) {
            Message edited = update.getEditedMessage();
            if (settings.chatLogEnabled() && settings.isChatAllowed(edited.getChatId())) {
                recordEdited(edited);
            }
            return;
        }
        if (!update.hasMessage()) {
            return;
        }
        Message message = update.getMessage();
        if (!settings.isChatAllowed(message.getChatId())) {
            log.debug("ChatId [{}] 不在允许范围内，忽略消息", message.getChatId());
            return;
        }
        if (settings.chatLogEnabled()) {
            recordIncoming(message);
        }
        if (message.hasText()) {
            handleText(message);
        }
    }

    // ==================== 命令处理 ====================

    private void handleText(Message message) {
        String text = message.getText();
        String command = parseCommand(text);
        if (command == null) {
            return;
        }
        String[] parts = text.strip().split("\\s+", 2);
        String args = parts.length > 1 ? parts[1].strip() : "";
        switch (command) {
            case "/start", "/help" -> sendText(message, message.getMessageId(), buildHelpText());
            case "/about" -> sendText(message, message.getMessageId(), buildAboutText());
            case "/websearch" -> handleWebSearch(message, args);
            case "/chat", "/ai", "/llm" -> handleChat(message, args);
            case "/rank" -> handleRank(message);
            default -> {
                // 未知命令（可能属于其他机器人），忽略
            }
        }
    }

    private void handleWebSearch(Message message, String args) {
        if (!settings.webSearchEnabled()) {
            sendText(message, message.getMessageId(), "网络搜索功能未启用");
            return;
        }
        TgBotWebSearchService.Provider provider = TgBotWebSearchService.Provider.DEEPSEEK;
        String query = args;
        if (args.startsWith("--")) {
            String[] flagAndQuery = args.split("\\s+", 2);
            switch (flagAndQuery[0].toLowerCase()) {
                case "--deepseek" -> provider = TgBotWebSearchService.Provider.DEEPSEEK;
                case "--bocha" -> provider = TgBotWebSearchService.Provider.BOCHA;
                default -> {
                    sendText(message, message.getMessageId(), "未知参数：" + flagAndQuery[0] + "\n" + WEB_SEARCH_USAGE);
                    return;
                }
            }
            query = flagAndQuery.length > 1 ? flagAndQuery[1].strip() : "";
        }
        if (query.isEmpty()) {
            sendText(message, message.getMessageId(), WEB_SEARCH_USAGE);
            return;
        }
        if (!acquireCooldown(message, "/websearch")) {
            return;
        }
        final TgBotWebSearchService.Provider finalProvider = provider;
        final String finalQuery = query;
        runAsync(message, "🔍 正在搜索…", () -> webSearchService.search(finalProvider, finalQuery));
    }

    private void handleChat(Message message, String args) {
        if (!settings.chatEnabled()) {
            sendText(message, message.getMessageId(), "智能问答功能未启用");
            return;
        }
        if (args.isEmpty()) {
            sendText(message, message.getMessageId(), "用法：/chat <内容>（别名 /ai、/llm）");
            return;
        }
        if (!acquireCooldown(message, "/chat")) {
            return;
        }
        runAsync(message, "🤔 正在思考…",
                () -> agentService.chat(settings.chatModelId(), settings.chatSystemPrompt(), args));
    }

    private void handleRank(Message message) {
        if (!settings.rankEnabled()) {
            sendText(message, message.getMessageId(), "活跃排行功能未启用");
            return;
        }
        if (!settings.chatLogEnabled()) {
            sendText(message, message.getMessageId(), "活跃排行需要先开启消息记录功能");
            return;
        }
        Chat chat = message.getChat();
        if (!Boolean.TRUE.equals(chat.isGroupChat()) && !Boolean.TRUE.equals(chat.isSuperGroupChat())) {
            sendText(message, message.getMessageId(), "活跃排行仅在群聊中可用");
            return;
        }
        runAsync(message, null, () -> buildRankText(chat.getId()));
    }

    private String buildRankText(Long chatId) {
        LocalDateTime since = LocalDateTime.now(ZONE).minusDays(RANK_RECENT_DAYS);
        StringBuilder sb = new StringBuilder();
        sb.append("【B").append(RANK_ALL_LIMIT).append("】所有记录范围内最活跃成员排行\n");
        appendRankLines(sb, chatLogService.rank(chatId, null, RANK_ALL_LIMIT));
        sb.append("\n【B").append(RANK_RECENT_LIMIT).append("】最近").append(RANK_RECENT_DAYS).append("日内最活跃成员排行\n");
        appendRankLines(sb, chatLogService.rank(chatId, since, RANK_RECENT_LIMIT));
        return sb.toString();
    }

    private static void appendRankLines(StringBuilder sb, List<TgChatLogMapper.RankItem> items) {
        if (items.isEmpty()) {
            sb.append("暂无记录\n");
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            TgChatLogMapper.RankItem item = items.get(i);
            String name = item.getSenderName() != null && !item.getSenderName().isBlank()
                    ? item.getSenderName() : "用户" + item.getSenderId();
            sb.append(i + 1).append("、").append(name).append("：").append(item.getMessageCount()).append(" 条\n");
        }
    }

    private String buildBotHeader() {
        return settings.botName() + "（更新日期：" + settings.botUpdateDate() + "）";
    }

    private String buildHelpText() {
        List<String> lines = new ArrayList<>();
        if (settings.webSearchEnabled()) {
            lines.add("/websearch [--deepseek|--bocha] <内容> — 网络搜索（默认 DeepSeek）");
        }
        if (settings.chatEnabled()) {
            lines.add("/chat <内容> — 智能问答（可联网搜索、阅读网页；别名 /ai、/llm）");
        }
        if (settings.rankEnabled() && settings.chatLogEnabled()) {
            lines.add("/rank — 群活跃成员排行");
        }
        lines.add("/help — 查看可用命令");
        lines.add("/about — 关于本机器人");
        return buildBotHeader() + "\n\n可用命令：\n" + String.join("\n", lines);
    }

    private String buildAboutText() {
        List<String> features = new ArrayList<>();
        if (settings.webSearchEnabled()) {
            features.add("网络搜索");
        }
        if (settings.chatEnabled()) {
            features.add("智能问答");
        }
        if (settings.rankEnabled() && settings.chatLogEnabled()) {
            features.add("活跃排行");
        }
        if (settings.chatLogEnabled()) {
            features.add("消息记录");
        }
        return settings.botName() + "\n"
                + "更新日期：" + settings.botUpdateDate() + "\n"
                + "已启用功能：" + (features.isEmpty() ? "无" : String.join("、", features)) + "\n"
                + "发送 /help 查看可用命令";
    }

    private static final String WEB_SEARCH_USAGE = "用法：/websearch [--deepseek|--bocha] <内容>（默认 DeepSeek）";

    /**
     * 解析命令：取第一个空白分隔的 token，去掉 @bot名 后缀。
     * 返回 null 表示不是本 bot 的命令（非命令文本，或 @ 指向其他 bot / 尚未获取到本 bot 用户名）。
     */
    private String parseCommand(String text) {
        if (!text.startsWith("/")) {
            return null;
        }
        String token = text.split("\\s+", 2)[0];
        int at = token.indexOf('@');
        if (at > 0) {
            String target = token.substring(at + 1);
            String username = resolvedUsername;
            if (username == null || !target.equalsIgnoreCase(username)) {
                return null;
            }
            token = token.substring(0, at);
        }
        return token.toLowerCase();
    }

    /**
     * 耗时命令冷却：同一聊天内同一发送者对同一命令 COMMAND_COOLDOWN_MS 内只能触发一次（各命令独立计时），冷却中回复剩余秒数
     */
    private boolean acquireCooldown(Message message, String command) {
        String key = command + ":" + message.getChatId() + ":"
                + (message.getFrom() != null ? message.getFrom().getId() : "anonymous");
        long now = System.currentTimeMillis();
        long[] remainingMs = {0};
        commandCooldowns.compute(key, (k, last) -> {
            if (last != null && now - last < COMMAND_COOLDOWN_MS) {
                remainingMs[0] = COMMAND_COOLDOWN_MS - (now - last);
                return last;
            }
            return now;
        });
        if (remainingMs[0] > 0) {
            sendText(message, message.getMessageId(), "操作过于频繁，请 " + ((remainingMs[0] + 999) / 1000) + " 秒后再试");
            return false;
        }
        if (commandCooldowns.size() > 1000) {
            commandCooldowns.values().removeIf(last -> now - last >= COMMAND_COOLDOWN_MS);
        }
        return true;
    }

    /**
     * 提交耗时任务到线程池：先发占位消息，完成后用结果编辑占位消息（超长部分追加发送）；线程池满时回复繁忙
     */
    private void runAsync(Message source, String placeholder, Supplier<String> task) {
        try {
            commandExecutor.execute(() -> {
                Integer placeholderId = placeholder != null ? sendRaw(source.getChatId(), source.getMessageId(), placeholder) : null;
                String result;
                try {
                    result = task.get();
                } catch (Exception e) {
                    log.error("Telegram 命令处理失败, chatId={}", source.getChatId(), e);
                    result = "处理失败，请稍后重试";
                }
                deliver(source, placeholderId, result);
            });
        } catch (RejectedExecutionException e) {
            sendText(source, source.getMessageId(), "机器人繁忙，请稍后再试");
        }
    }

    private void deliver(Message source, Integer placeholderId, String result) {
        List<String> parts = splitMessage(result);
        int start = 0;
        if (placeholderId != null) {
            if (editText(source.getChatId(), placeholderId, parts.get(0))) {
                recordOutgoing(source.getChat(), placeholderId, parts.get(0));
                start = 1;
            }
        }
        for (int i = start; i < parts.size(); i++) {
            sendText(source, source.getMessageId(), parts.get(i));
        }
    }

    private static List<String> splitMessage(String text) {
        String content = text == null || text.isBlank() ? "（无内容）" : text;
        List<String> parts = new ArrayList<>();
        while (content.length() > MAX_MESSAGE_LENGTH) {
            int cut = content.lastIndexOf('\n', MAX_MESSAGE_LENGTH);
            if (cut < MAX_MESSAGE_LENGTH / 2) {
                cut = MAX_MESSAGE_LENGTH;
            }
            parts.add(content.substring(0, cut));
            content = content.substring(cut).stripLeading();
        }
        parts.add(content);
        return parts;
    }

    // ==================== 消息发送 ====================

    /**
     * 发送文本并在开启消息记录时记录为机器人消息
     */
    private void sendText(Message source, Integer replyToMessageId, String text) {
        Integer sentId = sendRaw(source.getChatId(), replyToMessageId, text);
        if (sentId != null) {
            recordOutgoing(source.getChat(), sentId, text);
        }
    }

    /**
     * 发送纯文本（关闭链接预览），返回发送成功的 message_id，失败返回 null
     */
    private Integer sendRaw(Long chatId, Integer replyToMessageId, String text) {
        SendMessage sendMessage = new SendMessage(chatId.toString(), text);
        sendMessage.setDisableWebPagePreview(true);
        if (replyToMessageId != null) {
            sendMessage.setReplyToMessageId(replyToMessageId);
            sendMessage.setAllowSendingWithoutReply(true);
        }
        try {
            Message sent = execute(sendMessage);
            return sent != null ? sent.getMessageId() : null;
        } catch (TelegramApiException e) {
            log.error("发送 Telegram 消息失败, chatId={}", chatId, e);
            return null;
        }
    }

    private boolean editText(Long chatId, Integer messageId, String text) {
        EditMessageText edit = new EditMessageText(text);
        edit.setChatId(chatId.toString());
        edit.setMessageId(messageId);
        edit.setDisableWebPagePreview(true);
        try {
            execute(edit);
            return true;
        } catch (TelegramApiException e) {
            log.warn("编辑 Telegram 消息失败，改为发送新消息, chatId={}", chatId, e);
            return false;
        }
    }

    // ==================== 消息记录 ====================

    private void recordIncoming(Message message) {
        String eventContent = buildEventContent(message);
        if (eventContent != null) {
            recordSafely(buildLog(message, toLocalTime(message.getDate()), "[event]", true, eventContent));
            return;
        }
        String content = extractContent(message);
        if (content == null) {
            log.debug("未知消息类型，未记录, chatId={}, messageId={}", message.getChatId(), message.getMessageId());
            return;
        }
        recordSafely(buildLog(message, toLocalTime(message.getDate()), content, false, null));
    }

    /**
     * 编辑消息作为特殊新行记录，不触发命令
     */
    private void recordEdited(Message message) {
        String text = message.hasText() ? message.getText() : extractContent(message);
        String edited = text != null ? "[edited] " + text : "[edited]";
        LocalDateTime sendTime = message.getEditDate() != null
                ? toLocalTime(message.getEditDate())
                : toLocalTime(message.getDate());
        recordSafely(buildLog(message, sendTime, "[edited]", true, edited));
    }

    private void recordOutgoing(Chat chat, Integer messageId, String text) {
        if (!settings.chatLogEnabled()) {
            return;
        }
        TgChatLog chatLog = new TgChatLog();
        chatLog.setChatType(chat.getType());
        chatLog.setChatId(chat.getId());
        chatLog.setSenderName(resolvedUsername != null ? "Bot@" + resolvedUsername : "Bot");
        chatLog.setSenderIsBot(true);
        chatLog.setSendTime(LocalDateTime.now(ZONE));
        chatLog.setContent(text);
        chatLog.setSpecialMessage(false);
        chatLog.setMessageId(messageId != null ? messageId.longValue() : null);
        recordSafely(chatLog);
    }

    private TgChatLog buildLog(Message message, LocalDateTime sendTime, String content,
                               boolean special, String eventContent) {
        User from = message.getFrom();
        TgChatLog chatLog = new TgChatLog();
        chatLog.setChatType(message.getChat().getType());
        chatLog.setChatId(message.getChatId());
        chatLog.setSenderId(from != null ? from.getId() : null);
        chatLog.setSenderName(from != null ? userDisplayName(from) : null);
        chatLog.setSenderIsBot(from != null && Boolean.TRUE.equals(from.getIsBot()));
        chatLog.setSendTime(sendTime);
        chatLog.setContent(content);
        chatLog.setSpecialMessage(special);
        chatLog.setEventContent(eventContent);
        chatLog.setMessageId(message.getMessageId() != null ? message.getMessageId().longValue() : null);
        return chatLog;
    }

    private void recordSafely(TgChatLog chatLog) {
        try {
            chatLogService.record(chatLog);
        } catch (Exception e) {
            log.error("Telegram 聊天记录落库失败, chatId={}", chatLog.getChatId(), e);
        }
    }

    /**
     * 服务消息判定：命中任一场景返回事件文案，普通内容消息返回 null
     */
    private static String buildEventContent(Message message) {
        if (message.getNewChatMembers() != null && !message.getNewChatMembers().isEmpty()) {
            StringJoiner names = new StringJoiner("、");
            message.getNewChatMembers().forEach(u -> names.add(String.valueOf(userDisplayName(u))));
            return "[event] " + names + " 加入了群组";
        }
        if (message.getLeftChatMember() != null) {
            return "[event] " + userDisplayName(message.getLeftChatMember()) + " 离开了群组";
        }
        if (message.getPinnedMessage() != null) {
            return "[event] 有人置顶了一条消息";
        }
        if (message.getNewChatTitle() != null) {
            return "[event] 群名改为「" + message.getNewChatTitle() + "」";
        }
        if (Boolean.TRUE.equals(message.getDeleteChatPhoto())) {
            return "[event] 群头像已删除";
        }
        if (message.getNewChatPhoto() != null && !message.getNewChatPhoto().isEmpty()) {
            return "[event] 群头像已更新";
        }
        if (message.getMigrateToChatId() != null) {
            log.warn("群组已迁移，请用新 chat_id={} 更新黑白名单配置", message.getMigrateToChatId());
            return "[event] 群组已迁移，新 chat_id=" + message.getMigrateToChatId();
        }
        if (message.getMigrateFromChatId() != null) {
            return "[event] 群组已迁移（来源 chat_id=" + message.getMigrateFromChatId() + "）";
        }
        if (message.getMessageAutoDeleteTimerChanged() != null) {
            return "[event] 消息自动删除计时器已变更";
        }
        if (message.getForumTopicCreated() != null || message.getForumTopicClosed() != null
                || message.getForumTopicReopened() != null || message.getForumTopicEdited() != null) {
            return "[event] 论坛主题事件";
        }
        if (message.getVideoChatStarted() != null || message.getVideoChatEnded() != null
                || message.getVideoChatScheduled() != null || message.getVideoChatParticipantsInvited() != null) {
            return "[event] 语音聊天事件";
        }
        return null;
    }

    /**
     * 提取普通消息内容：文本原文或媒体占位符（带 caption 时拼接）；未知类型返回 null
     */
    private static String extractContent(Message message) {
        if (message.hasText()) {
            return message.getText();
        }
        if (message.hasPhoto()) {
            return withCaption("[image]", message.getCaption());
        }
        if (message.hasAnimation()) {
            return withCaption("[animation]", message.getCaption());
        }
        if (message.hasVideo()) {
            return withCaption("[video]", message.getCaption());
        }
        if (message.hasVoice()) {
            return "[voice]";
        }
        if (message.hasAudio()) {
            return withCaption("[audio]", message.getCaption());
        }
        if (message.hasVideoNote()) {
            return "[video_note]";
        }
        if (message.hasDocument()) {
            return withCaption("[document]", message.getCaption());
        }
        if (message.hasSticker()) {
            return "[sticker]";
        }
        if (message.hasPoll()) {
            return message.getPoll() != null && message.getPoll().getQuestion() != null
                    ? "[poll] " + message.getPoll().getQuestion()
                    : "[poll]";
        }
        if (message.hasDice()) {
            return "[dice]";
        }
        if (message.hasLocation()) {
            return "[location]";
        }
        if (message.hasContact()) {
            return "[contact]";
        }
        if (message.hasInvoice()) {
            return "[invoice]";
        }
        return null;
    }

    private static String withCaption(String placeholder, String caption) {
        return caption != null && !caption.isBlank() ? placeholder + " " + caption : placeholder;
    }

    private static String userDisplayName(User user) {
        String name = user.getFirstName() != null ? user.getFirstName() : "";
        if (user.getLastName() != null && !user.getLastName().isBlank()) {
            name = name.isBlank() ? user.getLastName() : name + " " + user.getLastName();
        }
        if (name.isBlank() && user.getUserName() != null) {
            name = "@" + user.getUserName();
        }
        return name.isBlank() ? null : name;
    }

    private static LocalDateTime toLocalTime(Integer epochSecond) {
        return epochSecond == null
                ? LocalDateTime.now(ZONE)
                : Instant.ofEpochSecond(epochSecond).atZone(ZONE).toLocalDateTime();
    }
}
