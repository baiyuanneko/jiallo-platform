package moe.byn.bynspring21.utils;

import moe.byn.bynspring21.entity.dto.CreateChatModelDto;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.OpenAiChatModel;

public final class SpringAiUtils {

    private SpringAiUtils() {
    }

    public static ChatModel createChatModel(CreateChatModelDto dto) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(dto.getBaseUrl())
                .apiKey(dto.getApiKey())
                .build();

        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .model(dto.getModelName());

        if (dto.getExtraBody() != null && !dto.getExtraBody().isEmpty()) {
            optionsBuilder.extraBody(dto.getExtraBody());
        }

        OpenAiChatOptions options = optionsBuilder.build();

        // Wrap API to inject reasoning_content into assistant messages for
        // DeepSeek thinking mode compatibility in multi-turn conversations.
        OpenAiApi reasoningAwareApi = ReasoningAwareOpenAiApi.wrap(openAiApi);

        return OpenAiChatModel.builder()
                .openAiApi(reasoningAwareApi)
                .defaultOptions(options)
                .build();
    }

    public static ChatClient createChatClientFromModel(ChatModel chatModel) {
        return ChatClient.create(chatModel);
    }
}
