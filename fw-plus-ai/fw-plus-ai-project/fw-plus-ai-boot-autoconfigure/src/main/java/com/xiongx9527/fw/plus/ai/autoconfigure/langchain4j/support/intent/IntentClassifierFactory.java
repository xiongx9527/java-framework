package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent;

import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 意图分类器工厂：持有可选的向量模型与 {@link IntentLlm}，按使用方的意图枚举批量构建分类器。
 *
 * <p>作为 Spring Bean 注入后，使用方无需自己碰各实现类；当容器里有 {@link EmbeddingModel} /
 * {@link IntentLlm} 时，{@link #auto} 可一键装配「规则 → 向量 → 大模型」三级级联，缺哪级自动跳过。</p>
 *
 * <p>用法示例（Spring 环境下注入本 Bean）：</p>
 * <pre>{@code
 * public class IntentService {
 *
 *     private final IntentClassifierFactory factory;
 *
 *     public IntentService(IntentClassifierFactory factory) {
 *         this.factory = factory;
 *     }
 *
 *     // 规则 → 向量 → 大模型，缺哪级自动跳过；三级都不可用则抛 IllegalStateException
 *     public IntentClassifier<MyIntent> classifier() {
 *         return factory.auto(MyIntent.class, rules(), examples());
 *     }
 *
 *     // 也可只建单级
 *     IntentClassifier<MyIntent> onlyRule = factory.rule(rules());
 *     IntentClassifier<MyIntent> onlyVec = factory.embedding(examples());
 *     IntentClassifier<MyIntent> onlyLlm = factory.llm(MyIntent.class);
 * }
 * }</pre>
 *
 * <p>阈值/置信度通过配置项调整，无需手填：
 * {@code fw.plus.ai.langchain4j.intent.embedding-threshold}（默认 0.72）、
 * {@code embedding-margin}（默认 0.05）、{@code llm-confidence-threshold}（默认 0.6）、
 * {@code cascade-min-confidence}（默认 0.0）。</p>
 *
 * @see LangChain4jIntentAutoConfiguration
 */
public class IntentClassifierFactory {

	private final IntentProperties properties;

	private final EmbeddingModel embeddingModel;

	private final IntentLlm llm;

	public IntentClassifierFactory(IntentProperties properties, EmbeddingModel embeddingModel, IntentLlm llm) {
		this.properties = properties == null ? new IntentProperties() : properties;
		this.embeddingModel = embeddingModel;
		this.llm = llm;
	}

	public boolean hasEmbeddingModel() {
		return embeddingModel != null;
	}

	public boolean hasLlm() {
		return llm != null;
	}

	public <I extends Enum<I>> IntentClassifier<I> rule(List<IntentRule<I>> rules) {
		return IntentClassifiers.rule(rules);
	}

	public <I extends Enum<I>> IntentClassifier<I> embedding(Map<I, List<String>> examples) {
		if (embeddingModel == null) {
			throw new IllegalStateException(
					I18nMessageResolver.resolveKey("fw.ai.IntentClassifierFactory.embeddingModelMissing"));
		}
		return IntentClassifiers.embedding(embeddingModel, examples,
				properties.getEmbeddingThreshold(), properties.getEmbeddingMargin());
	}

	public <I extends Enum<I>> IntentClassifier<I> llm(Class<I> intentType) {
		return llm(intentType, null);
	}

	public <I extends Enum<I>> IntentClassifier<I> llm(Class<I> intentType, Map<I, String> descriptions) {
		if (llm == null) {
			throw new IllegalStateException(
					I18nMessageResolver.resolveKey("fw.ai.IntentClassifierFactory.intentLlmMissing"));
		}
		return IntentClassifiers.llm(llm, intentType, descriptions, properties.getLlmConfidenceThreshold());
	}

	/**
	 * 按可用资源自动装配三级级联：规则（非空时）→ 向量（有模型且示例非空时）→ 大模型（有 {@link IntentLlm} 时）。
	 *
	 * @param intentType 使用方的意图枚举类型
	 * @param rules      L0 规则（可为 null/空）
	 * @param examples   L1 各意图示例句（可为 null/空）
	 */
	public <I extends Enum<I>> IntentClassifier<I> auto(Class<I> intentType, List<IntentRule<I>> rules,
			Map<I, List<String>> examples) {
		if (intentType == null) {
			throw new IllegalArgumentException(
					I18nMessageResolver.resolveKey("fw.ai.IntentClassifierFactory.intentTypeRequired"));
		}
		List<IntentClassifier<I>> tiers = new ArrayList<IntentClassifier<I>>();
		if (rules != null && !rules.isEmpty()) {
			tiers.add(rule(rules));
		}
		if (embeddingModel != null && examples != null && !examples.isEmpty()) {
			tiers.add(embedding(examples));
		}
		if (llm != null) {
			tiers.add(llm(intentType, null));
		}
		if (tiers.isEmpty()) {
			throw new IllegalStateException(
					I18nMessageResolver.resolveKey("fw.ai.IntentClassifierFactory.noTierAvailable"));
		}
		return IntentClassifiers.cascading(tiers, properties.getCascadeMinConfidence());
	}
}
