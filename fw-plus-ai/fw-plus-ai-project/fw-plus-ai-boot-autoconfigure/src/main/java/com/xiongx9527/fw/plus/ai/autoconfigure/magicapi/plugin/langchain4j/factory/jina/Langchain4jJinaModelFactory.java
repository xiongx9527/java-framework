package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.jina;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.jina.JinaEmbeddingModel;
import dev.langchain4j.model.jina.JinaScoringModel;

/**
 * Jina AI 工厂：embedding（jina-embeddings-v3 等）+ scoring（rerank，jina-reranker-v2）。
 * rerank 的消费端（知识库管理）后续接入，先以 {@link #buildScoringModel} 提供构建能力。
 */
public class Langchain4jJinaModelFactory implements ModelFactory {

    @Override
    public String provider() {
        return "jina";
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(Langchain4jOptionsReader o) {
        return JinaEmbeddingModel.builder()
                .baseUrl(o.string("base-url", null))
                .apiKey(o.required("api-key"))
                .modelName(o.required("model-name"))
                .lateChunking(o.bool("late-chunking", null))
                .timeout(o.duration("timeout", null))
                .maxRetries(nullIfZero(o.integer("max-retries", null)))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .build();
    }

    /** rerank 模型（ScoringModel），知识库管理消费端接入后启用 */
    public JinaScoringModel buildScoringModel(Langchain4jOptionsReader o) {
        return JinaScoringModel.builder()
                .baseUrl(o.string("base-url", null))
                .apiKey(o.required("api-key"))
                .modelName(o.required("model-name"))
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
