package com.xiongx9527.fw.plus.ai.langchain4j.adapter.store;

import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreDefinition;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;

public interface LangChain4jEmbeddingStoreProviderAdapter {

    boolean supports(AiEmbeddingStoreDefinition definition);

    EmbeddingStore<TextSegment> create(AiEmbeddingStoreDefinition definition);
}
