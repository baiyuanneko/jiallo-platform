package moe.byn.bynspring21.modules.tgbotgen.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.entity.tool.BochaWebSearch;
import moe.byn.bynspring21.entity.tool.DeepseekWebSearch;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Telegram /websearch：复用 DeepSeek / 博查搜索工具，按结构化结果格式化为纯文本回复（不含网页抓取）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TgBotWebSearchService {

    public enum Provider {
        DEEPSEEK("DeepSeek"),
        BOCHA("博查");

        private final String label;

        Provider(String label) {
            this.label = label;
        }
    }

    private static final int RESULT_COUNT = 8;

    private static final int SNIPPET_MAX_LENGTH = 150;

    private final DeepseekWebSearch deepseekWebSearch;

    private final BochaWebSearch bochaWebSearch;

    public String search(Provider provider, String query) {
        String rawText;
        List<BochaWebSearch.SearchResultItem> items;
        try {
            if (provider == Provider.BOCHA) {
                rawText = bochaWebSearch.bochaWebSearch(query);
                items = BochaWebSearch.consumeLastStructuredResults();
            } else {
                rawText = deepseekWebSearch.deepseekWebSearch(query, RESULT_COUNT, null, false);
                items = DeepseekWebSearch.consumeLastStructuredResults();
            }
        } catch (Exception e) {
            log.error("Telegram /websearch 搜索失败, provider={}", provider, e);
            return "搜索失败，请稍后重试";
        } finally {
            // 搜索工具会把结构化结果压入 ThreadLocal 队列（供网页聊天推送卡片），机器人线程池中必须清理
            BochaWebSearch.clearContext();
            DeepseekWebSearch.clearContext();
        }

        if (items.isEmpty()) {
            // 无结构化结果时工具返回的是错误或"未找到"提示文本
            return provider.label + " 搜索：" + query + "\n\n" + rawText;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("🔍 ").append(provider.label).append(" 搜索：").append(query).append("\n");
        int count = Math.min(items.size(), RESULT_COUNT);
        for (int i = 0; i < count; i++) {
            BochaWebSearch.SearchResultItem item = items.get(i);
            sb.append("\n").append(i + 1).append(". ").append(nullToEmpty(item.title())).append("\n");
            sb.append(nullToEmpty(item.url())).append("\n");
            String snippet = item.snippet() != null && !item.snippet().isBlank() ? item.snippet() : item.summary();
            if (snippet != null && !snippet.isBlank()) {
                String trimmed = snippet.strip();
                if (trimmed.length() > SNIPPET_MAX_LENGTH) {
                    trimmed = trimmed.substring(0, SNIPPET_MAX_LENGTH) + "…";
                }
                sb.append(trimmed).append("\n");
            }
        }
        return sb.toString();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
