package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding.provider;

import com.xiongx9527.fw.plus.ai.langchain4j.adapter.EmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.EmbeddingStoreProviderAdapter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.util.Collections;

public class Neo4jEmbeddingStoreProviderAdapter implements EmbeddingStoreProviderAdapter {

    private final LangChain4jEmbeddingStoreProviderAdapter delegate =
            new LangChain4jEmbeddingStoreProviderAdapter(Collections.singletonMap("neo4j",
                    "dev.langchain4j.store.embedding.neo4j.Neo4jEmbeddingStore"));

    @Override
    public boolean supports(String provider) {
        return ProviderAdapterSupport.supports(provider, "neo4j");
    }

    @Override
    public EmbeddingStore<TextSegment> create(EmbeddingStoreDefinition definition) {
        try {
            ProviderAdapterSupport.requiredIntegerProperty(definition, "dimension");
            if (definition.getProperties().get("driver") == null) {
                ProviderAdapterSupport.requiredStringProperty(definition, "uri");
                ProviderAdapterSupport.requiredStringProperty(definition, "username");
                ProviderAdapterSupport.requiredStringProperty(definition, "password");
            }
            return delegate.create(definition);
        } catch (Exception ex) {
            throw ProviderAdapterSupport.exception(definition, ex);
        }
    }
}
