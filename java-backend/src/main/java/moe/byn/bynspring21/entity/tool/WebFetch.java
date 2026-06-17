package moe.byn.bynspring21.entity.tool;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Dns;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Component
public class WebFetch {

    private static final String DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    private static final String[] NOISE_SELECTORS = {
            "script", "style", "noscript", "iframe", "svg",
            "nav", "footer", "header", "aside",
            ".sidebar", ".side", ".ad", ".advertisement", ".ads", ".advert",
            ".menu", ".nav", ".navbar", ".header", ".footer",
            ".cookie", ".cookie-banner", ".popup", ".modal", ".overlay",
            ".comment", ".comments", ".comment-list",
            ".social-share", ".share", ".related-posts", ".recommend",
            "[role=navigation]", "[role=complementary]", "[role=banner]"
    };

    private static final int MAX_REDIRECTS = 5;

    private static final long PARSE_TIMEOUT_MS = 5000;

    private static final ExecutorService PARSER_POOL =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "jsoup-parser");
                t.setDaemon(true);
                return t;
            });

    @Value("${web-fetch.timeout:15000}")
    private int timeout;

    @Value("${web-fetch.max-content-length:10000}")
    private int maxContentLength;

    @Value("${web-fetch.max-response-body-size:524288}")
    private int maxResponseBodySize;

    @Value("${web-fetch.max-links:20}")
    private int maxLinks;

    @Value("${web-fetch.user-agent:}")
    private String customUserAgent;

    private OkHttpClient client;

    @PostConstruct
    public void init() {
        client = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofMillis(timeout))
                .readTimeout(Duration.ofMillis(timeout))
                .writeTimeout(Duration.ofMillis(timeout))
                .proxy(Proxy.NO_PROXY)
                .followRedirects(false)
                .followSslRedirects(false)
                .dns(hostname -> {
                    List<InetAddress> addresses = Dns.SYSTEM.lookup(hostname);
                    for (InetAddress addr : addresses) {
                        if (SsrfBlockingSocketFactory.isBlockedAddress(addr)) {
                            throw new UnknownHostException(
                                    "SSRF blocked: " + hostname + " resolves to " + addr.getHostAddress());
                        }
                    }
                    return addresses;
                })
                .build();
    }

    @Tool(description = "抓取指定URL的网页正文内容。" +
            "返回页面标题、正文纯文本、以及页面上所有可点击的链接列表。" +
            "当你需要阅读某篇网页文章、技术文档或特定页面的详细内容时使用此工具。" +
            "如果页面上有感兴趣的文章链接，可以继续调用本工具打开该链接深入阅读。")
    public String webFetch(
            @ToolParam(description = "要抓取的网页完整URL，必须以 http:// 或 https:// 开头", required = true) String url) {

        if (url == null || url.isBlank()) {
            return "URL 不能为空";
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "无效的 URL，必须以 http:// 或 https:// 开头";
        }

        String ua = (customUserAgent != null && !customUserAgent.isBlank())
                ? customUserAgent : DEFAULT_USER_AGENT;

        try {
            return fetchWithRedirects(url, ua, 0);
        } catch (java.net.SocketTimeoutException e) {
            return "抓取超时，页面 " + (timeout / 1000) + " 秒内未响应";
        } catch (javax.net.ssl.SSLException e) {
            log.warn("SSL error fetching: {}", url, e);
            return "SSL 连接失败，请检查目标地址";
        } catch (java.net.UnknownHostException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("SSRF blocked")) {
                log.warn("SSRF blocked: {}", url);
                return "目标地址不可访问（内网地址已拦截）";
            }
            return "无法解析域名，请检查 URL 是否正确";
        } catch (java.io.InterruptedIOException e) {
            return "页面处理超时";
        } catch (Exception e) {
            log.error("WebFetch 抓取失败: {}", url, e);
            return "抓取页面失败，请稍后重试";
        }
    }

    private String fetchWithRedirects(String url, String userAgent, int depth) throws Exception {
        if (depth > MAX_REDIRECTS) {
            return "重定向次数过多";
        }

        /* Validate host IP before any connection (OkHttp skips Dns for IP literals) */
        validateHost(url);

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", sanitizeUserAgent(userAgent))
                .build();

        try (Response response = client.newCall(request).execute()) {
            int statusCode = response.code();
            if (isRedirect(statusCode)) {
                String location = response.header("Location");
                if (location == null) {
                    return "重定向响应缺少 Location 头";
                }

                URI redirectUri = new URI(url).resolve(location);
                String scheme = redirectUri.getScheme();
                if (!"http".equals(scheme) && !"https".equals(scheme)) {
                    return "不允许的重定向协议: " + scheme;
                }

                return fetchWithRedirects(redirectUri.toString(), userAgent, depth + 1);
            }

            ResponseBody body = response.body();
            if (body == null) {
                return "页面内容为空";
            }

            long contentLength = body.contentLength();
            int readLimit = (contentLength > 0)
                    ? (int) Math.min(contentLength, maxResponseBodySize)
                    : maxResponseBodySize;

            ByteArrayOutputStream baos = new ByteArrayOutputStream(Math.min(readLimit, 8192));
            byte[] buf = new byte[8192];
            int totalRead = 0;

            try (InputStream in = body.byteStream()) {
                while (totalRead < readLimit) {
                    int remaining = readLimit - totalRead;
                    int n = in.read(buf, 0, Math.min(buf.length, remaining));
                    if (n < 0) break;
                    baos.write(buf, 0, n);
                    totalRead += n;
                }
            }

            if (totalRead >= maxResponseBodySize && contentLength != 0) {
                log.warn("Response body truncated at {} bytes for URL: {}", maxResponseBodySize, url);
            }

            Document doc = parseHtml(baos.toString("UTF-8"), url);

            String title = doc.title();
            removeNoise(doc);
            String bodyText = doc.body().text();

            if (bodyText.length() > maxContentLength) {
                bodyText = bodyText.substring(0, maxContentLength)
                        + "\n\n...（内容已截断，原文 " + bodyText.length() + " 字符）";
            }

            String linksSection = buildLinksSection(doc);

            StringBuilder sb = new StringBuilder();
            sb.append("标题: ").append(title.isBlank() ? "(无标题)" : title).append("\n\n");
            sb.append("正文:\n").append(bodyText).append("\n");
            if (!linksSection.isEmpty()) {
                sb.append("\n").append(linksSection);
            }
            return sb.toString();
        }
    }

    private Document parseHtml(String html, String url) throws Exception {
        Future<Document> future = PARSER_POOL.submit(() -> Jsoup.parse(html, url));
        try {
            return future.get(PARSE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.warn("HTML parse timeout for URL: {}", url);
            throw new java.io.InterruptedIOException("HTML 解析超时");
        }
    }

    /**
     * Resolve the host from URL and validate its IP addresses.
     * This catches IP literals that OkHttp's Dns interface bypasses,
     * and provides defense-in-depth for hostnames that the custom Dns also validates.
     */
    private static void validateHost(String url) throws UnknownHostException {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null || host.isEmpty()) return;

            List<InetAddress> addresses = Dns.SYSTEM.lookup(host);
            for (InetAddress addr : addresses) {
                if (SsrfBlockingSocketFactory.isBlockedAddress(addr)) {
                    throw new UnknownHostException(
                            "SSRF blocked: " + host + " resolves to " + addr.getHostAddress());
                }
            }
        } catch (URISyntaxException e) {
            // Let downstream code handle invalid URLs
        }
    }

    private static boolean isRedirect(int statusCode) {
        return statusCode == 301 || statusCode == 302 || statusCode == 303
                || statusCode == 307 || statusCode == 308;
    }

    private static String sanitizeUserAgent(String ua) {
        return ua.replaceAll("[\\r\\n]", "");
    }

    private void removeNoise(Document doc) {
        for (String selector : NOISE_SELECTORS) {
            doc.select(selector).remove();
        }
    }

    private String buildLinksSection(Document doc) {
        Elements anchors = doc.select("a[href]");
        Set<String> seen = new LinkedHashSet<>();
        List<String> lines = new ArrayList<>();

        for (Element a : anchors) {
            String href = a.absUrl("href");
            if (href.isEmpty() || (!href.startsWith("http://") && !href.startsWith("https://"))) {
                continue;
            }
            if (seen.contains(href)) continue;
            seen.add(href);

            String text = a.text().trim();
            if (text.isEmpty()) text = href;
            else if (text.length() > 60) text = text.substring(0, 60) + "...";

            lines.add(String.format("%d. %s \u2192 %s", lines.size() + 1, text, href));
            if (lines.size() >= maxLinks) break;
        }

        if (lines.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("\u2500\u2500 \u9875\u9762\u94FE\u63A5 (").append(lines.size()).append(" \u4E2A) \u2500\u2500\n");
        for (String line : lines) {
            sb.append(line).append("\n");
        }
        return sb.toString();
    }
}
