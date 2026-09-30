package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;

/**
 * Pinecone 向量库工厂：必填 {@code api-key}，可选 {@code index}、{@code namespace}。
 */
public class Langchain4jPineconeModelFactory implements ModelFactory {

    @Override
    public String provider() {
        return "pinecone";
    }

    @Override
    public EmbeddingStore<TextSegment> buildEmbeddingStore(Langchain4jOptionsReader o, EmbeddingModel embeddingModel) {
        return PineconeEmbeddingStore.builder()
                .apiKey(o.required("api-key"))
                .index(o.string("index", null))
                .nameSpace(o.string("namespace", null))
                .build();
    }
}
