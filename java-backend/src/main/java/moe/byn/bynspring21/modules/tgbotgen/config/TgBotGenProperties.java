package moe.byn.bynspring21.modules.tgbotgen.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Telegram Bot 的部署级配置（application.yml: myapp.tgbot.*）。
 * 代理属于部署环境配置，不走 sys_config。
 */
@Slf4j
@Component
public class TgBotGenProperties {

    @Value("${myapp.tgbot.proxyEnabled:false}")
    private boolean proxyEnabled;

    @Value("${myapp.tgbot.proxyUrl:}")
    private String proxyUrl;

    /**
     * 根据 myapp.tgbot.proxy* 构造 DefaultBotOptions；代理未启用或无法解析时返回无代理的默认实例。
     */
    public DefaultBotOptions buildBotOptions() {
        DefaultBotOptions options = new DefaultBotOptions();
        if (!proxyEnabled) {
            return options;
        }
        if (proxyUrl == null || proxyUrl.isBlank()) {
            log.warn("myapp.tgbot.proxyEnabled=true 但 myapp.tgbot.proxyUrl 为空，按无代理启动");
            return options;
        }
        try {
            URI uri = new URI(proxyUrl.trim());
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
            DefaultBotOptions.ProxyType proxyType = switch (scheme) {
                case "http", "https" -> DefaultBotOptions.ProxyType.HTTP;
                case "socks4" -> DefaultBotOptions.ProxyType.SOCKS4;
                case "socks5" -> DefaultBotOptions.ProxyType.SOCKS5;
                default -> null;
            };
            if (proxyType == null || uri.getHost() == null || uri.getPort() < 0) {
                log.warn("无法识别的代理地址 [{}]，支持 http(s)://host:port 或 socks4/socks5://host:port，按无代理启动", proxyUrl);
                return options;
            }
            options.setProxyType(proxyType);
            options.setProxyHost(uri.getHost());
            options.setProxyPort(uri.getPort());
            log.info("Telegram Bot 将使用代理 {}://{}:{}", scheme, uri.getHost(), uri.getPort());
        } catch (URISyntaxException e) {
            log.warn("代理地址 [{}] 解析失败，按无代理启动", proxyUrl);
        }
        return options;
    }
}
