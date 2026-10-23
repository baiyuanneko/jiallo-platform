package moe.byn.bynspring21.modules.tgbotgen.service;

import lombok.RequiredArgsConstructor;
import moe.byn.bynspring21.service.SysConfigService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Telegram Bot 的运行时配置，存 sys_config（key 命名沿用项目 camelCase 风格）。
 * 机器人启动/热重载时通过 loadSettings() 读取一份快照，运行期间不再逐条消息查库
 */
@Service
@RequiredArgsConstructor
public class TgBotGenConfigService {

    public static final String KEY_ENABLED = "tgBotEnabled";
    public static final String KEY_TOKEN = "tgBotToken";
    public static final String KEY_CHAT_ID_MODE = "tgBotChatIdMode";
    public static final String KEY_WHITELIST_CHAT_IDS = "tgBotWhitelistChatIds";
    public static final String KEY_BLACKLIST_CHAT_IDS = "tgBotBlacklistChatIds";

    public static final String KEY_FEATURE_CHAT_LOG = "tgBotFeatureChatLog";
    public static final String KEY_FEATURE_WEB_SEARCH = "tgBotFeatureWebSearch";
    public static final String KEY_FEATURE_CHAT = "tgBotFeatureChat";
    public static final String KEY_FEATURE_RANK = "tgBotFeatureRank";
    public static final String KEY_CHAT_MODEL_ID = "tgBotChatModelId";
    public static final String KEY_CHAT_SYSTEM_PROMPT = "tgBotChatSystemPrompt";
    public static final String KEY_BOT_NAME = "tgBotName";
    public static final String KEY_BOT_UPDATE_DATE = "tgBotUpdateDate";

    /** /help、/about 中展示的机器人名称与更新日期，未配置时使用默认值 */
    public static final String DEFAULT_BOT_NAME = "Telegram 智能机器人";
    public static final String DEFAULT_BOT_UPDATE_DATE = "2026-10-24";

    public static final String MODE_WHITELIST = "whitelist";
    public static final String MODE_BLACKLIST = "blacklist";

    /**
     * Token 打码中间段，回显给前端时使用；保存时若收到打码值则保留原 Token
     */
    private static final String TOKEN_MASK = "****";

    private final SysConfigService sysConfigService;

    /**
     * 机器人运行期配置快照
     */
    public record Settings(
            boolean enabled,
            String token,
            String chatIdMode,
            Set<Long> whitelistChatIds,
            Set<Long> blacklistChatIds,
            boolean chatLogEnabled,
            boolean webSearchEnabled,
            boolean chatEnabled,
            boolean rankEnabled,
            String chatModelId,
            String chatSystemPrompt,
            String botName,
            String botUpdateDate
    ) {
        /**
         * ChatId 过滤：白名单模式仅名单内放行（空名单=全部拒绝），黑名单模式仅名单外放行。
         */
        public boolean isChatAllowed(Long chatId) {
            if (chatId == null) {
                return false;
            }
            if (MODE_WHITELIST.equals(chatIdMode)) {
                return whitelistChatIds.contains(chatId);
            }
            return !blacklistChatIds.contains(chatId);
        }
    }

    /**
     * 管理后台读写用的原始配置（ChatId 列表保留原始文本）
     */
    public record RawConfig(
            boolean enabled,
            String token,
            String chatIdMode,
            String whitelistChatIds,
            String blacklistChatIds,
            boolean chatLogEnabled,
            boolean webSearchEnabled,
            boolean chatEnabled,
            boolean rankEnabled,
            String chatModelId,
            String chatSystemPrompt,
            String botName,
            String botUpdateDate
    ) {
    }

    public Settings loadSettings() {
        return new Settings(
                isTrue(KEY_ENABLED),
                getRaw(KEY_TOKEN),
                getChatIdMode(),
                parseChatIds(getRaw(KEY_WHITELIST_CHAT_IDS)),
                parseChatIds(getRaw(KEY_BLACKLIST_CHAT_IDS)),
                isTrue(KEY_FEATURE_CHAT_LOG),
                isTrue(KEY_FEATURE_WEB_SEARCH),
                isTrue(KEY_FEATURE_CHAT),
                isTrue(KEY_FEATURE_RANK),
                getRaw(KEY_CHAT_MODEL_ID),
                getRaw(KEY_CHAT_SYSTEM_PROMPT),
                getOrDefault(KEY_BOT_NAME, DEFAULT_BOT_NAME),
                getOrDefault(KEY_BOT_UPDATE_DATE, DEFAULT_BOT_UPDATE_DATE));
    }

