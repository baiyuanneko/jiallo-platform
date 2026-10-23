package moe.byn.bynspring21.entity.tool;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * DeepSeek Search / Fetch API 公共 HTTP 层：鉴权、超时、错误码映射与重试。
 * 408、429、5xx 按指数退避 + 抖动重试，其余 4xx 直接返回错误提示
 */
@Slf4j
@Component
public class DeepseekToolsClient {

    private static final String BASE_URL = "https://api.deepseek.com/v1/tools/";

    private static final int MAX_RETRIES = 2;

    private static final long BASE_BACKOFF_MS = 500;

    @Value("${deepseek-tools.api-key:}")
    private String apiKey;

    /**
     * 调用结果：成功时 body 为响应 JSON，失败时 errorMessage 为可直接交给 LLM 的中文提示
     */
    public record Result(JSONObject body, String errorMessage) {
        public boolean isSuccess() {
            return body != null;
        }
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public Result post(String endpoint, JSONObject requestBody, int timeoutMs) {
        if (!isConfigured()) {
            return new Result(null, "DeepSeek 搜索/抓取服务未配置（缺少 DEEPSEEK_API_KEY）");
        }
        String url = BASE_URL + endpoint;
        for (int attempt = 0; ; attempt++) {
            int status;
            String responseBody;
            try (HttpResponse response = HttpRequest.post(url)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .body(requestBody.toJSONString())
                    .timeout(timeoutMs)
                    .execute()) {
                status = response.getStatus();
                responseBody = response.body();
            } catch (Exception e) {
                // 网络异常/超时按可重试处理
                log.warn("DeepSeek {} 请求异常, attempt={}, error={}", endpoint, attempt, e.getMessage());
                if (attempt < MAX_RETRIES && backoff(attempt)) {
                    continue;
                }
                return new Result(null, "DeepSeek 服务请求失败或超时，请稍后重试");
            }

            if (status == 200) {
                try {
                    return new Result(JSONObject.parseObject(responseBody), null);
                } catch (Exception e) {
                    log.warn("DeepSeek {} 响应解析失败: {}", endpoint, responseBody);
                    return new Result(null, "DeepSeek 服务返回了无法解析的响应");
                }
            }

            boolean retryable = status == 408 || status == 429 || status >= 500;
            if (retryable && attempt < MAX_RETRIES && backoff(attempt)) {
                log.warn("DeepSeek {} 返回 {}, 准备重试, attempt={}", endpoint, status, attempt);
                continue;
            }
            log.warn("DeepSeek {} 返回异常 status={}, body={}", endpoint, status, responseBody);
            return new Result(null, describeError(status));
        }
    }

    /**
     * 指数退避 + 抖动；被中断时返回 false（不再重试）
     */
    private boolean backoff(int attempt) {
        long delay = BASE_BACKOFF_MS * (1L << attempt);
        delay += ThreadLocalRandom.current().nextLong(delay / 2 + 1);
        try {
            Thread.sleep(delay);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static String describeError(int status) {
        return switch (status) {
            case 400 -> "请求参数不合法，请调整参数后重试";
            case 401 -> "DeepSeek API Key 缺失或无效";
            case 402 -> "DeepSeek 账户余额不足";
            case 403 -> "该网页被 DeepSeek 安全策略拒绝抓取";
            case 404 -> "无法获取该网页的可用正文（页面不可访问、被拦截或内容类型不支持）";
            case 408 -> "DeepSeek 服务读取请求超时，请稍后重试";
            case 413 -> "请求体过大";
            case 429 -> "请求过于频繁（账号或目标域名限速），请稍后重试";
            case 504 -> "DeepSeek 下游服务超时，请稍后重试";
            default -> status >= 500 ? "DeepSeek 服务内部错误，请稍后重试" : "DeepSeek 服务返回错误（HTTP " + status + "）";
        };
    }
}
