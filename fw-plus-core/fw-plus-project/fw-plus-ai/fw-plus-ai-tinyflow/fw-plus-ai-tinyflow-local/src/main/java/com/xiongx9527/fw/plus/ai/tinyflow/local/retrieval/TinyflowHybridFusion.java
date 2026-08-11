package com.xiongx9527.fw.plus.ai.tinyflow.local.retrieval;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeMatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TinyflowHybridFusion {

	private static final double RRF_K = 60D;

	public List<TinyflowKnowledgeMatch> normalize(List<TinyflowKnowledgeMatch> matches) {
		List<TinyflowKnowledgeMatch> unique = deduplicate(matches);
		if (unique.isEmpty()) {
			return unique;
		}
		double minimum = Double.MAX_VALUE;
		double maximum = -Double.MAX_VALUE;
		for (TinyflowKnowledgeMatch match : unique) {
			minimum = Math.min(minimum, match.getScore());
			maximum = Math.max(maximum, match.getScore());
		}
		List<TinyflowKnowledgeMatch> normalized = new ArrayList<TinyflowKnowledgeMatch>();
		for (TinyflowKnowledgeMatch match : unique) {
			double score = maximum == minimum ? 1D : (match.getScore() - minimum) / (maximum - minimum);
			normalized.add(TinyflowKeywordRetriever.match(match.getChunk(), score));
		}
		Collections.sort(normalized, TinyflowKeywordRetriever.SCORE_DESCENDING);
		return normalized;
	}

	public List<TinyflowKnowledgeMatch> fuse(List<TinyflowKnowledgeMatch> vectorMatches,
			List<TinyflowKnowledgeMatch> keywordMatches, String mode, double vectorWeight, double keywordWeight) {
		List<TinyflowKnowledgeMatch> vectors = normalize(vectorMatches);
		List<TinyflowKnowledgeMatch> keywords = normalize(keywordMatches);
		String normalizedMode = mode == null ? "RRF" : mode.trim().toUpperCase(Locale.ROOT);
		if (!"RRF".equals(normalizedMode) && !"WEIGHTED".equals(normalizedMode)) {
			throw new IllegalArgumentException("Unsupported fusion mode: " + mode);
		}
		Map<String, TinyflowKnowledgeMatch> sources = new LinkedHashMap<String, TinyflowKnowledgeMatch>();
		Map<String, Double> scores = new LinkedHashMap<String, Double>();
		accumulate(vectors, vectorWeight, normalizedMode, sources, scores);
		accumulate(keywords, keywordWeight, normalizedMode, sources, scores);
		List<TinyflowKnowledgeMatch> result = new ArrayList<TinyflowKnowledgeMatch>();
		for (Map.Entry<String, TinyflowKnowledgeMatch> entry : sources.entrySet()) {
			result.add(TinyflowKeywordRetriever.match(entry.getValue().getChunk(),
					scores.get(entry.getKey()).doubleValue()));
		}
		Collections.sort(result, TinyflowKeywordRetriever.SCORE_DESCENDING);
		return result;
	}

	private void accumulate(List<TinyflowKnowledgeMatch> matches, double weight, String mode,
			Map<String, TinyflowKnowledgeMatch> sources, Map<String, Double> scores) {
		for (int index = 0; index < matches.size(); index++) {
			TinyflowKnowledgeMatch match = matches.get(index);
			String chunkId = match.getChunkId();
			double contribution = "RRF".equals(mode)
					? weight / (RRF_K + index + 1D) : weight * match.getScore();
			sources.put(chunkId, match);
			Double current = scores.get(chunkId);
			scores.put(chunkId, Double.valueOf((current == null ? 0D : current.doubleValue()) + contribution));
		}
	}

	private List<TinyflowKnowledgeMatch> deduplicate(List<TinyflowKnowledgeMatch> matches) {
		Map<String, TinyflowKnowledgeMatch> unique = new LinkedHashMap<String, TinyflowKnowledgeMatch>();
		if (matches != null) {
			for (TinyflowKnowledgeMatch match : matches) {
				TinyflowKnowledgeMatch current = unique.get(match.getChunkId());
				if (current == null || match.getScore() > current.getScore()) {
					unique.put(match.getChunkId(), match);
				}
			}
		}
		return new ArrayList<TinyflowKnowledgeMatch>(unique.values());
	}
}
