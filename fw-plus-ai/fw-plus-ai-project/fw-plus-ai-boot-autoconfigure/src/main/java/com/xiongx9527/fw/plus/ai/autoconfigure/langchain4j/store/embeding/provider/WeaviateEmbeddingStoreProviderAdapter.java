package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding.provider;

import com.xiongx9527.fw.plus.ai.langchain4j.adapter.EmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.EmbeddingStoreProviderAdapter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.weaviate.WeaviateEmbeddingStore;

public class WeaviateEmbeddingStoreProviderAdapter implements EmbeddingStoreProviderAdapter {

    @Override
    public boolean supports(String provider) {
        return ProviderAdapterSupport.supports(provider, "weaviate");
    }

    @Override
    public EmbeddingStore<TextSegment> create(EmbeddingStoreDefinition definition) {
        try {
            ProviderAdapterSupport.requiredStringProperty(definition, "scheme");
            ProviderAdapterSupport.requiredStringProperty(definition, "host");
            ProviderAdapterSupport.requiredIntegerProperty(definition, "port");
            ProviderAdapterSupport.requiredStringProperty(definition, "className");
            return new WeaviateEmbeddingStore(
                    ProviderAdapterSupport.stringProperty(definition, "apiKey"),
                    ProviderAdapterSupport.stringProperty(definition, "scheme"),
                    ProviderAdapterSupport.stringProperty(definition, "host"),
                    ProviderAdapterSupport.integerProperty(definition, "port"),
                    ProviderAdapterSupport.booleanProperty(definition, "useGrpcForInserts"),
                    ProviderAdapterSupport.booleanProperty(definition, "securedGrpc"),
                    ProviderAdapterSupport.integerProperty(definition, "grpcPort"),
                    ProviderAdapterSupport.stringProperty(definition, "className"),
                    ProviderAdapterSupport.booleanProperty(definition, "avoidDups"),
                    ProviderAdapterSupport.stringProperty(definition, "consistencyLevel"),
                    ProviderAdapterSupport.metadataKeys(definition),
                    ProviderAdapterSupport.stringProperty(definition, "textFieldName"),
                    ProviderAdapterSupport.stringProperty(definition, "metadataFieldName"));
        } catch (Exception ex) {
            throw ProviderAdapterSupport.exception(definition, ex);
        }
    }
}
