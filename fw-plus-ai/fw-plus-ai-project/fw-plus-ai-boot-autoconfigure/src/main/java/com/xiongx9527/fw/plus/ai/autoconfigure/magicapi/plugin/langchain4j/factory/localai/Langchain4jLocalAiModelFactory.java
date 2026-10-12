package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.localai;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.localai.LocalAiChatModel;
import dev.langchain4j.model.localai.LocalAiEmbeddingModel;
import dev.langchain4j.model.localai.LocalAiStreamingChatModel;
import java.util.List;

/**
 * LocalAI 工厂：OpenAI 兼容本地推理服务，无需 api-key。
 * streaming 两个 builder 无 maxRetries（0.35.0）。
 */
public class Langchain4jLocalAiModelFactory implements ModelFactory {

    // 注意：0.35.0 该 SDK builder 无 listeners(...)，本 provider 忽略 listeners

    private static final String DEFAULT_BASE_URL = "http://localhost:8080";

    @Override
    public String provider() {
        return "local-ai";
    }

    @Override
    public LocalAiChatModel buildChatModel(Langchain4jOptionsReader o, List<ChatModelListener> listeners) {
        return LocalAiChatModel.builder()
                .baseUrl(o.string("base-url", DEFAULT_BASE_URL))
                .modelName(o.string("model-name", "gpt-4"))
                .temperature(o.dbl("temperature", null))
                .topP(o.dbl("top-p", null))
                .maxTokens(nullIfZero(o.integer("max-tokens", null)))
                .timeout(o.duration("timeout", null))
                .maxRetries(nullIfZero(o.integer("max-retries", null)))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .build();
    }

    @Override
    public LocalAiStreamingChatModel buildStreamingChatModel(Langchain4jOptionsReader o, List<ChatModelListener> listeners) {
        return LocalAiStreamingChatModel.builder()
                .baseUrl(o.string("base-url", DEFAULT_BASE_URL))
                .modelName(o.string("model-name", "gpt-4"))
                .temperature(o.dbl("temperature", null))
                .topP(o.dbl("top-p", null))
                .maxTokens(nullIfZero(o.integer("max-tokens", null)))
                .timeout(o.duration("timeout", null))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .build();
    }



    @Override
    public LocalAiEmbeddingModel buildEmbeddingModel(Langchain4jOptionsReader o) {
        return LocalAiEmbeddingModel.builder()
                .baseUrl(o.string("base-url", DEFAULT_BASE_URL))
                .modelName(o.string("model-name", "text-embedding-ada-002"))
                .timeout(o.duration("timeout", null))
                .maxRetries(nullIfZero(o.integer("max-retries", null)))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .build();
    }

    private static Integer nullIfZero(Integer value) {
        return value != null && value == 0 ? null : value;
    }
}
