package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;

/**
 * Elasticsearch 向量库工厂：dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore builder 链。
 * ES 需要显式 dimension，走 dimension 回退链（配置值 → EmbeddingModel.dimension()）。
 */
public class Langchain4jElasticsearchModelFactory implements ModelFactory {

    private static final String DEFAULT_SERVER_URL = "http://localhost:9200";
    private static final String DEFAULT_INDEX_NAME = "magic_langchain4j";

    @Override
    public String provider() {
        return "elasticsearch";
    }

    @Override
    public EmbeddingStore<TextSegment> buildEmbeddingStore(Langchain4jOptionsReader o, EmbeddingModel embeddingModel) {
        Integer dimension = EmbeddingStoreDimensions.resolve(o, embeddingModel);
        return ElasticsearchEmbeddingStore.builder()
                .serverUrl(o.string("server-url", DEFAULT_SERVER_URL))
                .apiKey(o.string("api-key", null))
                .userName(o.string("username", null))
                .password(o.string("password", null))
                .indexName(o.string("index-name", DEFAULT_INDEX_NAME))
                .dimension(dimension)
                .build();
    }
}
