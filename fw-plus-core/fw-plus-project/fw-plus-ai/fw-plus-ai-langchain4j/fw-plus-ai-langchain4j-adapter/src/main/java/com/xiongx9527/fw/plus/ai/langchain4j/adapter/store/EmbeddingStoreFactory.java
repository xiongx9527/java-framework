package com.xiongx9527.fw.plus.ai.langchain4j.adapter.store;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.util.List;

public interface EmbeddingStoreFactory {

    EmbeddingStore<TextSegment> create(EmbeddingStoreDefinition definition, List<EmbeddingStoreProviderAdapter> adapters);
}
