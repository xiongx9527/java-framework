package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding.provider;

public class VearchEmbeddingStoreProviderAdapter extends ReflectiveEmbeddingStoreProviderAdapter {

    public VearchEmbeddingStoreProviderAdapter() {
        super("dev.langchain4j.store.embedding.vearch.VearchEmbeddingStore", "vearch");
    }
}
