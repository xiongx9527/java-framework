package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;

/**
 * Qdrant 向量库工厂：dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore builder 链。
 * qdrant 从 collection 自动探测维度，不校验 dimension 参数。
 */
public class Langchain4jQdrantModelFactory implements ModelFactory {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 6334;
    private static final String DEFAULT_COLLECTION_NAME = "magic_langchain4j";

    @Override
    public String provider() {
        return "qdrant";
    }

    @Override
    public EmbeddingStore<TextSegment> buildEmbeddingStore(Langchain4jOptionsReader o, EmbeddingModel embeddingModel) {
        return QdrantEmbeddingStore.builder()
                .host(o.string("host", DEFAULT_HOST))
                .port(o.integer("port", DEFAULT_PORT))
                .collectionName(o.string("collection-name", DEFAULT_COLLECTION_NAME))
                .useTls(o.bool("use-tls", false))
                .payloadTextKey(o.string("payload-text-key", "content"))
                .apiKey(o.string("api-key", null))
                .build();
    }
}
