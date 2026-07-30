package com.xiongx9527.fw.plus.ai.langchain4j.adapter;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;

public interface EmbeddingStoreProviderAdapter {

    boolean supports(String provider);

    EmbeddingStore<TextSegment> create(EmbeddingStoreDefinition definition);
}
