package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.CosineSimilarity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * L1 向量分类器：把用户输入与各意图的示例句做余弦相似度，取每个意图的最大分（max-pooling）。
 *
 * <p>双判据短路，避免近义意图被高分误判：</p>
 * <ol>
 *   <li>最优意图得分 {@code >= threshold}；</li>
 *   <li>最优与次优分差 {@code >= margin}（仅有一个意图时跳过该判据）。</li>
 * </ol>
 *
 * <p>示例句在构造时一次性向量化并缓存（仅嵌入用户输入一次/次判定）。任一判据不满足即返回
 * {@link IntentResult#none(IntentSource)}，交给级联下一级（大模型）。</p>
 *
 * @param <I> 使用方自定义的意图枚举类型
 */
public final class EmbeddingIntentClassifier<I extends Enum<I>> implements IntentClassifier<I> {

	private final EmbeddingModel embeddingModel;

	private final Map<I, List<Embedding>> exampleEmbeddings;

	private final double threshold;

	private final double margin;

	public EmbeddingIntentClassifier(EmbeddingModel embeddingModel, Map<I, List<String>> exampleTexts,
			double threshold, double margin) {
		if (embeddingModel == null) {
			throw new IllegalArgumentException("embeddingModel 不能为 null");
		}
		if (exampleTexts == null || exampleTexts.isEmpty()) {
			throw new IllegalArgumentException("必须至少为一个意图提供示例句");
		}
		this.embeddingModel = embeddingModel;
		this.threshold = threshold;
		this.margin = margin;
		this.exampleEmbeddings = embedExamples(exampleTexts);
	}

	private Map<I, List<Embedding>> embedExamples(Map<I, List<String>> exampleTexts) {
		Map<I, List<Embedding>> embedded = new LinkedHashMap<I, List<Embedding>>();
		for (Map.Entry<I, List<String>> entry : exampleTexts.entrySet()) {
			List<Embedding> vectors = new ArrayList<Embedding>();
			List<String> texts = entry.getValue();
			if (texts != null) {
				for (String text : texts) {
					if (text != null && !text.trim().isEmpty()) {
						vectors.add(embed(text));
					}
				}
			}
			embedded.put(entry.getKey(), vectors);
		}
		return embedded;
	}

	private Embedding embed(String text) {
		Response<Embedding> response = embeddingModel.embed(text);
		if (response == null || response.content() == null) {
			throw new IllegalStateException("EmbeddingModel 返回空结果，无法完成示例向量化");
		}
		return response.content();
	}

	@Override
	public IntentResult<I> classify(IntentContext context) {
		String text = context == null ? null : context.normalizedText();
		if (text == null || text.isEmpty()) {
			return IntentResult.none(IntentSource.EMBEDDING);
		}
		Embedding query = embed(text);

		I best = null;
		double bestScore = Double.NEGATIVE_INFINITY;
		double secondScore = Double.NEGATIVE_INFINITY;
		for (Map.Entry<I, List<Embedding>> entry : exampleEmbeddings.entrySet()) {
			double score = maxSimilarity(query, entry.getValue());
			if (score > bestScore) {
				secondScore = bestScore;
				bestScore = score;
				best = entry.getKey();
			} else if (score > secondScore) {
				secondScore = score;
			}
		}
		if (best == null) {
			return IntentResult.none(IntentSource.EMBEDDING);
		}

		double runnerUp = secondScore == Double.NEGATIVE_INFINITY ? 0.0d : secondScore;
		boolean singleIntent = exampleEmbeddings.size() <= 1;
		boolean confident = bestScore >= threshold && (singleIntent || (bestScore - runnerUp) >= margin);
		return confident
				? IntentResult.of(best, bestScore, Collections.<String, String>emptyMap(), IntentSource.EMBEDDING)
				: IntentResult.none(IntentSource.EMBEDDING);
	}

	private static double maxSimilarity(Embedding query, List<Embedding> candidates) {
		double max = Double.NEGATIVE_INFINITY;
		if (candidates != null) {
			for (Embedding candidate : candidates) {
				double score = CosineSimilarity.between(query, candidate);
				if (score > max) {
					max = score;
				}
			}
		}
		return max;
	}

	public double threshold() {
		return threshold;
	}

	public double margin() {
		return margin;
	}

	/** 参与判定的意图数量。 */
	public int intentCount() {
		return exampleEmbeddings.size();
	}
}
