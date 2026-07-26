package dev.langchain4j.store.embedding.astradb;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.NonNull;

import java.util.List;

/**
 * Java 8 placeholder for the upstream AstraDB embedding store.
 */
public class AstraDbEmbeddingStore implements EmbeddingStore<TextSegment> {

    public AstraDbEmbeddingStore(@NonNull Object client) {
        throw unsupportedAstraDbClient();
    }

    public AstraDbEmbeddingStore(@NonNull Object client, int itemsPerChunk, int concurrentThreads) {
        throw unsupportedAstraDbClient();
    }

    public void clear() {
        throw unsupportedAstraDbClient();
    }

    @Override
    public String add(Embedding embedding) {
        throw unsupportedAstraDbClient();
    }

    @Override
    public String add(Embedding embedding, TextSegment textSegment) {
        throw unsupportedAstraDbClient();
    }

    @Override
    public void add(String id, Embedding embedding) {
        throw unsupportedAstraDbClient();
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        throw unsupportedAstraDbClient();
    }

    @Override
    public List<String> addAll(List<Embedding> embeddingList, List<TextSegment> textSegmentList) {
        throw unsupportedAstraDbClient();
    }

    @Override
    public List<EmbeddingMatch<TextSegment>> findRelevant(Embedding referenceEmbedding, int maxResults, double minScore) {
        throw unsupportedAstraDbClient();
    }

    public List<EmbeddingMatch<TextSegment>> findRelevant(Embedding referenceEmbedding, Object metaDataFilter, int maxResults, double minScore) {
        throw unsupportedAstraDbClient();
    }

    public List<EmbeddingMatch<TextSegment>> findRelevant(Embedding referenceEmbedding, Metadata metadata, int maxResults, double minScore) {
        throw unsupportedAstraDbClient();
    }

    private static UnsupportedOperationException unsupportedAstraDbClient() {
        return new UnsupportedOperationException("astra-db-client is compiled for Java 11 and is not provided by the Java 8 compatibility module");
    }
}
