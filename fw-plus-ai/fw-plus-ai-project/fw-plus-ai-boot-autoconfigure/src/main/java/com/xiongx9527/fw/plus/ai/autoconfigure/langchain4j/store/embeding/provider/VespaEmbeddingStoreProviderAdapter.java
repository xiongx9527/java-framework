package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding.provider;

public class VespaEmbeddingStoreProviderAdapter extends ReflectiveEmbeddingStoreProviderAdapter {

    public VespaEmbeddingStoreProviderAdapter() {
        super("dev.langchain4j.store.embedding.vespa.VespaEmbeddingStore", "vespa");
    }
}
