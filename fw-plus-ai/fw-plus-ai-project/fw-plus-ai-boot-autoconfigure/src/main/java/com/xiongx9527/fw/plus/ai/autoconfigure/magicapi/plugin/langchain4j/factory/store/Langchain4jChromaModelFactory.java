package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;

import java.time.Duration;

/**
 * Chroma 向量库工厂：常用 {@code base-url/collection}。
 */
public class Langchain4jChromaModelFactory implements ModelFactory {

    private static final String DEFAULT_BASE_URL = "http://localhost:8000";
    private static final String DEFAULT_COLLECTION = "embeddings";

    @Override
    public String provider() {
        return "chroma";
    }

    @Override
    public EmbeddingStore<TextSegment> buildEmbeddingStore(Langchain4jOptionsReader o, EmbeddingModel embeddingModel) {
        return ChromaEmbeddingStore.builder()
                .baseUrl(o.string("base-url", DEFAULT_BASE_URL))
                .collectionName(o.string("collection", DEFAULT_COLLECTION))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .timeout(o.duration("timeout", null))
                .build();
    }
}
