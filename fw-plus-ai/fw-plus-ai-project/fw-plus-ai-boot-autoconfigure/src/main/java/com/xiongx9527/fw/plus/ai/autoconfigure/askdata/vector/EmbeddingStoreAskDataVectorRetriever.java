package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 将 LangChain4j EmbeddingStore 适配为 AskData 检索边界。 */
public class EmbeddingStoreAskDataVectorRetriever implements AskDataVectorRetriever {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    public EmbeddingStoreAskDataVectorRetriever(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
    }

    @Override
    public List<AskDataRetrievedDocument> retrieve(AskDataVectorQuery query) {
        if (query == null || embeddingStore == null || embeddingModel == null || isBlank(query.getQuestion())) {
            return Collections.emptyList();
        }
        if (!query.getFilters().isEmpty()) {
            throw new IllegalStateException("LangChain4j EmbeddingStore 兼容适配器不支持 AskData 授权过滤");
        }
        Embedding embedding = embeddingModel.embed(query.getQuestion()).content();
        if (embedding == null) {
            return Collections.emptyList();
        }
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(EmbeddingSearchRequest.builder()
                .queryEmbedding(embedding)
                .maxResults(query.getTopK() <= 0 ? 5 : query.getTopK())
                .build()).matches();
        List<AskDataRetrievedDocument> documents = new ArrayList<AskDataRetrievedDocument>();
        for (EmbeddingMatch<TextSegment> match : matches) {
            if (match != null && match.embedded() != null) {
                documents.add(new AskDataRetrievedDocument(match.embedded().text()));
            }
        }
        return documents;
    }

    private boolean isBlank(String value) { return value == null || value.trim().length() == 0; }
}
