package moe.byn.bynspring21.utils;

import org.springframework.ai.model.ApiKey;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletion;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionChunk;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionMessage;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionRequest;
import org.springframework.ai.openai.api.OpenAiStreamFunctionCallingHelper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An {@link OpenAiApi} wrapper that patches assistant {@link ChatCompletionMessage}
 * objects with {@code reasoning_content} before they are sent to the API.
 * <p>
 * DeepSeek's thinking mode requires that previous assistant messages in a
 * multi-turn conversation carry the {@code reasoning_content} field. Spring AI
 * 1.1.2's {@code OpenAiChatModel.createRequest} does not populate this field
 * from {@code AssistantMessage.getProperties()}, so this wrapper fills the gap
 * using position-indexed data provided by {@link ReasoningContentHelper}.
 */
@Slf4j
public class ReasoningAwareOpenAiApi extends OpenAiApi {

    /**
     * Creates a wrapper around the given {@link OpenAiApi} that shares the same
     * HTTP client configuration.
     */
    public static ReasoningAwareOpenAiApi wrap(OpenAiApi original) {
        try {
            String baseUrl = extractField(original, "baseUrl");
            ApiKey apiKey = extractField(original, "apiKey");
            MultiValueMap<String, String> headers = extractField(original, "headers");
            String completionsPath = extractField(original, "completionsPath");
            String embeddingsPath = extractField(original, "embeddingsPath");
            RestClient restClient = extractField(original, "restClient");
            WebClient webClient = extractField(original, "webClient");
            ResponseErrorHandler responseErrorHandler = extractField(original, "responseErrorHandler");

            return new ReasoningAwareOpenAiApi(
                    baseUrl, apiKey, headers, completionsPath, embeddingsPath,
                    restClient.mutate(), webClient.mutate(), responseErrorHandler
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to wrap OpenAiApi with reasoning content support", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T extractField(Object target, String fieldName) throws Exception {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return (T) field.get(target);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException("Field '" + fieldName + "' not found in class hierarchy of " + target.getClass());
    }

    ReasoningAwareOpenAiApi(String baseUrl, ApiKey apiKey,
                            MultiValueMap<String, String> headers,
                            String completionsPath, String embeddingsPath,
                            RestClient.Builder restClientBuilder,
                            WebClient.Builder webClientBuilder,
                            ResponseErrorHandler responseErrorHandler) {
        super(baseUrl, apiKey, headers, completionsPath, embeddingsPath,
                restClientBuilder, webClientBuilder, responseErrorHandler);
    }

    @Override
    public ResponseEntity<ChatCompletion> chatCompletionEntity(
            ChatCompletionRequest request,
            MultiValueMap<String, String> additionalHttpHeaders) {
        return super.chatCompletionEntity(patchRequest(request), additionalHttpHeaders);
    }

    @Override
    public ResponseEntity<ChatCompletion> chatCompletionEntity(
            ChatCompletionRequest request) {
        return super.chatCompletionEntity(patchRequest(request));
    }

    @Override
    public Flux<ChatCompletionChunk> chatCompletionStream(
            ChatCompletionRequest request,
            MultiValueMap<String, String> additionalHttpHeaders) {

        ChatCompletionRequest patchedRequest = patchRequest(request);

        try {
            WebClient webClient = extractField(this, "webClient");
            String completionsPath = extractField(this, "completionsPath");
            ApiKey apiKey = extractField(this, "apiKey");
            OpenAiStreamFunctionCallingHelper chunkMerger = extractField(this, "chunkMerger");
            AtomicBoolean isInsideTool = new AtomicBoolean(false);

            return webClient.post()
                    .uri(completionsPath)
                    .headers(headers -> {
                        headers.addAll(additionalHttpHeaders);
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        if (!headers.containsKey(HttpHeaders.AUTHORIZATION)
                                && apiKey != null && apiKey.getValue() != null && !apiKey.getValue().isEmpty()) {
                            headers.setBearerAuth(apiKey.getValue());
                        }
                    })
                    .bodyValue(patchedRequest)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .takeUntil(s -> "[DONE]".equals(s))
                    .filter(s -> !"[DONE]".equals(s))
                    .map(this::fixReasoningFieldName)
                    .map(content -> ModelOptionsUtils.jsonToObject(content, ChatCompletionChunk.class))
                    .map(chunk -> {
                        if (chunkMerger.isStreamingToolFunctionCall(chunk)) {
                            isInsideTool.set(true);
                        }
                        return chunk;
                    })
                    .windowUntil(chunk -> {
                        if (isInsideTool.get() && chunkMerger.isStreamingToolFunctionCallFinish(chunk)) {
                            isInsideTool.set(false);
                            return true;
                        }
                        return !isInsideTool.get();
                    })
                    .concatMapIterable(window -> {
                        Mono<ChatCompletionChunk> monoChunk = window.reduce(
                                new ChatCompletionChunk(null, null, null, null, null, null, null, null),
                                (previous, current) -> chunkMerger.merge(previous, current));
                        return List.of(monoChunk);
                    })
                    .flatMap(mono -> mono);

        } catch (Exception e) {
            log.error("Failed to access OpenAiApi internals via reflection, falling back to super", e);
            return super.chatCompletionStream(patchedRequest, additionalHttpHeaders);
        }
    }

    @Override
    public Flux<ChatCompletionChunk> chatCompletionStream(
            ChatCompletionRequest request) {
        return chatCompletionStream(request, new org.springframework.util.LinkedMultiValueMap<>());
    }

    private ChatCompletionRequest patchRequest(ChatCompletionRequest request) {
        if (!ReasoningContentHelper.hasEntries()) {
            return request;
        }

        List<ChatCompletionMessage> originalMessages = request.messages();
        boolean changed = false;

        ChatCompletionMessage[] patched = new ChatCompletionMessage[originalMessages.size()];
        for (int i = 0; i < originalMessages.size(); i++) {
            ChatCompletionMessage msg = originalMessages.get(i);
            if (msg.role() == ChatCompletionMessage.Role.ASSISTANT) {
                String reasoningContent = ReasoningContentHelper.getReasoningContent(i);
                if (reasoningContent != null && !reasoningContent.equals(msg.reasoningContent())) {
                    patched[i] = new ChatCompletionMessage(
                            msg.rawContent(), msg.role(), msg.name(), msg.toolCallId(),
                            msg.toolCalls(), msg.refusal(), msg.audioOutput(),
                            msg.annotations(), reasoningContent
                    );
                    changed = true;
                    continue;
                }
            }
            patched[i] = msg;
        }

        if (!changed) {
            return request;
        }

        return new ChatCompletionRequest(
                List.of(patched), request.model(), request.store(), request.metadata(),
                request.frequencyPenalty(), request.logitBias(), request.logprobs(),
                request.topLogprobs(), request.maxTokens(), request.maxCompletionTokens(),
                request.n(), request.outputModalities(), request.audioParameters(),
                request.presencePenalty(), request.responseFormat(), request.seed(),
                request.serviceTier(), request.stop(), request.stream(),
                request.streamOptions(), request.temperature(), request.topP(),
                request.tools(), request.toolChoice(), request.parallelToolCalls(),
                request.user(), request.reasoningEffort(), request.webSearchOptions(),
                request.verbosity(), request.promptCacheKey(), request.safetyIdentifier(),
                request.extraBody()
        );
    }

    /**
     * Maps provider-specific {@code "reasoning"} JSON field to
     * {@code "reasoning_content"} before Jackson deserialization.
     * <p>
     * Some OpenAI-compatible providers (e.g., AtlasCloud, StepFun without
     * {@code reasoning_format=deepseek-style}) return thinking content under
     * the field name {@code reasoning} rather than the standard
     * {@code reasoning_content} that Spring AI's
     * {@link ChatCompletionMessage} expects via
     * {@code @JsonProperty("reasoning_content")}.
     * <p>
     * This pre-processing step renames the field so the thinking content is
     * properly deserialized and propagated to the SSE output.
     */
    private String fixReasoningFieldName(String json) {
        JSONObject obj = JSON.parseObject(json);
        JSONArray choices = obj.getJSONArray("choices");
        if (choices != null && !choices.isEmpty()) {
            JSONObject delta = choices.getJSONObject(0).getJSONObject("delta");
            if (delta != null && delta.containsKey("reasoning") && !delta.containsKey("reasoning_content")) {
                delta.put("reasoning_content", delta.remove("reasoning"));
                return obj.toJSONString();
            }
        }
        return json;
    }
}
