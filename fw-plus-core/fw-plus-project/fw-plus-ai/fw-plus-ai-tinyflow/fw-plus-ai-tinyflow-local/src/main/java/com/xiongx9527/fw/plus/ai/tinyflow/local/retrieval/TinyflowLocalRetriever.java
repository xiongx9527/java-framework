package com.xiongx9527.fw.plus.ai.tinyflow.local.retrieval;

import com.xiongx9527.fw.plus.ai.tinyflow.TinyflowException;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowChunk;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDocument;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeDefinition;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeMatch;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRequest;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeResult;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowReranker;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowQueryRewriter;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowEmbeddingModel;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowModelProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorMatch;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStore;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStoreProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TinyflowLocalRetriever {
	private final TinyflowKnowledgeRepository repository;
	private final TinyflowModelProvider modelProvider;
	private final TinyflowVectorStoreProvider vectorStoreProvider;
	private final TinyflowReranker reranker;
	private final TinyflowQueryRewriter queryRewriter;
	private final TinyflowKeywordRetriever keywordRetriever;
	private final TinyflowHybridFusion fusion = new TinyflowHybridFusion();
	private final TinyflowMetadataPredicate metadataPredicate = new TinyflowMetadataPredicate();

	public TinyflowLocalRetriever(TinyflowKnowledgeRepository repository, TinyflowModelProvider modelProvider,
			TinyflowVectorStoreProvider vectorStoreProvider, TinyflowReranker reranker) {
		this(repository, modelProvider, vectorStoreProvider, reranker, null);
	}

	public TinyflowLocalRetriever(TinyflowKnowledgeRepository repository, TinyflowModelProvider modelProvider,
			TinyflowVectorStoreProvider vectorStoreProvider, TinyflowReranker reranker,
			TinyflowQueryRewriter queryRewriter) {
		if (repository == null || modelProvider == null || vectorStoreProvider == null) {
			throw new IllegalArgumentException("Retriever dependencies must not be null");
		}
		this.repository = repository;
		this.modelProvider = modelProvider;
		this.vectorStoreProvider = vectorStoreProvider;
		this.reranker = reranker;
		this.queryRewriter = queryRewriter;
		this.keywordRetriever = new TinyflowKeywordRetriever(repository);
	}

	public TinyflowKnowledgeResult retrieve(TinyflowKnowledgeRequest request) {
		if (request == null) {
			return TinyflowKnowledgeResult.notFound();
		}
		TinyflowKnowledgeDefinition knowledge = repository.findKnowledge(
				request.getTenantId(), request.getKnowledgeId());
		if (knowledge == null) {
			return TinyflowKnowledgeResult.notFound();
		}
		if (!knowledge.isEnabled()) {
			return TinyflowKnowledgeResult.empty();
		}
		String effectiveQuery = effectiveQuery(request);
		TinyflowLocalRetrievalOptions options = TinyflowLocalRetrievalOptions.from(request.getRetrievalOptions());
		TinyflowLocalRetrievalOptions.Mode mode = options.getMode();
		int candidateCount = options.candidateCount(request.getTopK());
		List<TinyflowKnowledgeMatch> vectors = Collections.emptyList();
		List<TinyflowKnowledgeMatch> keywords = Collections.emptyList();
		if (mode != TinyflowLocalRetrievalOptions.Mode.KEYWORD) {
			vectors = filter(request, vectorMatches(request, knowledge, effectiveQuery, candidateCount),
					options.getMetadataLogic());
		}
		if (mode != TinyflowLocalRetrievalOptions.Mode.VECTOR) {
			keywords = filter(request, keywordRetriever.retrieve(request, effectiveQuery, candidateCount),
					options.getMetadataLogic());
		}
		List<TinyflowKnowledgeMatch> matches;
		if (mode == TinyflowLocalRetrievalOptions.Mode.VECTOR) {
			matches = fusion.normalize(vectors);
		} else if (mode == TinyflowLocalRetrievalOptions.Mode.KEYWORD) {
			matches = fusion.normalize(keywords);
		} else {
			matches = fusion.fuse(vectors, keywords, options.getFusionMode().name(),
					options.getVectorWeight(), options.getKeywordWeight());
		}
		if (reranker != null && !matches.isEmpty()) {
			matches = filter(request, bounded(reranker.rerank(request,
					Collections.unmodifiableList(new ArrayList<TinyflowKnowledgeMatch>(matches))), candidateCount),
					options.getMetadataLogic());
		}
		matches = expandParents(request, matches, options.getMetadataLogic());
		List<TinyflowKnowledgeMatch> result = new ArrayList<TinyflowKnowledgeMatch>();
		for (TinyflowKnowledgeMatch match : matches) {
			if (request.getThreshold() == null || match.getScore() >= request.getThreshold().doubleValue()) {
				result.add(match);
			}
		}
		Collections.sort(result, TinyflowKeywordRetriever.SCORE_DESCENDING);
		if (result.size() > request.getTopK()) {
			result = new ArrayList<TinyflowKnowledgeMatch>(result.subList(0, request.getTopK()));
		}
		return TinyflowKnowledgeResult.builder().matches(result).build();
	}

	private List<TinyflowKnowledgeMatch> vectorMatches(TinyflowKnowledgeRequest request,
			TinyflowKnowledgeDefinition knowledge, String query, int candidateCount) {
		String embeddingKey = knowledgeOption(knowledge, "embeddingModelKey");
		String storeKey = knowledgeOption(knowledge, "vectorStoreKey");
		TinyflowEmbeddingModel model = modelProvider.getEmbeddingModel(embeddingKey);
		TinyflowVectorStore store = vectorStoreProvider.get(storeKey);
		if (model == null || store == null) {
			throw new IllegalStateException("Embedding model or vector store is unavailable");
		}
		float[] vector = model.embed(query);
		if (vector == null || vector.length == 0) {
			throw new IllegalStateException("Embedding model returned an empty vector");
		}
		List<TinyflowVectorMatch> raw = store.search(vector, candidateCount, 0D);
		Map<String, Double> vectorScores = new LinkedHashMap<String, Double>();
		List<String> vectorIds = new ArrayList<String>();
		if (raw != null) {
			int size = Math.min(raw.size(), candidateCount);
			for (int index = 0; index < size; index++) {
				TinyflowVectorMatch match = raw.get(index);
				if (match == null || match.getId() == null) {
					continue;
				}
				Double current = vectorScores.get(match.getId());
				if (current == null) {
					vectorIds.add(match.getId());
				}
				if (current == null || match.getScore() > current.doubleValue()) {
					vectorScores.put(match.getId(), Double.valueOf(match.getScore()));
				}
			}
		}
		Map<String, TinyflowKnowledgeMatch> chunks = new LinkedHashMap<String, TinyflowKnowledgeMatch>();
		List<TinyflowChunk> loaded = repository.findChunksByVectorIds(
				request.getTenantId(), request.getKnowledgeId(), vectorIds);
		int loadedSize = loaded == null ? 0 : Math.min(loaded.size(), candidateCount);
		for (int index = 0; index < loadedSize; index++) {
			TinyflowChunk chunk = loaded.get(index);
			if (chunk == null) {
				continue;
			}
			double score = -Double.MAX_VALUE;
			for (String vectorId : chunk.getVectorIds()) {
				Double value = vectorScores.get(vectorId);
				if (value != null) {
					score = Math.max(score, value.doubleValue());
				}
			}
			if (score != -Double.MAX_VALUE) {
				TinyflowKnowledgeMatch current = chunks.get(chunk.getChunkId());
				if (current == null || score > current.getScore()) {
					chunks.put(chunk.getChunkId(), TinyflowKeywordRetriever.match(chunk, score));
				}
			}
		}
		return new ArrayList<TinyflowKnowledgeMatch>(chunks.values());
	}

	private String effectiveQuery(TinyflowKnowledgeRequest request) {
		if (queryRewriter == null) {
			return request.getQuery();
		}
		final String rewritten;
		try {
			rewritten = queryRewriter.rewrite(request);
		} catch (RuntimeException exception) {
			throw new TinyflowException("Tinyflow query rewrite failed", exception);
		}
		if (rewritten == null || rewritten.trim().isEmpty()) {
			throw new TinyflowException("Tinyflow query rewriter returned a blank query");
		}
		return rewritten;
	}

	private List<TinyflowKnowledgeMatch> bounded(List<TinyflowKnowledgeMatch> matches, int limit) {
		if (matches == null || matches.isEmpty()) {
			return Collections.emptyList();
		}
		return matches.size() <= limit ? matches : matches.subList(0, limit);
	}

	private List<TinyflowKnowledgeMatch> filter(TinyflowKnowledgeRequest request,
			List<TinyflowKnowledgeMatch> candidates, String metadataLogic) {
		Map<String, TinyflowKnowledgeMatch> filtered = new LinkedHashMap<String, TinyflowKnowledgeMatch>();
		if (candidates == null) {
			return new ArrayList<TinyflowKnowledgeMatch>();
		}
		for (TinyflowKnowledgeMatch match : candidates) {
			if (!active(request, match.getChunk()) || !metadataPredicate.test(
					match.getMetadata(), request.getMetadataConditions(), metadataLogic)) {
				continue;
			}
			TinyflowKnowledgeMatch current = filtered.get(match.getChunkId());
			if (current == null || match.getScore() > current.getScore()) {
				filtered.put(match.getChunkId(), match);
			}
		}
		return new ArrayList<TinyflowKnowledgeMatch>(filtered.values());
	}

	private boolean active(TinyflowKnowledgeRequest request, TinyflowChunk chunk) {
		if (chunk == null || !chunk.isEnabled()
				|| !request.getTenantId().equals(chunk.getTenantId())
				|| !request.getKnowledgeId().equals(chunk.getKnowledgeId())) {
			return false;
		}
		TinyflowDocument document = repository.findActiveDocument(
				request.getTenantId(), request.getKnowledgeId(), chunk.getSourceId());
		return document != null && document.isEnabled()
				&& chunk.getDocumentId().equals(document.getDocumentId());
	}

	private List<TinyflowKnowledgeMatch> expandParents(TinyflowKnowledgeRequest request,
			List<TinyflowKnowledgeMatch> matches, String metadataLogic) {
		Map<String, TinyflowKnowledgeMatch> expanded = new LinkedHashMap<String, TinyflowKnowledgeMatch>();
		for (TinyflowKnowledgeMatch match : matches) {
			TinyflowChunk chunk = match.getChunk();
			if (chunk.getParentChunkId() != null && !chunk.getParentChunkId().trim().isEmpty()) {
				TinyflowChunk parent = repository.findChunk(
						request.getTenantId(), request.getKnowledgeId(), chunk.getParentChunkId());
				if (!active(request, parent) || !metadataPredicate.test(parent.getMetadata(),
						request.getMetadataConditions(), metadataLogic)) {
					continue;
				}
				match = TinyflowKeywordRetriever.match(parent, match.getScore());
			}
			TinyflowKnowledgeMatch current = expanded.get(match.getChunkId());
			if (current == null || match.getScore() > current.getScore()) {
				expanded.put(match.getChunkId(), match);
			}
		}
		return new ArrayList<TinyflowKnowledgeMatch>(expanded.values());
	}

	private String knowledgeOption(TinyflowKnowledgeDefinition knowledge, String key) {
		Object value = knowledge.getMetadata().get(key);
		if (value == null || String.valueOf(value).trim().isEmpty()) {
			throw new IllegalArgumentException(key + " is required");
		}
		return String.valueOf(value).trim();
	}
}
