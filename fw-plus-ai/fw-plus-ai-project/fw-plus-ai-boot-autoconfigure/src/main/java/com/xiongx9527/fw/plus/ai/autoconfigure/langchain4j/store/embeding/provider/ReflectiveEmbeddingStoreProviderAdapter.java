package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding.provider;

import com.xiongx9527.fw.plus.ai.langchain4j.adapter.EmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.EmbeddingStoreProviderAdapter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.context.ApplicationContext;

import java.util.LinkedHashMap;
import java.util.Map;

abstract class ReflectiveEmbeddingStoreProviderAdapter implements EmbeddingStoreProviderAdapter {

    private final String[] providers;

    private final LangChain4jEmbeddingStoreProviderAdapter delegate;

    ReflectiveEmbeddingStoreProviderAdapter(String className, String... providers) {
        this(className, null, providers);
    }

    ReflectiveEmbeddingStoreProviderAdapter(String className, ApplicationContext applicationContext, String... providers) {
        this.providers = providers;
        Map<String, String> mappings = new LinkedHashMap<String, String>();
        for (String provider : providers) {
            mappings.put(provider, className);
        }
        this.delegate = new LangChain4jEmbeddingStoreProviderAdapter(mappings, applicationContext);
    }

    @Override
    public boolean supports(String provider) {
        for (String supportedProvider : providers) {
            if (ProviderAdapterSupport.supports(provider, supportedProvider)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public EmbeddingStore<TextSegment> create(EmbeddingStoreDefinition definition) {
        try {
            validate(definition);
            return delegate.create(definition);
        } catch (Exception ex) {
            throw ProviderAdapterSupport.exception(definition, ex);
        }
    }

    protected void validate(EmbeddingStoreDefinition definition) {
    }

    protected EmbeddingStoreDefinition copyDefinition(EmbeddingStoreDefinition source) {
        EmbeddingStoreDefinition target = new EmbeddingStoreDefinition();
        target.setKey(source.getKey());
        target.setProvider(source.getProvider());
        target.setEnabled(source.isEnabled());
        target.setDimension(source.getDimension());
        target.getMetadataKeys().addAll(source.getMetadataKeys());
        target.getProperties().putAll(source.getProperties());
        return target;
    }
}
