package com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.autoconfigure.provider;

import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreProviderAdapter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;

public class MilvusEmbeddingStoreProviderAdapter implements EmbeddingStoreProviderAdapter {

    @Override
    public boolean supports(String provider) {
        return ProviderAdapterSupport.supports(provider, "milvus");
    }

    @Override
    public EmbeddingStore<TextSegment> create(EmbeddingStoreDefinition definition) {
        try {
            return configureBuilder(definition).build();
        } catch (Exception ex) {
            throw ProviderAdapterSupport.exception(definition, ex);
        }
    }

    MilvusEmbeddingStore.Builder configureBuilder(EmbeddingStoreDefinition definition) {
        try {
            ProviderAdapterSupport.requireOneStringProperty(definition, "host", "uri");
            ProviderAdapterSupport.requiredStringProperty(definition, "collectionName");
            ProviderAdapterSupport.requiredIntegerProperty(definition, "dimension");
            String host = ProviderAdapterSupport.stringProperty(definition, "host");
            if (host != null && host.trim().length() == 0) {
                throw new IllegalArgumentException("host must not be blank");
            }
            String apiKey = ProviderAdapterSupport.stringProperty(definition, "apiKey");
            if (apiKey == null) {
                apiKey = ProviderAdapterSupport.stringProperty(definition, "token");
            }
            MilvusEmbeddingStore.Builder builder = MilvusEmbeddingStore.builder()
                    .host(host)
                    .port(ProviderAdapterSupport.integerProperty(definition, "port"))
                    .uri(ProviderAdapterSupport.stringProperty(definition, "uri"))
                    .collectionName(ProviderAdapterSupport.stringProperty(definition, "collectionName"))
                    .dimension(ProviderAdapterSupport.integerProperty(definition, "dimension"))
                    .token(apiKey)
                    .username(ProviderAdapterSupport.stringProperty(definition, "username"))
                    .password(ProviderAdapterSupport.stringProperty(definition, "password"))
                    .databaseName(ProviderAdapterSupport.stringProperty(definition, "databaseName"));
            IndexType indexType = ProviderAdapterSupport.enumProperty(definition, "indexType", IndexType.class);
            if (indexType != null) {
                builder.indexType(indexType);
            }
            MetricType metricType = ProviderAdapterSupport.enumProperty(definition, "metricType", MetricType.class);
            if (metricType != null) {
                builder.metricType(metricType);
            }
            ConsistencyLevelEnum consistencyLevel = ProviderAdapterSupport.enumProperty(definition,
                    "consistencyLevel", ConsistencyLevelEnum.class);
            if (consistencyLevel != null) {
                builder.consistencyLevel(consistencyLevel);
            }
            Boolean retrieveEmbeddingsOnSearch = ProviderAdapterSupport.booleanProperty(definition,
                    "retrieveEmbeddingsOnSearch");
            if (retrieveEmbeddingsOnSearch != null) {
                builder.retrieveEmbeddingsOnSearch(retrieveEmbeddingsOnSearch);
            }
            Boolean autoFlushOnInsert = ProviderAdapterSupport.booleanProperty(definition, "autoFlushOnInsert");
            if (autoFlushOnInsert != null) {
                builder.autoFlushOnInsert(autoFlushOnInsert);
            }
            return builder;
        } catch (Exception ex) {
            throw ProviderAdapterSupport.exception(definition, ex);
        }
    }
}
