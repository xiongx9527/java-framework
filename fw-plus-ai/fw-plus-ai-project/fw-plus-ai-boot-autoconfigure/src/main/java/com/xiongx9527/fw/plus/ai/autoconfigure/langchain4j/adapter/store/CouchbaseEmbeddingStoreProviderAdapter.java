package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.store;

public class CouchbaseEmbeddingStoreProviderAdapter extends ReflectiveEmbeddingStoreProviderAdapter {

    public CouchbaseEmbeddingStoreProviderAdapter() {
        super("dev.langchain4j.store.embedding.couchbase.CouchbaseEmbeddingStore", "couchbase");
    }
}
