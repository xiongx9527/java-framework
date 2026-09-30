package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.opensearch.OpenSearchEmbeddingStore;

/**
 * OpenSearch 向量库工厂：必填 {@code server-url}，可选 {@code user/password/index-name/api-key}。
 */
public class Langchain4jOpenSearchModelFactory implements ModelFactory {

    private static final String DEFAULT_INDEX_NAME = "magic-langchain4j";

    @Override
    public String provider() {
        return "opensearch";
    }

    @Override
    public EmbeddingStore<TextSegment> buildEmbeddingStore(Langchain4jOptionsReader o, EmbeddingModel embeddingModel) {
        return OpenSearchEmbeddingStore.builder()
                .serverUrl(o.required("server-url"))
                .userName(o.string("user", null))
                .password(o.string("password", null))
                .apiKey(o.string("api-key", null))
                .indexName(o.string("index-name", DEFAULT_INDEX_NAME))
                .build();
    }
}
