package moe.byn.bynspring21.utils;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ThreadLocal helper for passing {@code reasoningContent} from
 * {@link AssistantMessage} (Spring AI Prompt layer) to
 * {@link org.springframework.ai.openai.api.OpenAiApi.ChatCompletionMessage}
 * (API request layer).
 * <p>
 * DeepSeek's thinking mode requires that assistant messages in multi-turn
 * conversations include the {@code reasoning_content} field. Spring AI 1.1.2's
 * {@code createRequest} does not populate this field from
 * {@code AssistantMessage.getProperties()}. This utility fills that gap by
 * transferring the value via a position-indexed {@link ThreadLocal} map.
 */
public final class ReasoningContentHelper {

    private static final ThreadLocal<Map<Integer, String>> REASONING_MAP = ThreadLocal.withInitial(HashMap::new);

    private ReasoningContentHelper() {
    }

    /**
     * Reads {@code reasoningContent} from every {@link AssistantMessage} in
     * the list and stores it keyed by the <em>API message position</em> (accounting
     * for tool-message expansion). Does <b>not</b> remove the entry from the
     * message's properties so it survives across tool-call loop iterations.
     * <p>
     * Position rules (matching {@code OpenAiChatModel.createRequest()}):
     * <ul>
     *   <li>{@code SystemMessage} / {@code UserMessage} / {@code AssistantMessage}
     *       each produce exactly <b>1</b> {@code ChatCompletionMessage}.</li>
     *   <li>{@code ToolResponseMessage} produces <b>N</b> entries – one per
     *       {@code ToolResponse}.</li>
     * </ul>
     *
     * @param messages the Prompt instruction list
     */
    public static void prepareMessages(List<Message> messages) {
        Map<Integer, String> reasoningMap = new HashMap<>();
        int apiPos = 0;
        for (Message msg : messages) {
            if (msg instanceof AssistantMessage assistant) {
                Map<String, Object> props = assistant.getMetadata();
                if (props != null) {
                    Object val = props.get("reasoningContent");
                    if (val instanceof String s && !s.isEmpty()) {
                        reasoningMap.put(apiPos, s);
                    }
                }
                apiPos++;
            } else if (msg instanceof ToolResponseMessage toolMsg) {
                if (toolMsg.getResponses() != null) {
                    apiPos += toolMsg.getResponses().size();
                }
            } else {
                apiPos++;
            }
        }
        REASONING_MAP.get().clear();
        if (!reasoningMap.isEmpty()) {
            REASONING_MAP.get().putAll(reasoningMap);
        }
    }

    /**
     * Returns the reasoning content for the given API message position, or
     * {@code null} if none is stored.
     */
    public static String getReasoningContent(int apiPosition) {
        return REASONING_MAP.get().get(apiPosition);
    }

    /**
     * Clears the ThreadLocal for the current thread. Must be called after the
     * model invocation completes (e.g. in a {@code finally} block).
     */
    public static void clear() {
        REASONING_MAP.remove();
    }

    /**
     * Returns {@code true} when the current thread has any reasoning-content
     * entries to inject.
     */
    public static boolean hasEntries() {
        return !REASONING_MAP.get().isEmpty();
    }
}
