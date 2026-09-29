package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.onnx;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;

/**
 * 本地内置 embedding 模型工厂（onnx 量化模型，离线可用、无需凭证）。
 * 每个短名注册一个实例：new Langchain4jOnnxEmbeddingModelFactory("bge-small") /
 * new Langchain4jOnnxEmbeddingModelFactory("all-minilm")，仅 embedding 能力。
 */
public class Langchain4jOnnxEmbeddingModelFactory implements ModelFactory {

    private final String provider;

    public Langchain4jOnnxEmbeddingModelFactory(String provider) {
        this.provider = provider;
    }

    @Override
    public String provider() {
        return provider;
    }

    @Override
    public dev.langchain4j.model.embedding.EmbeddingModel buildEmbeddingModel(Langchain4jOptionsReader o) {
        if ("all-minilm".equals(provider)) {
            return new AllMiniLmL6V2QuantizedEmbeddingModel();
        }
        return new BgeSmallEnV15QuantizedEmbeddingModel();
    }
}
