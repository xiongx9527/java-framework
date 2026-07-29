package com.xiongx9527.fw.plus.ai.langchain4j.adapter;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.util.Set;

public interface EmbeddingStoreRegistry {

    EmbeddingStore<TextSegment> get(String key);

    EmbeddingStore<TextSegment> getDefault();

    boolean contains(String key);

    Set<String> keys();
}
