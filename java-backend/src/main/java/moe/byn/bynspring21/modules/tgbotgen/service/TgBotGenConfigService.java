package moe.byn.bynspring21.modules.tgbotgen.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.exception.BynBaseException;
import moe.byn.bynspring21.service.SysConfigService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Telegram Bot 的运行时配置，存 sys_config（key 命名沿用项目 camelCase 风格）。
 * 机器人启动/热重载时通过 loadSettings() 读取一份快照，运行期间不再逐条消息查库。
 * 功能开关分两层：全局功能开关 + ChatID 特定配置（按聊天覆盖全局，未设置项跟随全局）
 */
@Slf4j
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
    public static final String KEY_CHAT_FEATURE_OVERRIDES = "tgBotChatFeatureOverrides";

    private static final int REMARK_MAX_LENGTH = 64;

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
            String botUpdateDate,
            Map<Long, ChatFeatureOverride> chatFeatureOverrides
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

        /**
         * 全局功能开关（命令菜单按此生成）
         */
        public Features globalFeatures() {
            return new Features(chatLogEnabled, webSearchEnabled, chatEnabled, rankEnabled && chatLogEnabled);
        }

        /**
         * 指定聊天最终生效的功能：ChatID 特定配置中显式开启/关闭的项覆盖全局开关，未设置（null）的项跟随全局；
         * 活跃排行依赖该聊天最终生效的消息记录
         */
        public Features featuresFor(Long chatId) {
            ChatFeatureOverride override = chatId != null ? chatFeatureOverrides.get(chatId) : null;
            if (override == null) {
                return globalFeatures();
            }
            boolean chatLog = pick(override.chatLogEnabled(), chatLogEnabled);
            return new Features(
                    chatLog,
                    pick(override.webSearchEnabled(), webSearchEnabled),
                    pick(override.chatEnabled(), chatEnabled),
                    chatLog && pick(override.rankEnabled(), rankEnabled));
        }

        private static boolean pick(Boolean override, boolean global) {
            return override != null ? override : global;
        }
    }

    /**
     * 某个聊天最终生效的功能
     */
    public record Features(boolean chatLogEnabled, boolean webSearchEnabled, boolean chatEnabled, boolean rankEnabled) {
    }

    /**
     * ChatID 特定功能配置：各功能为 null 表示跟随全局，true/false 表示对该聊天强制开启/关闭
     */
    public record ChatFeatureOverride(
            Long chatId,
            String remark,
            Boolean chatLogEnabled,
            Boolean webSearchEnabled,
            Boolean chatEnabled,
            Boolean rankEnabled
    ) {
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
            String botUpdateDate,
            List<ChatFeatureOverride> chatFeatureOverrides
    ) {
    }

    public Settings loadSettings() {
        Map<Long, ChatFeatureOverride> overrides = new LinkedHashMap<>();
        for (ChatFeatureOverride override : loadChatFeatureOverrides()) {
            overrides.put(override.chatId(), override);
        }
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
                getOrDefault(KEY_BOT_UPDATE_DATE, DEFAULT_BOT_UPDATE_DATE),
                Collections.unmodifiableMap(overrides));
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
                getOrDefault(KEY_BOT_UPDATE_DATE, DEFAULT_BOT_UPDATE_DATE),
                loadChatFeatureOverrides());
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
        sysConfigService.setConfigValue(KEY_CHAT_FEATURE_OVERRIDES, serializeChatFeatureOverrides(config.chatFeatureOverrides()));
    }

    /**
     * 校验并序列化 ChatID 特定配置：ChatID 必填且不可重复，备注截断至 REMARK_MAX_LENGTH
     */
    private static String serializeChatFeatureOverrides(List<ChatFeatureOverride> overrides) {
        if (overrides == null || overrides.isEmpty()) {
            return "[]";
        }
        Set<Long> seen = new HashSet<>();
        JSONArray array = new JSONArray();
        for (ChatFeatureOverride override : overrides) {
            if (override == null || override.chatId() == null) {
                throw new BynBaseException("ChatID 特定配置中存在未填写的 ChatID");
            }
            if (!seen.add(override.chatId())) {
                throw new BynBaseException("ChatID 特定配置中存在重复的 ChatID：" + override.chatId());
            }
            String remark = nullToEmpty(override.remark()).trim();
            if (remark.length() > REMARK_MAX_LENGTH) {
                remark = remark.substring(0, REMARK_MAX_LENGTH);
            }
            JSONObject item = new JSONObject();
            // ChatID 以字符串存储，避免前端 JSON 数值精度问题
            item.put("chatId", String.valueOf(override.chatId()));
            item.put("remark", remark);
            item.put("chatLogEnabled", override.chatLogEnabled());
            item.put("webSearchEnabled", override.webSearchEnabled());
            item.put("chatEnabled", override.chatEnabled());
            item.put("rankEnabled", override.rankEnabled());
            array.add(item);
        }
        return JSON.toJSONString(array, JSONWriter.Feature.WriteNulls);
    }

    /**
     * 读取 ChatID 特定配置，解析失败或非法条目忽略
     */
    private List<ChatFeatureOverride> loadChatFeatureOverrides() {
        String raw = getRaw(KEY_CHAT_FEATURE_OVERRIDES);
        if (raw.isEmpty()) {
            return List.of();
        }
        List<ChatFeatureOverride> result = new ArrayList<>();
        try {
            JSONArray array = JSON.parseArray(raw);
            for (int i = 0; i < array.size(); i++) {
                JSONObject item = array.getJSONObject(i);
                Long chatId;
                try {
                    chatId = Long.parseLong(item.getString("chatId").trim());
                } catch (Exception e) {
                    continue;
                }
                result.add(new ChatFeatureOverride(
                        chatId,
                        item.getString("remark"),
                        item.getBoolean("chatLogEnabled"),
                        item.getBoolean("webSearchEnabled"),
                        item.getBoolean("chatEnabled"),
                        item.getBoolean("rankEnabled")));
            }
        } catch (Exception e) {
            log.warn("解析 {} 失败，ChatID 特定配置不生效", KEY_CHAT_FEATURE_OVERRIDES, e);
            return List.of();
        }
        return result;
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
