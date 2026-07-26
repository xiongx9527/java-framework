package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding.provider;

public class AzureCosmosMongoVCoreEmbeddingStoreProviderAdapter extends ReflectiveEmbeddingStoreProviderAdapter {

    public AzureCosmosMongoVCoreEmbeddingStoreProviderAdapter() {
        super("dev.langchain4j.store.embedding.azure.cosmos.mongo.vcore.AzureCosmosDbMongoVCoreEmbeddingStore",
                "azure-cosmos-mongo-vcore");
    }
}
