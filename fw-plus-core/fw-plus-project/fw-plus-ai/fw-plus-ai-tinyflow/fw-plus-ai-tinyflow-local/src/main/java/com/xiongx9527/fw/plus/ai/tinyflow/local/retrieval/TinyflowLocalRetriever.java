package com.xiongx9527.fw.plus.ai.tinyflow.local.retrieval;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowChunk;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDocument;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeDefinition;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeMatch;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRequest;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeResult;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowReranker;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowEmbeddingModel;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowModelProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorMatch;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStore;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStoreProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TinyflowLocalRetriever {
	private static final int DEFAULT_CANDIDATE_MULTIPLIER = 5;
	private static final int HARD_MAX_CANDIDATE_COUNT = 200;

	private final TinyflowKnowledgeRepository repository;
	private final TinyflowModelProvider modelProvider;
	private final TinyflowVectorStoreProvider vectorStoreProvider;
	private final TinyflowReranker reranker;
	private final TinyflowKeywordRetriever keywordRetriever;
	private final TinyflowHybridFusion fusion = new TinyflowHybridFusion();
	private final TinyflowMetadataPredicate metadataPredicate = new TinyflowMetadataPredicate();

	public TinyflowLocalRetriever(TinyflowKnowledgeRepository repository, TinyflowModelProvider modelProvider,
			TinyflowVectorStoreProvider vectorStoreProvider, TinyflowReranker reranker) {
		if (repository == null || modelProvider == null || vectorStoreProvider == null) {
			throw new IllegalArgumentException("Retriever dependencies must not be null");
		}
		this.repository = repository;
		this.modelProvider = modelProvider;
		this.vectorStoreProvider = vectorStoreProvider;
		this.reranker = reranker;
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
		String mode = option(request, "retrievalMode", "VECTOR").toUpperCase(Locale.ROOT);
		if (!"VECTOR".equals(mode) && !"KEYWORD".equals(mode) && !"HYBRID".equals(mode)) {
			throw new IllegalArgumentException("Unsupported retrieval mode: " + mode);
		}
		int candidateCount = candidateCount(request);
		List<TinyflowKnowledgeMatch> vectors = Collections.emptyList();
		List<TinyflowKnowledgeMatch> keywords = Collections.emptyList();
		if (!"KEYWORD".equals(mode)) {
			vectors = filter(request, vectorMatches(request, knowledge, candidateCount));
		}
		if (!"VECTOR".equals(mode)) {
			keywords = filter(request, keywordRetriever.retrieve(request, candidateCount));
		}
		List<TinyflowKnowledgeMatch> matches;
		if ("VECTOR".equals(mode)) {
			matches = fusion.normalize(vectors);
		} else if ("KEYWORD".equals(mode)) {
			matches = fusion.normalize(keywords);
		} else {
			matches = fusion.fuse(vectors, keywords, option(request, "fusionMode", "RRF"),
					doubleOption(request, "vectorWeight", 0.5D),
					doubleOption(request, "keywordWeight", 0.5D));
		}
		if (reranker != null && !matches.isEmpty()) {
			matches = filter(request, bounded(reranker.rerank(request,
					Collections.unmodifiableList(new ArrayList<TinyflowKnowledgeMatch>(matches))), candidateCount));
		}
		matches = expandParents(request, matches);
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
			TinyflowKnowledgeDefinition knowledge, int candidateCount) {
		String embeddingKey = option(request, knowledge, "embeddingModelKey");
		String storeKey = option(request, knowledge, "vectorStoreKey");
		TinyflowEmbeddingModel model = modelProvider.getEmbeddingModel(embeddingKey);
		TinyflowVectorStore store = vectorStoreProvider.get(storeKey);
		if (model == null || store == null) {
			throw new IllegalStateException("Embedding model or vector store is unavailable");
		}
		float[] vector = model.embed(request.getQuery());
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

	private List<TinyflowKnowledgeMatch> bounded(List<TinyflowKnowledgeMatch> matches, int limit) {
		if (matches == null || matches.isEmpty()) {
			return Collections.emptyList();
		}
		return matches.size() <= limit ? matches : matches.subList(0, limit);
	}

	private List<TinyflowKnowledgeMatch> filter(TinyflowKnowledgeRequest request,
			List<TinyflowKnowledgeMatch> candidates) {
		Map<String, TinyflowKnowledgeMatch> filtered = new LinkedHashMap<String, TinyflowKnowledgeMatch>();
		if (candidates == null) {
			return new ArrayList<TinyflowKnowledgeMatch>();
		}
		String logic = option(request, "metadataLogic", "and");
		for (TinyflowKnowledgeMatch match : candidates) {
			if (!active(request, match.getChunk()) || !metadataPredicate.test(
					match.getMetadata(), request.getMetadataConditions(), logic)) {
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
			List<TinyflowKnowledgeMatch> matches) {
		Map<String, TinyflowKnowledgeMatch> expanded = new LinkedHashMap<String, TinyflowKnowledgeMatch>();
		for (TinyflowKnowledgeMatch match : matches) {
			TinyflowChunk chunk = match.getChunk();
			if (chunk.getParentChunkId() != null && !chunk.getParentChunkId().trim().isEmpty()) {
				TinyflowChunk parent = repository.findChunk(
						request.getTenantId(), request.getKnowledgeId(), chunk.getParentChunkId());
				if (!active(request, parent) || !metadataPredicate.test(parent.getMetadata(),
						request.getMetadataConditions(), option(request, "metadataLogic", "and"))) {
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

	private int candidateCount(TinyflowKnowledgeRequest request) {
		int multiplier = intOption(request, "candidateMultiplier", DEFAULT_CANDIDATE_MULTIPLIER);
		int configuredMaximum = intOption(request, "maxCandidateCount", HARD_MAX_CANDIDATE_COUNT);
		if (multiplier <= 0) {
			multiplier = DEFAULT_CANDIDATE_MULTIPLIER;
		}
		if (configuredMaximum <= 0) {
			configuredMaximum = HARD_MAX_CANDIDATE_COUNT;
		}
		long multiplied = (long) request.getTopK() * (long) multiplier;
		return (int) Math.min(Math.min(multiplied, configuredMaximum), HARD_MAX_CANDIDATE_COUNT);
	}

	private String option(TinyflowKnowledgeRequest request, TinyflowKnowledgeDefinition knowledge, String key) {
		Object requestValue = request.getMetadata().get(key);
		Object value = requestValue == null ? knowledge.getMetadata().get(key) : requestValue;
		if (value == null || String.valueOf(value).trim().isEmpty()) {
			throw new IllegalArgumentException(key + " is required");
		}
		return String.valueOf(value).trim();
	}

	private String option(TinyflowKnowledgeRequest request, String key, String defaultValue) {
		Object value = request.getMetadata().get(key);
		return value == null || String.valueOf(value).trim().isEmpty()
				? defaultValue : String.valueOf(value).trim();
	}

	private int intOption(TinyflowKnowledgeRequest request, String key, int defaultValue) {
		Object value = request.getMetadata().get(key);
		if (value == null) {
			return defaultValue;
		}
		try {
			return value instanceof Number ? ((Number) value).intValue()
					: Integer.parseInt(String.valueOf(value));
		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException(key + " must be an integer", exception);
		}
	}

	private double doubleOption(TinyflowKnowledgeRequest request, String key, double defaultValue) {
		Object value = request.getMetadata().get(key);
		if (value == null) {
			return defaultValue;
		}
		try {
			return value instanceof Number ? ((Number) value).doubleValue()
					: Double.parseDouble(String.valueOf(value));
		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException(key + " must be a number", exception);
		}
	}
}
