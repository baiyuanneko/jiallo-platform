package moe.byn.bynspring21.entity.tool;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import moe.byn.bynspring21.entity.base.AgenticToolEnum;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ObservableToolCallingManager implements ToolCallingManager {

    private final ToolCallingManager delegate;
    private final SseEmitter emitter;
    private final ToolCallCollector collector;

    public ObservableToolCallingManager(ToolCallingManager delegate,
                                        SseEmitter emitter,
                                        ToolCallCollector collector) {
        this.delegate = delegate;
        this.emitter = emitter;
        this.collector = collector;
    }

    @Override
    public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions) {
        return delegate.resolveToolDefinitions(chatOptions);
    }

    @Override
    public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
        AssistantMessage output = chatResponse.getResult().getOutput();
        List<AssistantMessage.ToolCall> toolCalls = output.getToolCalls();

        for (AssistantMessage.ToolCall tc : toolCalls) {
            sendSseEvent("tool_call", JSON.toJSONString(new ToolCallEvent(AgenticToolEnum.toToolCode(tc.name()), tc.id(), tc.arguments())));
        }

        ToolExecutionResult result = delegate.executeToolCalls(prompt, chatResponse);

        int existingCount = prompt.getInstructions().size();
        List<Message> newMessages = result.conversationHistory().subList(
                Math.min(existingCount, result.conversationHistory().size()),
                result.conversationHistory().size());

        List<ToolResponseMessage.ToolResponse> allResponses = new ArrayList<>();
        List<Map<String, Object>> structuredResponses = new ArrayList<>();
        for (Message msg : newMessages) {
            if (msg instanceof ToolResponseMessage trm) {
                for (ToolResponseMessage.ToolResponse tr : trm.getResponses()) {
                    Object structuredData = resolveToolResultData(tr.name(), tr.responseData());
                    sendSseEvent("tool_result", JSON.toJSONString(new ToolResultEvent(AgenticToolEnum.toToolCode(tr.name()), tr.id(), structuredData)));
                    allResponses.add(tr);
                    Map<String, Object> sr = new LinkedHashMap<>();
                    sr.put("name", tr.name());
                    sr.put("id", tr.id());
                    sr.put("response", structuredData);
                    structuredResponses.add(sr);
                }
            }
        }

        String structuredToolContent = structuredResponses.isEmpty() ? null : JSON.toJSONString(structuredResponses);
        collector.addRound(toolCalls, allResponses, structuredToolContent);
        return result;
    }

    private void sendSseEvent(String eventName, String data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException e) {
            log.error("SSE 发送 {} 事件失败", eventName, e);
        }
    }

    public record ToolCallEvent(String name, String id, String arguments) {}
    public record ToolResultEvent(String name, String id, Object result) {}

    private static final String STRUCTURED_TOOL_BOCHA = "bochaWebSearch";
    private static final String STRUCTURED_TOOL_PYODIDE = "pyodideCodeRunner";
    private static final String STRUCTURED_TOOL_RAG = "searchRagKnowledgeBase";

    private Object resolveToolResultData(String toolName, String rawData) {
        if (STRUCTURED_TOOL_BOCHA.equals(toolName)) {
            List<BochaWebSearch.SearchResultItem> items = BochaWebSearch.consumeLastStructuredResults();
            if (!items.isEmpty()) {
                return items;
            }
        }
        if (STRUCTURED_TOOL_PYODIDE.equals(toolName)) {
            PyodideResultPendingManager.PyodideResult pyodideResult = PyodideCodeRunner.consumeLastStructuredResult();
            if (pyodideResult != null) {
                return pyodideResult;
            }
        }
        if (STRUCTURED_TOOL_RAG.equals(toolName)) {
            List<RagKnowledgeBaseTool.RagSearchResultItem> items = RagKnowledgeBaseTool.consumeLastStructuredResults();
            if (!items.isEmpty()) {
                return items;
            }
        }
        return rawData;
    }
}
