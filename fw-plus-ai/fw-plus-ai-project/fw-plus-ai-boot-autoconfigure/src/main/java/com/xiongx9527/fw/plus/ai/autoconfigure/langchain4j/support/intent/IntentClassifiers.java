package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent;

import dev.langchain4j.model.embedding.EmbeddingModel;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 意图分类器门面：把各层级与级联的构造收敛到一组静态工厂，使用方无需关心实现类。
 *
 * <p>典型用法（三级级联 + 消费结果）：</p>
 *
 * <pre>{@code
 * IntentClassifier<MyIntent> classifier = IntentClassifiers.cascading(
 *         IntentClassifiers.rule(IntentRule.of(MyIntent.HUMAN, "转人工|人工客服")),
 *         IntentClassifiers.embedding(embeddingModel, examples),
 *         IntentClassifiers.llm(intentLlm, MyIntent.class));
 *
 * IntentResult<MyIntent> result = classifier.classify(IntentContext.of("我的快递到哪了"));
 * if (result.matched()) {
 *     MyIntent intent = result.intent();
 *     double confidence = result.confidence();
 *     IntentSource source = result.source();
 * }
 * }</pre>
 *
 * <p>只建单个层级同样走本门面：{@code rule(List)}、{@code embedding(model, examples)}、
 * {@code llm(llm, intentType)}，或自定义阈值/置信度的重载。默认值见下方常量。</p>
 */
public final class IntentClassifiers {

	/** 向量判定默认阈值：最优意图余弦相似度下限。 */
	public static final double DEFAULT_EMBEDDING_THRESHOLD = 0.72d;

	/** 向量判定默认间距：最优与次优相似度之差下限。 */
	public static final double DEFAULT_EMBEDDING_MARGIN = 0.05d;

	/** 大模型判定默认置信度阈值。 */
	public static final double DEFAULT_LLM_CONFIDENCE_THRESHOLD = 0.6d;

	/** 级联默认最低置信度（层级自身已把好关，此处不额外抬门槛）。 */
	public static final double DEFAULT_CASCADE_MIN_CONFIDENCE = 0.0d;

	private IntentClassifiers() {
	}

	@SafeVarargs
	public static <I extends Enum<I>> IntentClassifier<I> rule(IntentRule<I>... rules) {
		return rule(Arrays.asList(rules));
	}

	public static <I extends Enum<I>> IntentClassifier<I> rule(List<IntentRule<I>> rules) {
		return new RuleIntentClassifier<I>(rules);
	}

	public static <I extends Enum<I>> IntentClassifier<I> embedding(EmbeddingModel embeddingModel,
			Map<I, List<String>> examples) {
		return embedding(embeddingModel, examples, DEFAULT_EMBEDDING_THRESHOLD, DEFAULT_EMBEDDING_MARGIN);
	}

	public static <I extends Enum<I>> IntentClassifier<I> embedding(EmbeddingModel embeddingModel,
			Map<I, List<String>> examples, double threshold, double margin) {
		return new EmbeddingIntentClassifier<I>(embeddingModel, examples, threshold, margin);
	}

	public static <I extends Enum<I>> IntentClassifier<I> llm(IntentLlm llm, Class<I> intentType) {
		return llm(llm, intentType, null, DEFAULT_LLM_CONFIDENCE_THRESHOLD);
	}

	public static <I extends Enum<I>> IntentClassifier<I> llm(IntentLlm llm, Class<I> intentType,
			Map<I, String> descriptions, double confidenceThreshold) {
		return new LlmIntentClassifier<I>(llm, intentType, descriptions, confidenceThreshold);
	}

	@SafeVarargs
	public static <I extends Enum<I>> IntentClassifier<I> cascading(IntentClassifier<I>... delegates) {
		return cascading(Arrays.asList(delegates), DEFAULT_CASCADE_MIN_CONFIDENCE);
	}

	@SafeVarargs
	public static <I extends Enum<I>> IntentClassifier<I> cascading(double minConfidence,
			IntentClassifier<I>... delegates) {
		return cascading(Arrays.asList(delegates), minConfidence);
	}

	public static <I extends Enum<I>> IntentClassifier<I> cascading(List<IntentClassifier<I>> delegates,
			double minConfidence) {
		return new CascadingIntentClassifier<I>(delegates, minConfidence);
	}
}
