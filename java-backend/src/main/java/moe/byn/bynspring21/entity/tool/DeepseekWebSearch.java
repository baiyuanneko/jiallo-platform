package moe.byn.bynspring21.entity.tool;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * DeepSeek 网页搜索工具（POST /v1/tools/search），结构化结果复用博查的 SearchResultItem 供前端渲染搜索卡片
 */
@Slf4j
@Component
public class DeepseekWebSearch {

    private static final int MAX_QUERY_LENGTH = 128;

    private static final int DEFAULT_COUNT = 10;

    private static final int MAX_COUNT = 20;

    private static final ThreadLocal<Deque<List<BochaWebSearch.SearchResultItem>>> structuredResultsQueue =
            ThreadLocal.withInitial(ArrayDeque::new);

    @Autowired
    private DeepseekToolsClient client;

    @Value("${deepseek-tools.search-timeout:20000}")
    private int timeout;

    @Value("${deepseek-tools.max-content-length:10000}")
    private int maxContentLength;

    @Tool(description = "使用 DeepSeek 搜索全网网页信息，返回网页标题、链接、来源、发布时间、摘要及相关性/权威性评分。"
            + "当需要查找最新新闻、事实信息、或任何需要互联网搜索的问题时使用此工具。"
            + "query 支持搜索语法：site:github.com 限定站点，-site:example.com 排除站点，"
            + "site:a.com OR site:b.com 多站点，\"精确短语\" 精确匹配，-关键词 排除关键词，"
            + "lang:zh,en 限制语言，after:2026-01-01 / before:2026-12-31 限制发布日期，操作符可组合。"
            + "rerank_score（0～3）≥1.5 为优质结果，authority（0～4，-1 未知）≥3 为极权威来源。")
    public String deepseekWebSearch(
            @ToolParam(description = "搜索词，1～128 个字符，可包含搜索语法", required = true) String query,
            @ToolParam(description = "返回条数，1～20，默认 10", required = false) Integer count,
            @ToolParam(description = "用户的原始问题（1～128 个字符），有助于理解搜索意图", required = false) String oriQuestion,
            @ToolParam(description = "是否返回网页正文，默认 false；需要深入阅读原文时设为 true", required = false) Boolean needContent) {

        if (query == null || query.isBlank()) {
            structuredResultsQueue.get().addLast(Collections.emptyList());
            return "搜索词不能为空";
        }
        String normalizedQuery = truncate(query.trim(), MAX_QUERY_LENGTH);
        int normalizedCount = count == null ? DEFAULT_COUNT : Math.max(1, Math.min(count, MAX_COUNT));
        boolean withContent = Boolean.TRUE.equals(needContent);

        JSONObject requestBody = new JSONObject();
        requestBody.put("query", normalizedQuery);
        requestBody.put("count", normalizedCount);
        requestBody.put("need_content", withContent);
        if (oriQuestion != null && !oriQuestion.isBlank()) {
            requestBody.put("ori_question", truncate(oriQuestion.trim(), MAX_QUERY_LENGTH));
        }

        DeepseekToolsClient.Result result = client.post("search", requestBody, timeout);
        if (!result.isSuccess()) {
            structuredResultsQueue.get().addLast(Collections.emptyList());
            return "搜索失败：" + result.errorMessage();
        }

        JSONArray results = result.body().getJSONArray("results");
        if (results == null || results.isEmpty()) {
            structuredResultsQueue.get().addLast(Collections.emptyList());
            return "未找到搜索结果";
        }

        List<BochaWebSearch.SearchResultItem> structured = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            JSONObject page = results.getJSONObject(i);
            String title = page.getString("title");
            String url = page.getString("url");
            String siteName = page.getString("site_name");
            String snippet = page.getString("snippet");
            String summary = page.getString("summary");
            String date = page.getString("date");

            structured.add(new BochaWebSearch.SearchResultItem(
                    title, url, siteName, snippet, summary, page.getString("site_icon"), date));

            text.append("【").append(i + 1).append("】").append(title).append("\n");
            text.append("链接: ").append(url).append("\n");
            if (siteName != null) {
                text.append("来源: ").append(siteName).append("\n");
            }
            if (date != null) {
                text.append("发布时间: ").append(date).append("\n");
            }
            text.append("评分: rerank_score=").append(page.getString("rerank_score"))
                    .append(", authority=").append(page.getString("authority")).append("\n");
            if (snippet != null && !snippet.isBlank()) {
                text.append("摘要: ").append(snippet).append("\n");
            }
            if (summary != null && !summary.isBlank()) {
                text.append("详细摘要: ").append(summary).append("\n");
            }
            String content = page.getString("content");
            if (withContent && content != null && !content.isBlank()) {
                text.append("正文:\n").append(truncateContent(content)).append("\n");
            }
            text.append("\n");
        }
        structuredResultsQueue.get().addLast(structured);
        return text.toString();
    }

    public static void clearContext() {
        structuredResultsQueue.remove();
    }

    public static List<BochaWebSearch.SearchResultItem> consumeLastStructuredResults() {
        List<BochaWebSearch.SearchResultItem> results = structuredResultsQueue.get().pollFirst();
        return results != null ? results : Collections.emptyList();
    }

    private String truncateContent(String content) {
        if (content.length() <= maxContentLength) {
            return content;
        }
        return content.substring(0, maxContentLength) + "\n...（内容已截断，原文 " + content.length() + " 字符）";
    }

    private static String truncate(String value, int maxCodePoints) {
        if (value.codePointCount(0, value.length()) <= maxCodePoints) {
            return value;
        }
        return value.substring(0, value.offsetByCodePoints(0, maxCodePoints));
    }
}
