package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.store;

import com.azure.core.credential.TokenCredential;
import com.azure.search.documents.indexes.models.SearchIndex;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.store.EmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.store.EmbeddingStoreProviderAdapter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.retriever.azure.search.AzureAiSearchFilterMapper;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.azure.search.AzureAiSearchEmbeddingStore;
import org.springframework.context.ApplicationContext;

public class AzureAiSearchEmbeddingStoreProviderAdapter implements EmbeddingStoreProviderAdapter {

    private final ApplicationContext applicationContext;

    public AzureAiSearchEmbeddingStoreProviderAdapter() {
        this(null);
    }

    public AzureAiSearchEmbeddingStoreProviderAdapter(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public boolean supports(String provider) {
        return ProviderAdapterSupport.supports(provider, "azure-ai-search");
    }

    @Override
    public EmbeddingStore<TextSegment> create(EmbeddingStoreDefinition definition) {
        try {
            return configureBuilder(definition).build();
        } catch (Exception ex) {
            throw ProviderAdapterSupport.exception(definition, ex);
        }
    }

    AzureAiSearchEmbeddingStore.Builder configureBuilder(EmbeddingStoreDefinition definition) {
        try {
            ProviderAdapterSupport.requiredStringProperty(definition, "endpoint");
            ProviderAdapterSupport.requiredStringProperty(definition, "indexName");
            Integer dimensions = dimensions(definition);
            if (dimensions == null) {
                throw new IllegalArgumentException("dimensions must not be null");
            }
            AzureAiSearchEmbeddingStore.Builder builder = AzureAiSearchEmbeddingStore.builder()
                    .endpoint(ProviderAdapterSupport.stringProperty(definition, "endpoint"))
                    .apiKey(ProviderAdapterSupport.stringProperty(definition, "apiKey"))
                    .indexName(ProviderAdapterSupport.stringProperty(definition, "indexName"))
                    .dimensions(dimensions.intValue());
            Boolean createOrUpdateIndex = ProviderAdapterSupport.booleanProperty(definition, "createOrUpdateIndex");
            if (createOrUpdateIndex != null) {
                builder.createOrUpdateIndex(createOrUpdateIndex.booleanValue());
            }
            TokenCredential tokenCredential = ProviderAdapterSupport.beanProperty(definition, "tokenCredentialBean",
                    TokenCredential.class, applicationContext);
            if (tokenCredential != null) {
                builder.tokenCredential(tokenCredential);
            }
            SearchIndex index = ProviderAdapterSupport.beanProperty(definition, "indexBean", SearchIndex.class,
                    applicationContext);
            if (index != null) {
                builder.index(index);
            }
            AzureAiSearchFilterMapper filterMapper = ProviderAdapterSupport.beanProperty(definition, "filterMapperBean",
                    AzureAiSearchFilterMapper.class, applicationContext);
            if (filterMapper != null) {
                builder.filterMapper(filterMapper);
            }
            return builder;
        } catch (Exception ex) {
            throw ProviderAdapterSupport.exception(definition, ex);
        }
    }

    private Integer dimensions(EmbeddingStoreDefinition definition) {
        Integer dimensions = ProviderAdapterSupport.integerProperty(definition, "dimensions");
        return dimensions == null ? ProviderAdapterSupport.integerProperty(definition, "dimension") : dimensions;
    }
}
