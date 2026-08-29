package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding.provider;

import com.xiongx9527.fw.plus.ai.langchain4j.adapter.store.EmbeddingStoreDefinition;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.context.ApplicationContext;

public class AzureCosmosNoSqlEmbeddingStoreProviderAdapter extends ReflectiveEmbeddingStoreProviderAdapter {

    public AzureCosmosNoSqlEmbeddingStoreProviderAdapter() {
        this(null);
    }

    public AzureCosmosNoSqlEmbeddingStoreProviderAdapter(ApplicationContext applicationContext) {
        super("dev.langchain4j.store.embedding.azure.cosmos.nosql.AzureCosmosDbNoSqlEmbeddingStore",
                applicationContext, "azure-cosmos-nosql");
    }

    @Override
    public EmbeddingStore<TextSegment> create(EmbeddingStoreDefinition definition) {
        EmbeddingStoreDefinition mapped = copyDefinition(definition);
        mapBeanProperty(mapped, "cosmosClientBean", "cosmosClient");
        mapBeanProperty(mapped, "cosmosVectorEmbeddingPolicyBean", "cosmosVectorEmbeddingPolicy");
        mapBeanProperty(mapped, "cosmosVectorIndexesBean", "cosmosVectorIndexes");
        mapBeanProperty(mapped, "containerPropertiesBean", "containerProperties");
        return super.create(mapped);
    }

    @Override
    protected void validate(EmbeddingStoreDefinition definition) {
        ProviderAdapterSupport.requiredStringProperty(definition, "cosmosClient");
        ProviderAdapterSupport.requiredStringProperty(definition, "databaseName");
        ProviderAdapterSupport.requiredStringProperty(definition, "containerName");
    }

    private void mapBeanProperty(EmbeddingStoreDefinition definition, String beanNameProperty, String builderProperty) {
        Object value = definition.getProperties().remove(beanNameProperty);
        if (value != null && !definition.getProperties().containsKey(builderProperty)) {
            definition.getProperties().put(builderProperty, value);
        }
    }
}
