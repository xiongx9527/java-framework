package com.xiongx9527.fw.plus.ai.nl2sql;

import java.util.List;
import java.util.stream.Collectors;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;

/**
 * 把用户问题向量化，从向量库召回与问题最相关的表结构 DDL。
 */
public class SchemaRetriever {

	private final EmbeddingStore<TextSegment> embeddingStore;
	private final EmbeddingModel embeddingModel;
	private final int topK;
	private final double minScore;

	public SchemaRetriever(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel,
			int topK, double minScore) {
		this.embeddingStore = embeddingStore;
		this.embeddingModel = embeddingModel;
		this.topK = topK;
		this.minScore = minScore;
	}

	public List<TableDdl> retrieve(String question) {
		Embedding query = EmbeddingUtils.embed(embeddingModel, question);
		EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
				.queryEmbedding(query)
				.maxResults(topK)
				.minScore(minScore)
				.build();
		EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
		return result.matches().stream()
				.map(SchemaRetriever::toTableDdl)
				.collect(Collectors.toList());
	}

	private static TableDdl toTableDdl(EmbeddingMatch<TextSegment> match) {
		TextSegment segment = match.embedded();
		return new TableDdl(segment.metadata().getString("table"), segment.text());
	}
}