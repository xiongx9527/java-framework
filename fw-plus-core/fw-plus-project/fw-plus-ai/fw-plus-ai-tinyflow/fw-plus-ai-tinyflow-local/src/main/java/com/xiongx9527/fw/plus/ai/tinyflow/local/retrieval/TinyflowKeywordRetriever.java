package com.xiongx9527.fw.plus.ai.tinyflow.local.retrieval;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowChunk;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeMatch;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TinyflowKeywordRetriever {

	private final TinyflowKnowledgeRepository repository;

	public TinyflowKeywordRetriever(TinyflowKnowledgeRepository repository) {
		if (repository == null) {
			throw new IllegalArgumentException("repository must not be null");
		}
		this.repository = repository;
	}

	public List<TinyflowKnowledgeMatch> retrieve(
			TinyflowKnowledgeRequest request, String query, int candidateCount) {
		if (query == null || query.trim().isEmpty()) {
			return Collections.emptyList();
		}
		Map<String, TinyflowKnowledgeMatch> matches = new LinkedHashMap<String, TinyflowKnowledgeMatch>();
		List<TinyflowKnowledgeMatch> candidates = repository.searchChunksByKeyword(
				request.getTenantId(), request.getKnowledgeId(), query, candidateCount);
		int size = candidates == null ? 0 : Math.min(candidates.size(), candidateCount);
		for (int index = 0; index < size; index++) {
			TinyflowKnowledgeMatch candidate = candidates.get(index);
			if (candidate == null) {
				continue;
			}
			double score = candidate.getScore();
			if (Double.isNaN(score) || Double.isInfinite(score) || score < 0D || score > 1D) {
				throw new IllegalStateException("Keyword repository returned an invalid score");
			}
			TinyflowKnowledgeMatch existing = matches.get(candidate.getChunkId());
			if (existing == null || score > existing.getScore()) {
				matches.put(candidate.getChunkId(), candidate);
			}
		}
		List<TinyflowKnowledgeMatch> result = new ArrayList<TinyflowKnowledgeMatch>(matches.values());
		Collections.sort(result, SCORE_DESCENDING);
		return result;
	}

	static TinyflowKnowledgeMatch match(TinyflowChunk chunk, double score) {
		return TinyflowKnowledgeMatch.builder().tenantId(chunk.getTenantId()).knowledgeId(chunk.getKnowledgeId())
				.sourceId(chunk.getSourceId()).sourceType(chunk.getSourceType()).documentId(chunk.getDocumentId())
				.chunkId(chunk.getChunkId()).content(chunk.getContent()).sourceUri(chunk.getSourceUri())
				.sourceVersion(chunk.getSourceVersion()).location(chunk.getLocation()).position(chunk.getPosition())
				.parentChunkId(chunk.getParentChunkId()).tokenCount(chunk.getTokenCount()).metadata(chunk.getMetadata())
				.enabled(chunk.isEnabled()).vectorIds(chunk.getVectorIds()).score(score).build();
	}

	static final Comparator<TinyflowKnowledgeMatch> SCORE_DESCENDING = new Comparator<TinyflowKnowledgeMatch>() {
		@Override
		public int compare(TinyflowKnowledgeMatch left, TinyflowKnowledgeMatch right) {
			int score = Double.compare(right.getScore(), left.getScore());
			return score != 0 ? score : left.getChunkId().compareTo(right.getChunkId());
		}
	};
}
