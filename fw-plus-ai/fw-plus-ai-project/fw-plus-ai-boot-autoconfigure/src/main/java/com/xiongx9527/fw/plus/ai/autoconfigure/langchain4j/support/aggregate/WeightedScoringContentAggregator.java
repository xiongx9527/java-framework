package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.aggregate;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.aggregator.ContentAggregator;
import dev.langchain4j.rag.query.Query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 加权重排聚合器：向量相似度分与 rerank 语义分按权重融合排序。
 *
 * <p>总分 = 相似度分 × {@code similarityWeight} + 语义分 × {@code semanticWeight}。</p>
 * <ul>
 *     <li>相似度分：EmbeddingStoreContentRetriever 写入 Content metadata 的 {@code score} 键；
 *     缺失（如 web-search 来源）时该条目的总分退化为纯语义分。</li>
 *     <li>语义分：{@link ScoringModel#scoreAll(List, String)} 对问题与候选片段交叉打分。</li>
 * </ul>
 *
 * <p>多路检索（多 query / 多检索器）先按文本去重，再统一打分重排；
 * 可选 {@code minScore} 过滤低分条目、{@code maxResults} 截断保留条数。</p>
 */
public class WeightedScoringContentAggregator implements ContentAggregator {

	/** 相似度分在 Content metadata 中的键（EmbeddingStoreContentRetriever 写入） */
	public static final String KEY_SIMILARITY_SCORE = "score";

	private final ScoringModel scoringModel;

	private final double similarityWeight;

	private final double semanticWeight;

	private final Double minScore;

	private final Integer maxResults;

	public WeightedScoringContentAggregator(ScoringModel scoringModel, double similarityWeight,
			double semanticWeight, Double minScore, Integer maxResults) {
		this.scoringModel = scoringModel;
		this.similarityWeight = similarityWeight;
		this.semanticWeight = semanticWeight;
		this.minScore = minScore;
		this.maxResults = maxResults;
	}

	@Override
	public List<Content> aggregate(Map<Query, Collection<List<Content>>> resultMap) {
		// 按 query 分别重排（语义分依赖问题文本），多 query 结果按最大加权分融合去重
		Map<String, Weighted> best = new LinkedHashMap<>();
		for (Map.Entry<Query, Collection<List<Content>>> entry : resultMap.entrySet()) {
			Query query = entry.getKey();
			List<Content> merged = new ArrayList<>();
			entry.getValue().forEach(merged::addAll);
			if (merged.isEmpty()) {
				continue;
			}
			// 同 query 内按文本去重
			List<Content> candidates = new ArrayList<>(new LinkedHashMap<String, Content>() {
				{
					for (Content content : merged) {
						putIfAbsent(content.textSegment().text(), content);
					}
				}
			}.values());
			List<TextSegment> segments = new ArrayList<>();
			for (Content candidate : candidates) {
				segments.add(candidate.textSegment());
			}
			List<Double> semanticScores = scoringModel.scoreAll(segments, query.text()).content();
			for (int i = 0; i < candidates.size(); i++) {
				Weighted weighted = weighted(candidates.get(i), semanticScores.get(i));
				best.merge(weighted.key(), weighted, (left, right) -> left.score >= right.score ? left : right);
			}
		}

		List<Weighted> ranked = new ArrayList<>(best.values());
		ranked.sort((left, right) -> Double.compare(right.score, left.score));

		List<Content> result = new ArrayList<>();
		for (Weighted weighted : ranked) {
			if (minScore != null && weighted.score < minScore) {
				continue;
			}
			result.add(weighted.content);
			if (maxResults != null && result.size() >= maxResults) {
				break;
			}
		}
		return result;
	}

	private Weighted weighted(Content content, double semanticScore) {
		Double similarity = null;
		try {
			String raw = content.textSegment().metadata().getString(KEY_SIMILARITY_SCORE);
			if (raw != null && !raw.trim().isEmpty()) {
				similarity = Double.parseDouble(raw);
			}
		} catch (Exception ignored) {
			// metadata 分数缺失或格式异常：退化为纯语义分
		}
		double score = similarity == null
				? semanticScore
				: similarity * similarityWeight + semanticScore * semanticWeight;
		return new Weighted(content, score);
	}

	/** 加权打分后的条目：content 与其总分 */
	private static final class Weighted {

		private final Content content;

		private final double score;

		private Weighted(Content content, double score) {
			this.content = content;
			this.score = score;
		}

		/** 融合去重的键：以片段文本为准 */
		private String key() {
			return content.textSegment().text();
		}
	}
}
