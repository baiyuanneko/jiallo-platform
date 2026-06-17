package moe.byn.bynspring21.entity.tool;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ToolCallCollector {

public record ToolCallRound(
List<AssistantMessage.ToolCall> toolCalls,
List<ToolResponseMessage.ToolResponse> responses,
String reasoningContent,
String structuredToolContent,
String textContent
) {}

private final List<ToolCallRound> rounds = new ArrayList<>();

public void addRound(List<AssistantMessage.ToolCall> toolCalls,
List<ToolResponseMessage.ToolResponse> responses,
String structuredToolContent) {
rounds.add(new ToolCallRound(
Collections.unmodifiableList(toolCalls),
Collections.unmodifiableList(responses),
null,
structuredToolContent,
null
));
}

public void setLastRoundReasoning(String reasoningContent) {
    if (rounds.isEmpty()) return;
    ToolCallRound last = rounds.get(rounds.size() - 1);
    rounds.set(rounds.size() - 1, new ToolCallRound(
        last.toolCalls(), last.responses(), reasoningContent, last.structuredToolContent(), last.textContent()
    ));
}

public void setLastRoundTextAndReasoning(String textContent, String reasoningContent) {
    if (rounds.isEmpty()) return;
    ToolCallRound last = rounds.get(rounds.size() - 1);
    rounds.set(rounds.size() - 1, new ToolCallRound(
        last.toolCalls(), last.responses(), reasoningContent, last.structuredToolContent(), textContent
    ));
}

    public List<ToolCallRound> getRounds() {
        return Collections.unmodifiableList(rounds);
    }

    public boolean isEmpty() {
        return rounds.isEmpty();
    }
}
