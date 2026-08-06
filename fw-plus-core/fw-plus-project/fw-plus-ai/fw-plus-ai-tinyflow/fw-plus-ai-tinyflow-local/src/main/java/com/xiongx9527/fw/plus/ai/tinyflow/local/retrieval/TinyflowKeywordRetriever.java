package com.xiongx9527.fw.plus.ai.tinyflow.local.retrieval;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowChunk;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeMatch;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class TinyflowKeywordRetriever {

	private final TinyflowKnowledgeRepository repository;

	public TinyflowKeywordRetriever(TinyflowKnowledgeRepository repository) {
		if (repository == null) {
			throw new IllegalArgumentException("repository must not be null");
		}
		this.repository = repository;
	}

	public List<TinyflowKnowledgeMatch> retrieve(TinyflowKnowledgeRequest request, int candidateCount) {
		Set<String> terms = terms(request.getQuery());
		if (terms.isEmpty()) {
			return Collections.emptyList();
		}
		Map<String, TinyflowKnowledgeMatch> matches = new LinkedHashMap<String, TinyflowKnowledgeMatch>();
		List<TinyflowChunk> chunks = repository.findChunks(
				request.getTenantId(), request.getKnowledgeId(), candidateCount);
		int size = chunks == null ? 0 : Math.min(chunks.size(), candidateCount);
		for (int index = 0; index < size; index++) {
			TinyflowChunk chunk = chunks.get(index);
			if (chunk == null) {
				continue;
			}
			double score = score(chunk.getContent(), terms);
			if (score <= 0D) {
				continue;
			}
			TinyflowKnowledgeMatch candidate = match(chunk, score);
			TinyflowKnowledgeMatch existing = matches.get(chunk.getChunkId());
			if (existing == null || score > existing.getScore()) {
				matches.put(chunk.getChunkId(), candidate);
			}
		}
		List<TinyflowKnowledgeMatch> result = new ArrayList<TinyflowKnowledgeMatch>(matches.values());
		Collections.sort(result, SCORE_DESCENDING);
		return result;
	}

	private Set<String> terms(String query) {
		Set<String> result = new LinkedHashSet<String>();
		if (query == null) {
			return result;
		}
		for (String value : query.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
			if (!value.isEmpty()) {
				result.add(value);
			}
		}
		return result;
	}

	private double score(String content, Set<String> terms) {
		String text = content == null ? "" : content.toLowerCase(Locale.ROOT);
		int matched = 0;
		for (String term : terms) {
			if (text.contains(term)) {
				matched++;
			}
		}
		return (double) matched / (double) terms.size();
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
