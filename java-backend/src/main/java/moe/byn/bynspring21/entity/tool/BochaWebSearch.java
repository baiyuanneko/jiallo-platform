package moe.byn.bynspring21.entity.tool;

import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

@Slf4j
@Component
public class BochaWebSearch {

    private static final String BOCHA_API_URL = "https://api.bocha.cn/v1/web-search";

    private static final ThreadLocal<Deque<List<SearchResultItem>>> structuredResultsQueue =
            ThreadLocal.withInitial(ArrayDeque::new);

    @Value("${bocha.api-key:}")
    private String apiKey;

    public record SearchResultItem(
            String title,
            String url,
            String siteName,
            String snippet,
            String summary,
            String siteIcon,
            String datePublished
    ) {}

    @Tool(description = "搜索全网网页信息，获取与搜索关键词相关的网页标题、链接和摘要。当需要查找最新新闻、事实信息、或任何需要互联网搜索的问题时使用此工具。")
    public String bochaWebSearch(
            @ToolParam(description = "搜索关键词或问题", required = true) String query) {

        JSONObject requestBody = new JSONObject();
        requestBody.put("query", query);
        requestBody.put("summary", true);
        requestBody.put("count", 10);

        String responseBody;
        try {
            responseBody = HttpRequest.post(BOCHA_API_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(requestBody.toJSONString())
                    .execute()
                    .body();
        } catch (Exception e) {
            log.error("Bocha搜索请求失败, query={}", query, e);
            return "搜索请求失败，请稍后重试";
        }

        List<SearchResultItem> structured = extractStructuredResults(responseBody);
        structuredResultsQueue.get().addLast(structured);

        return extractSearchResults(responseBody);
    }

    public static void clearContext() {
        structuredResultsQueue.remove();
    }

    public static List<SearchResultItem> consumeLastStructuredResults() {
        Deque<List<SearchResultItem>> queue = structuredResultsQueue.get();
        List<SearchResultItem> results = queue.pollFirst();
        return results != null ? results : Collections.emptyList();
    }

    private List<SearchResultItem> extractStructuredResults(String responseBody) {
        JSONObject response = JSON.parseObject(responseBody);

        if (response.getIntValue("code") != 200) {
            return Collections.emptyList();
        }

        JSONObject data = response.getJSONObject("data");
        if (data == null) {
            return Collections.emptyList();
        }

        JSONObject webPages = data.getJSONObject("webPages");
        if (webPages == null || webPages.getJSONArray("value") == null) {
            return Collections.emptyList();
        }

        List<SearchResultItem> results = new ArrayList<>();
        for (int i = 0; i < webPages.getJSONArray("value").size(); i++) {
            JSONObject page = webPages.getJSONArray("value").getJSONObject(i);
            results.add(new SearchResultItem(
                    page.getString("name"),
                    page.getString("url"),
                    page.getString("siteName"),
                    page.getString("snippet"),
                    page.getString("summary"),
                    page.getString("siteIcon"),
                    page.getString("datePublished")
            ));
        }
        return results;
    }

    private String extractSearchResults(String responseBody) {
        JSONObject response = JSON.parseObject(responseBody);

        if (response.getIntValue("code") != 200) {
            log.warn("Bocha搜索返回异常: {}", responseBody);
            return "搜索服务异常: " + response.getString("msg");
        }

        JSONObject data = response.getJSONObject("data");
        if (data == null) {
            return "未找到搜索结果";
        }

        JSONObject webPages = data.getJSONObject("webPages");
        if (webPages == null || webPages.getJSONArray("value") == null) {
            return "未找到搜索结果";
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < webPages.getJSONArray("value").size(); i++) {
            JSONObject page = webPages.getJSONArray("value").getJSONObject(i);
            result.append("【").append(i + 1).append("】");
            result.append(page.getString("name")).append("\n");
            result.append("链接: ").append(page.getString("url")).append("\n");
            result.append("来源: ").append(page.getString("siteName")).append("\n");
            if (page.getString("snippet") != null) {
                result.append("摘要: ").append(page.getString("snippet")).append("\n");
            }
            if (page.getString("summary") != null) {
                result.append("详细摘要: ").append(page.getString("summary")).append("\n");
            }
            result.append("\n");
        }

        return result.toString();
    }
}
