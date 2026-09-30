package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.openaiofficial;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.openaiofficial.OpenAiOfficialChatModel;
import dev.langchain4j.model.openaiofficial.OpenAiOfficialEmbeddingModel;
import dev.langchain4j.model.openaiofficial.OpenAiOfficialStreamingChatModel;

import java.util.List;

/**
 * OpenAI 官方 SDK 版工厂（beta 线）：基于 openai-java 官方 SDK，与 openai 工厂（自研 HTTP）互补。
 * 也兼容 Azure OpenAI / GitHub Models（options: is-azure / is-github-models 等）。
 */
public class Langchain4jOpenAiOfficialModelFactory implements ModelFactory {

    private static final Double DEFAULT_TEMPERATURE = 0.7;

    @Override
    public String provider() {
        return "openai-official";
    }

    @Override
    public OpenAiOfficialChatModel buildChatModel(Langchain4jOptionsReader o, List<ChatModelListener> listeners) {
        return OpenAiOfficialChatModel.builder()
                .baseUrl(o.string("base-url", null))
                .apiKey(o.required("api-key"))
                .modelName(o.required("model-name"))
                .organizationId(o.string("organization-id", null))
                .isAzure(o.bool("is-azure", null))
                .isGitHubModels(o.bool("is-github-models", null))
                .temperature(o.dbl("temperature", DEFAULT_TEMPERATURE))
                .topP(o.dbl("top-p", null))
                .maxCompletionTokens(nullIfZero(o.integer("max-completion-tokens", null)))
                .presencePenalty(o.dbl("presence-penalty", null))
                .frequencyPenalty(o.dbl("frequency-penalty", null))
                .seed(nullIfZero(o.integer("seed", null)))
                .user(o.string("user", null))
                .timeout(o.duration("timeout", null))
                .maxRetries(nullIfZero(o.integer("max-retries", null)))
                .listeners(listeners)
                .build();
    }

    @Override
    public OpenAiOfficialStreamingChatModel buildStreamingChatModel(Langchain4jOptionsReader o,
            List<ChatModelListener> listeners) {
        return OpenAiOfficialStreamingChatModel.builder()
                .baseUrl(o.string("base-url", null))
                .apiKey(o.required("api-key"))
                .modelName(o.required("model-name"))
                .organizationId(o.string("organization-id", null))
                .isAzure(o.bool("is-azure", null))
                .isGitHubModels(o.bool("is-github-models", null))
                .temperature(o.dbl("temperature", DEFAULT_TEMPERATURE))
                .topP(o.dbl("top-p", null))
                .maxCompletionTokens(nullIfZero(o.integer("max-completion-tokens", null)))
                .presencePenalty(o.dbl("presence-penalty", null))
                .frequencyPenalty(o.dbl("frequency-penalty", null))
                .seed(nullIfZero(o.integer("seed", null)))
                .user(o.string("user", null))
                .timeout(o.duration("timeout", null))
                .listeners(listeners)
                .build();
    }

    @Override
    public OpenAiOfficialEmbeddingModel buildEmbeddingModel(Langchain4jOptionsReader o) {
        return OpenAiOfficialEmbeddingModel.builder()
                .baseUrl(o.string("base-url", null))
                .apiKey(o.required("api-key"))
                .modelName(o.required("model-name"))
                .timeout(o.duration("timeout", null))
                .maxRetries(nullIfZero(o.integer("max-retries", null)))
                .build();
    }

    private static Integer nullIfZero(Integer value) {
        return value != null && value == 0 ? null : value;
    }
}
