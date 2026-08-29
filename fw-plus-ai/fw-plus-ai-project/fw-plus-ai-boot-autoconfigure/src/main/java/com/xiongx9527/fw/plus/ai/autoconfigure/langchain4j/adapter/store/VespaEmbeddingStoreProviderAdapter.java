package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.store;

public class VespaEmbeddingStoreProviderAdapter extends ReflectiveEmbeddingStoreProviderAdapter {

    public VespaEmbeddingStoreProviderAdapter() {
        super("dev.langchain4j.store.embedding.vespa.VespaEmbeddingStore", "vespa");
    }
}
