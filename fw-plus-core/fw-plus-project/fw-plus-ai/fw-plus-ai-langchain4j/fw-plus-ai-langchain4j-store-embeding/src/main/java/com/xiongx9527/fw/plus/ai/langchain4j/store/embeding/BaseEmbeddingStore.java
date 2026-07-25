package com.xiongx9527.fw.plus.ai.langchain4j.store.embeding;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.Data;

@Data
public class BaseEmbeddingStore {

    private EmbeddingStore<TextSegment> embeddingStore;

    private String embeddingStoreClassName;

}
