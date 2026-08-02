package moe.byn.bynspring21.modules.tgbotgen.service;

import lombok.RequiredArgsConstructor;
import moe.byn.bynspring21.service.SysConfigService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Telegram Bot 的运行时配置，存 sys_config（key 命名沿用项目 camelCase 风格）。
 */
@Service
@RequiredArgsConstructor
public class TgBotGenConfigService {

    public static final String KEY_ENABLED = "tgBotEnabled";
    public static final String KEY_TOKEN = "tgBotToken";
    public static final String KEY_CHAT_ID_MODE = "tgBotChatIdMode";
    public static final String KEY_WHITELIST_CHAT_IDS = "tgBotWhitelistChatIds";
    public static final String KEY_BLACKLIST_CHAT_IDS = "tgBotBlacklistChatIds";

    public static final String MODE_WHITELIST = "whitelist";
    public static final String MODE_BLACKLIST = "blacklist";

    private final SysConfigService sysConfigService;

    public boolean isEnabled() {
        return "true".equalsIgnoreCase(getRaw(KEY_ENABLED));
    }

    public String getToken() {
        return getRaw(KEY_TOKEN);
    }

    public String getChatIdMode() {
        return MODE_BLACKLIST.equalsIgnoreCase(getRaw(KEY_CHAT_ID_MODE)) ? MODE_BLACKLIST : MODE_WHITELIST;
    }

    public String getWhitelistChatIdsRaw() {
        return getRaw(KEY_WHITELIST_CHAT_IDS);
    }

    public String getBlacklistChatIdsRaw() {
        return getRaw(KEY_BLACKLIST_CHAT_IDS);
    }

    /**
     * ChatId 过滤：白名单模式仅名单内放行（空名单=全部拒绝），黑名单模式仅名单外放行。
     */
    public boolean isChatAllowed(Long chatId) {
        if (chatId == null) {
            return false;
        }
        if (MODE_WHITELIST.equals(getChatIdMode())) {
            return parseChatIds(getWhitelistChatIdsRaw()).contains(chatId);
        }
        return !parseChatIds(getBlacklistChatIdsRaw()).contains(chatId);
    }

    public void saveConfig(boolean enabled, String token, String chatIdMode,
                           String whitelistChatIds, String blacklistChatIds) {
        sysConfigService.setConfigValue(KEY_ENABLED, String.valueOf(enabled));
        sysConfigService.setConfigValue(KEY_TOKEN, nullToEmpty(token));
        sysConfigService.setConfigValue(KEY_CHAT_ID_MODE,
                MODE_BLACKLIST.equalsIgnoreCase(nullToEmpty(chatIdMode).trim()) ? MODE_BLACKLIST : MODE_WHITELIST);
        sysConfigService.setConfigValue(KEY_WHITELIST_CHAT_IDS, nullToEmpty(whitelistChatIds));
        sysConfigService.setConfigValue(KEY_BLACKLIST_CHAT_IDS, nullToEmpty(blacklistChatIds));
    }

    private String getRaw(String key) {
        String value = sysConfigService.getConfigValue(key);
        return value == null ? "" : value.trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private Set<Long> parseChatIds(String raw) {
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
        return ids;
    }
}
