package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.nomic;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.nomic.NomicEmbeddingModel;

/**
 * Nomic AI 工厂：embedding（nomic-embed-text-v1 / v1.5）。只有 embedding 能力。
 */
public class Langchain4jNomicModelFactory implements ModelFactory {

    @Override
    public String provider() {
        return "nomic";
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(Langchain4jOptionsReader o) {
        return NomicEmbeddingModel.builder()
                .baseUrl(o.string("base-url", null))
                .apiKey(o.required("api-key"))
                .modelName(o.required("model-name"))
                .taskType(o.string("task-type", null))
                .maxSegmentsPerBatch(nullIfZero(o.integer("max-segments-per-batch", null)))
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
