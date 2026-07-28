package com.xiongx9527.fw.plus.ai.langchain4j.store.embeding;

import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingDocument;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingMatch;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingQuery;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingSearchResult;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreException;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreType;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingVector;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class LangChain4jAiEmbeddingStore implements AiEmbeddingStore {

    private final AiEmbeddingStoreDefinition definition;
    private final EmbeddingStore<TextSegment> nativeStore;

    LangChain4jAiEmbeddingStore(AiEmbeddingStoreDefinition definition, EmbeddingStore<TextSegment> nativeStore) {
        this.definition = definition;
        this.nativeStore = nativeStore;
    }

    @Override
    public String getKey() {
        return definition.getKey();
    }

    @Override
    public String getFramework() {
        return definition.getFramework();
    }

    @Override
    public String getProvider() {
        return definition.getProvider();
    }

    @Override
    public AiEmbeddingStoreType getType() {
        return definition.getType();
    }

    @Override
    public String add(AiEmbeddingDocument document) {
        requireDocument(document);
        Embedding embedding = embedding(document.getVector());
        if (!isBlank(document.getId())) {
            nativeStore.add(document.getId(), embedding);
            return document.getId();
        }
        return nativeStore.add(embedding, segment(document));
    }

    @Override
    public List<String> addAll(List<AiEmbeddingDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> ids = new ArrayList<String>(documents.size());
        for (AiEmbeddingDocument document : documents) {
            ids.add(add(document));
        }
        return ids;
    }

    @Override
    public void remove(String id) {
        nativeStore.remove(id);
    }

    @Override
    public void removeAll(List<String> ids) {
        nativeStore.removeAll(ids == null ? Collections.<String>emptyList() : ids);
    }

    @Override
    public AiEmbeddingSearchResult search(AiEmbeddingQuery query) {
        if (query == null || query.getVector() == null) {
            throw new AiEmbeddingStoreException("Embedding query and vector must not be null: key=" + getKey());
        }
        EmbeddingSearchRequest request = new EmbeddingSearchRequest(embedding(query.getVector()),
                query.getMaxResults(), query.getMinScore(), null);
        dev.langchain4j.store.embedding.EmbeddingSearchResult<TextSegment> result = nativeStore.search(request);
        List<AiEmbeddingMatch> matches = new ArrayList<AiEmbeddingMatch>();
        if (result != null && result.matches() != null) {
            for (dev.langchain4j.store.embedding.EmbeddingMatch<TextSegment> match : result.matches()) {
                matches.add(toNeutral(match));
            }
        }
        return new AiEmbeddingSearchResult(matches);
    }

    EmbeddingStore<TextSegment> nativeStore() {
        return nativeStore;
    }

    private AiEmbeddingMatch toNeutral(dev.langchain4j.store.embedding.EmbeddingMatch<TextSegment> match) {
        TextSegment embedded = match.embedded();
        AiEmbeddingVector vector = match.embedding() == null ? null
                : new AiEmbeddingVector(match.embedding().vector());
        AiEmbeddingDocument document = new AiEmbeddingDocument(match.embeddingId(),
                embedded == null ? null : embedded.text(),
                embedded == null || embedded.metadata() == null
                        ? Collections.<String, Object>emptyMap() : embedded.metadata().toMap(),
                vector);
        return new AiEmbeddingMatch(match.score(), match.embeddingId(), document);
    }

    private TextSegment segment(AiEmbeddingDocument document) {
        return TextSegment.from(document.getText() == null ? "" : document.getText(),
                new Metadata(document.getMetadata()));
    }

    private Embedding embedding(AiEmbeddingVector vector) {
        if (vector == null) {
            throw new AiEmbeddingStoreException("Embedding vector must not be null: key=" + getKey());
        }
        return Embedding.from(vector.toArray());
    }

    private void requireDocument(AiEmbeddingDocument document) {
        if (document == null) {
            throw new AiEmbeddingStoreException("Embedding document must not be null: key=" + getKey());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
