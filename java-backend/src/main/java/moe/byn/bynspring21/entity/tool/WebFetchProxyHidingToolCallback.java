package moe.byn.bynspring21.entity.tool;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/**
 * web_fetch 工具装饰器：当前用户未获「网页抓取代理」授权时，从工具 schema 中移除 withProxy 参数，
 * 模型看不到该参数；调用时同样剔除入参中的 withProxy（兜底，WebFetch 内部仍会校验授权）
 */
public class WebFetchProxyHidingToolCallback implements ToolCallback {

    private final ToolCallback delegate;

    private final ToolDefinition toolDefinition;

    public WebFetchProxyHidingToolCallback(ToolCallback delegate) {
        this.delegate = delegate;
        ToolDefinition original = delegate.getToolDefinition();
        this.toolDefinition = ToolDefinition.builder()
                .name(original.name())
                .description(original.description())
                .inputSchema(stripWithProxy(original.inputSchema()))
                .build();
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return toolDefinition;
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    @Override
    public String call(String toolInput) {
        return delegate.call(stripWithProxyArgument(toolInput));
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        return delegate.call(stripWithProxyArgument(toolInput), toolContext);
    }

    private static String stripWithProxy(String inputSchema) {
        JSONObject schema = JSON.parseObject(inputSchema);
        JSONObject properties = schema.getJSONObject("properties");
        if (properties != null) {
            properties.remove(WebFetch.WITH_PROXY_PARAM);
        }
        JSONArray required = schema.getJSONArray("required");
        if (required != null) {
            required.remove(WebFetch.WITH_PROXY_PARAM);
        }
        return schema.toJSONString();
    }

    private static String stripWithProxyArgument(String toolInput) {
        try {
            JSONObject input = JSON.parseObject(toolInput);
            if (input == null || !input.containsKey(WebFetch.WITH_PROXY_PARAM)) {
                return toolInput;
            }
            input.remove(WebFetch.WITH_PROXY_PARAM);
            return input.toJSONString();
        } catch (Exception e) {
            // 非法 JSON 原样交给 delegate，由其返回参数错误
            return toolInput;
        }
    }
}
