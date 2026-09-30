package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.cohere;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.community.model.CohereChatModel;
import dev.langchain4j.community.model.CohereStreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.cohere.CohereEmbeddingModel;
import dev.langchain4j.model.cohere.CohereScoringModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.util.List;

/**
 * Cohere 工厂：chat/streaming 走 community 版构件，embedding/scoring（rerank）走主线构件；
 * 必填 {@code api-key}，可选 {@code base-url}、{@code thinking-type}（思考模式）等。
 * rerank 的消费端（知识库管理）后续接入，先以 {@link #buildScoringModel} 提供构建能力。
 */
public class Langchain4jCohereModelFactory implements ModelFactory {

    private static final Double DEFAULT_TEMPERATURE = 0.7;

    @Override
    public String provider() {
        return "cohere";
    }

    @Override
    public CohereChatModel buildChatModel(Langchain4jOptionsReader o, List<ChatModelListener> listeners) {
        return CohereChatModel.builder()
                .baseUrl(o.string("base-url", null))
                .apiKey(o.required("api-key"))
                .modelName(o.string("model-name", null))
                .temperature(o.dbl("temperature", DEFAULT_TEMPERATURE))
                .topP(o.dbl("top-p", null))
                .topK(nullIfZero(o.integer("top-k", null)))
                .maxTokens(nullIfZero(o.integer("max-tokens", null)))
                .stopSequences(o.stringList("stop", null))
                .timeout(o.duration("timeout", null))
                .maxRetries(nullIfZero(o.integer("max-retries", null)))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .listeners(listeners)
                .build();
    }

    @Override
    public CohereStreamingChatModel buildStreamingChatModel(Langchain4jOptionsReader o,
            List<ChatModelListener> listeners) {
        return CohereStreamingChatModel.builder()
                .baseUrl(o.string("base-url", null))
                .apiKey(o.required("api-key"))
                .modelName(o.string("model-name", null))
                .temperature(o.dbl("temperature", DEFAULT_TEMPERATURE))
                .topP(o.dbl("top-p", null))
                .topK(nullIfZero(o.integer("top-k", null)))
                .maxTokens(nullIfZero(o.integer("max-tokens", null)))
                .stopSequences(o.stringList("stop", null))
                .timeout(o.duration("timeout", null))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .listeners(listeners)
                .build();
    }

    /** rerank 模型（ScoringModel，主线构件），知识库管理消费端接入后启用 */
    @Override
    public CohereScoringModel buildScoringModel(Langchain4jOptionsReader o) {
        return CohereScoringModel.builder()
                .baseUrl(o.string("base-url", null))
                .apiKey(o.required("api-key"))
                .modelName(o.required("model-name"))
                .timeout(o.duration("timeout", null))
                .maxRetries(nullIfZero(o.integer("max-retries", null)))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .build();
    }

    /** embedding（主线构件） */
    @Override
    public EmbeddingModel buildEmbeddingModel(Langchain4jOptionsReader o) {
        return CohereEmbeddingModel.builder()
                .baseUrl(o.string("base-url", null))
                .apiKey(o.required("api-key"))
                .modelName(o.required("model-name"))
                .inputType(o.string("input-type", null))
                .timeout(o.duration("timeout", null))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .build();
    }

    private static Integer nullIfZero(Integer value) {
        return value != null && value == 0 ? null : value;
    }
}
