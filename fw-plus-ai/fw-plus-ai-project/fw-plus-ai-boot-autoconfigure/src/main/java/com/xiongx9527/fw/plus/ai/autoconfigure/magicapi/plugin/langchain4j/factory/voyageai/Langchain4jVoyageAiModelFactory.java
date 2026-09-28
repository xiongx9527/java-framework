package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.voyageai;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.model.voyageai.VoyageAiEmbeddingModel;

/**
 * Voyage AI 工厂：仅 embedding 能力（另有 ScoringModel，本次不覆盖）。
 */
public class Langchain4jVoyageAiModelFactory implements ModelFactory {

    @Override
    public String provider() {
        return "voyage-ai";
    }

    @Override
    public VoyageAiEmbeddingModel buildEmbeddingModel(Langchain4jOptionsReader o) {
        return VoyageAiEmbeddingModel.builder()
                .baseUrl(o.string("base-url", null))
                .apiKey(o.required("api-key"))
                .modelName(o.string("model-name", "voyage-2"))
                .inputType(o.string("input-type", null))
                .truncation(o.bool("truncation", null))
                .encodingFormat(o.string("encoding-format", null))
                .timeout(o.duration("timeout", null))
                .maxRetries(nullIfZero(o.integer("max-retries", null)))
                .maxSegmentsPerBatch(nullIfZero(o.integer("max-segments-per-batch", null)))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .build();
    }

    private static Integer nullIfZero(Integer value) {
        return value != null && value == 0 ? null : value;
    }
}