    /**
     * 读取管理后台展示用配置，Token 打码返回；机器人名称/更新日期未配置时返回默认值
     */
    public RawConfig loadRawConfigMasked() {
        return new RawConfig(
                isTrue(KEY_ENABLED),
                maskToken(getRaw(KEY_TOKEN)),
                getChatIdMode(),
                getRaw(KEY_WHITELIST_CHAT_IDS),
                getRaw(KEY_BLACKLIST_CHAT_IDS),
                isTrue(KEY_FEATURE_CHAT_LOG),
                isTrue(KEY_FEATURE_WEB_SEARCH),
                isTrue(KEY_FEATURE_CHAT),
                isTrue(KEY_FEATURE_RANK),
                getRaw(KEY_CHAT_MODEL_ID),
                getRaw(KEY_CHAT_SYSTEM_PROMPT),
                getOrDefault(KEY_BOT_NAME, DEFAULT_BOT_NAME),
                getOrDefault(KEY_BOT_UPDATE_DATE, DEFAULT_BOT_UPDATE_DATE));
    }

    /**
     * 保存配置。token 为当前 Token 的打码值时保留原 Token；排行功能依赖消息记录，记录关闭时强制关闭排行
     */
    public void saveConfig(RawConfig config) {
        String currentToken = getRaw(KEY_TOKEN);
        String token = nullToEmpty(config.token()).trim();
        if (!currentToken.isEmpty() && token.equals(maskToken(currentToken))) {
            token = currentToken;
        }
        sysConfigService.setConfigValue(KEY_ENABLED, String.valueOf(config.enabled()));
        sysConfigService.setConfigValue(KEY_TOKEN, token);
        sysConfigService.setConfigValue(KEY_CHAT_ID_MODE,
                MODE_BLACKLIST.equalsIgnoreCase(nullToEmpty(config.chatIdMode()).trim()) ? MODE_BLACKLIST : MODE_WHITELIST);
        sysConfigService.setConfigValue(KEY_WHITELIST_CHAT_IDS, nullToEmpty(config.whitelistChatIds()));
        sysConfigService.setConfigValue(KEY_BLACKLIST_CHAT_IDS, nullToEmpty(config.blacklistChatIds()));
        sysConfigService.setConfigValue(KEY_FEATURE_CHAT_LOG, String.valueOf(config.chatLogEnabled()));
        sysConfigService.setConfigValue(KEY_FEATURE_WEB_SEARCH, String.valueOf(config.webSearchEnabled()));
        sysConfigService.setConfigValue(KEY_FEATURE_CHAT, String.valueOf(config.chatEnabled()));
        sysConfigService.setConfigValue(KEY_FEATURE_RANK, String.valueOf(config.chatLogEnabled() && config.rankEnabled()));
        sysConfigService.setConfigValue(KEY_CHAT_MODEL_ID, nullToEmpty(config.chatModelId()).trim());
        sysConfigService.setConfigValue(KEY_CHAT_SYSTEM_PROMPT, nullToEmpty(config.chatSystemPrompt()));
        // 留空保存则回落到默认值
        sysConfigService.setConfigValue(KEY_BOT_NAME, nullToEmpty(config.botName()).trim());
        sysConfigService.setConfigValue(KEY_BOT_UPDATE_DATE, nullToEmpty(config.botUpdateDate()).trim());
    }

    private String getOrDefault(String key, String defaultValue) {
        String value = getRaw(key);
        return value.isEmpty() ? defaultValue : value;
    }

    private String getChatIdMode() {
        return MODE_BLACKLIST.equalsIgnoreCase(getRaw(KEY_CHAT_ID_MODE)) ? MODE_BLACKLIST : MODE_WHITELIST;
    }

    /**
     * Token 打码：保留前 4 位与后 4 位，过短时整体打码
     */
    private static String maskToken(String token) {
        if (token == null || token.isEmpty()) {
            return "";
        }
        if (token.length() <= 12) {
            return TOKEN_MASK;
        }
        return token.substring(0, 4) + TOKEN_MASK + token.substring(token.length() - 4);
    }

    private boolean isTrue(String key) {
        return "true".equalsIgnoreCase(getRaw(key));
    }

    private String getRaw(String key) {
        String value = sysConfigService.getConfigValue(key);
        return value == null ? "" : value.trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static Set<Long> parseChatIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptySet();
        }
        Set<Long> ids = new HashSet<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                ids.add(Long.parseLong(trimmed));
            } catch (NumberFormatException ignored) {
                // 忽略非法 ChatId 项
            }
        }
        return Collections.unmodifiableSet(ids);
    }
}
