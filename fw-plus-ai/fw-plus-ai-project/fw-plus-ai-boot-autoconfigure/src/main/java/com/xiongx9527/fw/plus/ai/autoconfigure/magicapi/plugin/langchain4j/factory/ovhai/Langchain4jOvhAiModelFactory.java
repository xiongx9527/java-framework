package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ovhai;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ovhai.OvhAiEmbeddingModel;

/**
 * OVHcloud AI Endpoints 工厂：只有 embedding 能力。必填 {@code api-key}，
 * 可选 {@code base-url}（AI Endpoints 按模型而定）。
 */
public class Langchain4jOvhAiModelFactory implements ModelFactory {

    @Override
    public String provider() {
        return "ovh-ai";
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(Langchain4jOptionsReader o) {
        return OvhAiEmbeddingModel.builder()
                .baseUrl(o.string("base-url", null))
                .apiKey(o.required("api-key"))
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
