package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.cohere;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.model.cohere.CohereEmbeddingModel;
import dev.langchain4j.model.cohere.CohereScoringModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.scoring.ScoringModel;

/**
 * Cohere 工厂：embedding（embed-multilingual-v3.0 等）+ scoring（rerank-multilingual-v3.0）。
 */
public class Langchain4jCohereModelFactory implements ModelFactory {

    @Override
    public String provider() {
        return "cohere";
    }

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

    @Override
    public ScoringModel buildScoringModel(Langchain4jOptionsReader o) {
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

    private static Integer nullIfZero(Integer value) {
        return value != null && value == 0 ? null : value;
    }
}
