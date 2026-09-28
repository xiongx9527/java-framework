package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.huggingface;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.model.huggingface.HuggingFaceChatModel;
import dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel;
import dev.langchain4j.model.huggingface.HuggingFaceLanguageModel;

/**
 * HuggingFace 工厂：chat 为 Builder，language/embedding 为多参构造（0.35.0 无 builder）。
 * 凭证为 accessToken（HF Access Token）。
 */
public class Langchain4jHuggingFaceModelFactory implements ModelFactory {

    @Override
    public String provider() {
        return "hugging-face";
    }

    @Override
    public HuggingFaceChatModel buildChatModel(Langchain4jOptionsReader o) {
        return HuggingFaceChatModel.builder()
                .accessToken(o.required("access-token"))
                .modelId(o.string("model-id", "mistralai/Mistral-7B-Instruct-v0.2"))
                .temperature(o.dbl("temperature", null))
                .maxNewTokens(nullIfZero(o.integer("max-new-tokens", null)))
                .returnFullText(o.bool("return-full-text", null))
                .waitForModel(o.bool("wait-for-model", null))
                .timeout(o.duration("timeout", null))
                .build();
    }

    @Override
    public HuggingFaceLanguageModel buildLanguageModel(Langchain4jOptionsReader o) {
        // HuggingFaceLanguageModel 构造：(accessToken, modelId, timeout, temperature, maxNewTokens, returnFullText, waitForModel)
        return new HuggingFaceLanguageModel(
                o.required("access-token"),
                o.string("model-id", "mistralai/Mistral-7B-Instruct-v0.2"),
                o.duration("timeout", null),
                o.dbl("temperature", null),
                nullIfZero(o.integer("max-new-tokens", null)),
                o.bool("return-full-text", null),
                o.bool("wait-for-model", null));
    }

    @Override
    public HuggingFaceEmbeddingModel buildEmbeddingModel(Langchain4jOptionsReader o) {
        // HuggingFaceEmbeddingModel 构造：(accessToken, modelId, waitForModel, timeout)
        return new HuggingFaceEmbeddingModel(
                o.required("access-token"),
                o.string("model-id", "sentence-transformers/all-MiniLM-L6-v2"),
                o.bool("wait-for-model", true),
                o.duration("timeout", null));
    }

    private static Integer nullIfZero(Integer value) {
        return value != null && value == 0 ? null : value;
    }
}
