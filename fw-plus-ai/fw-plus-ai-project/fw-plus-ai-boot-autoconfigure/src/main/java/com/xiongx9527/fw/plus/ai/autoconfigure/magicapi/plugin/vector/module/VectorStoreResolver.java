package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.module;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;

public interface VectorStoreResolver {

    EmbeddingStore<TextSegment> get(String key);

    EmbeddingStore<TextSegment> getDefault();
}
