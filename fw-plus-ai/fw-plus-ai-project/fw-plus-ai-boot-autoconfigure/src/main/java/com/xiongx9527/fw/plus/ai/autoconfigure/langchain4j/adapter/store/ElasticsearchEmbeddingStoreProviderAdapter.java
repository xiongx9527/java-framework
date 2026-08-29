package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.store;

import com.xiongx9527.fw.plus.ai.langchain4j.adapter.store.EmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.store.EmbeddingStoreProviderAdapter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;

public class ElasticsearchEmbeddingStoreProviderAdapter implements EmbeddingStoreProviderAdapter {

    @Override
    public boolean supports(String provider) {
        return ProviderAdapterSupport.supports(provider, "elasticsearch");
    }

    @Override
    public EmbeddingStore<TextSegment> create(EmbeddingStoreDefinition definition) {
        try {
            ProviderAdapterSupport.requiredStringProperty(definition, "uri");
            ProviderAdapterSupport.requiredStringProperty(definition, "indexName");
            ProviderAdapterSupport.requiredIntegerProperty(definition, "dimension");
            return ElasticsearchEmbeddingStore.builder()
                    .serverUrl(ProviderAdapterSupport.stringProperty(definition, "uri"))
                    .apiKey(ProviderAdapterSupport.stringProperty(definition, "apiKey"))
                    .userName(ProviderAdapterSupport.stringProperty(definition, "username"))
                    .password(ProviderAdapterSupport.stringProperty(definition, "password"))
                    .indexName(ProviderAdapterSupport.stringProperty(definition, "indexName"))
                    .dimension(ProviderAdapterSupport.integerProperty(definition, "dimension"))
                    .build();
        } catch (Exception ex) {
            throw ProviderAdapterSupport.exception(definition, ex);
        }
    }
}
