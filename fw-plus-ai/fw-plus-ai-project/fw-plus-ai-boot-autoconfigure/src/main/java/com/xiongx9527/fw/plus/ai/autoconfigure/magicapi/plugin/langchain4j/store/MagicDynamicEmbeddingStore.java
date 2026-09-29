package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.store;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jEmbeddingStoreModule;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import com.xiongx9527.fw.plus.common.util.StringUtils;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import java.util.Collections;
import java.util.List;

public class MagicDynamicEmbeddingStore implements EmbeddingStore<TextSegment> {

    private final Langchain4jEmbeddingStoreModule embeddingStore;

    private final String key;

    public MagicDynamicEmbeddingStore(Langchain4jEmbeddingStoreModule embeddingStore, String key) {
        this.embeddingStore = embeddingStore;
        this.key = key;
    }

    private EmbeddingStore<TextSegment> getEmbeddingStore() {
        try {
            return embeddingStore.getDynamicAttribute(key);
        } catch (Exception e) {
            if (StringUtils.isEmpty(key)) {
                return SpringUtils.getBean(EmbeddingStore.class);
            }
            return SpringUtils.getBean(key,EmbeddingStore.class);
        }
    }

    @Override
    public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
        return getEmbeddingStore().search(request);
    }

    @Override
    public String add(Embedding embedding) {
        return getEmbeddingStore().add(embedding);
    }

    @Override
    public void add(String id, Embedding embedding) {
        getEmbeddingStore().add(id, embedding);

    }

    @Override
    public String add(Embedding embedding, TextSegment textSegment) {
        return getEmbeddingStore().add(embedding, textSegment);
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        return getEmbeddingStore().addAll(embeddings);
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> embedded) {
        return getEmbeddingStore().addAll(embeddings, embedded);
    }
}
