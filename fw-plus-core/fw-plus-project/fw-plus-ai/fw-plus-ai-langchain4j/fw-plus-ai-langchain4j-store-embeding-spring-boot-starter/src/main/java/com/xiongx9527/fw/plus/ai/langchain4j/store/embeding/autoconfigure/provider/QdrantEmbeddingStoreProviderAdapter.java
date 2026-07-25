package com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.autoconfigure.provider;

import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreProviderAdapter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;

public class QdrantEmbeddingStoreProviderAdapter implements EmbeddingStoreProviderAdapter {

    @Override
    public boolean supports(String provider) {
        return ProviderAdapterSupport.supports(provider, "qdrant");
    }

    @Override
    public EmbeddingStore<TextSegment> create(EmbeddingStoreDefinition definition) {
        try {
            return configureBuilder(definition).build();
        } catch (Exception ex) {
            throw ProviderAdapterSupport.exception(definition, ex);
        }
    }

    QdrantEmbeddingStore.Builder configureBuilder(EmbeddingStoreDefinition definition) {
        try {
            ProviderAdapterSupport.requiredStringProperty(definition, "host");
            Integer port = ProviderAdapterSupport.requiredIntegerProperty(definition, "port");
            ProviderAdapterSupport.requiredStringProperty(definition, "collectionName");
            QdrantEmbeddingStore.Builder builder = QdrantEmbeddingStore.builder()
                    .host(ProviderAdapterSupport.stringProperty(definition, "host"))
                    .port(port.intValue())
                    .collectionName(ProviderAdapterSupport.stringProperty(definition, "collectionName"))
                    .apiKey(ProviderAdapterSupport.stringProperty(definition, "apiKey"));
            String payloadTextKey = ProviderAdapterSupport.stringProperty(definition, "payloadTextKey");
            if (payloadTextKey != null) {
                builder.payloadTextKey(payloadTextKey);
            }
            Boolean useTls = ProviderAdapterSupport.booleanProperty(definition, "useTls");
            if (useTls == null) {
                useTls = ProviderAdapterSupport.booleanProperty(definition, "utls");
            }
            if (useTls != null) {
                builder.useTls(useTls.booleanValue());
            }
            return builder;
        } catch (Exception ex) {
            throw ProviderAdapterSupport.exception(definition, ex);
        }
    }
}
