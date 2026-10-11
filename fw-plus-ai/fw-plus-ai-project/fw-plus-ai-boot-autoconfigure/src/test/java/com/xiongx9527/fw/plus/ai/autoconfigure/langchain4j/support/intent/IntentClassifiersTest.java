package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 规则分类器与级联分类器：命中短路、低置信继续、抛错跳过、全miss兜底。
 */
class IntentClassifiersTest {

	@Test
	void ruleReturnsFirstMatchingIntent() {
		IntentClassifier<SampleIntent> classifier = IntentClassifiers.rule(
				IntentRule.of(SampleIntent.HUMAN, "转人工|人工客服"),
				IntentRule.of(SampleIntent.LOGISTICS, "物流|快递"));

		IntentResult<SampleIntent> result = classifier.classify(IntentContext.of("我要转人工"));

		assertThat(result.intent()).isEqualTo(SampleIntent.HUMAN);
		assertThat(result.source()).isEqualTo(IntentSource.RULE);
		assertThat(result.confidence()).isEqualTo(1.0d);
	}

	@Test
	void ruleReturnsNoneWhenNothingMatches() {
		IntentClassifier<SampleIntent> classifier = IntentClassifiers.rule(
				IntentRule.of(SampleIntent.HUMAN, "转人工"));

		IntentResult<SampleIntent> result = classifier.classify(IntentContext.of("随便聊聊"));

		assertThat(result.matched()).isFalse();
		assertThat(result.source()).isEqualTo(IntentSource.RULE);
	}

	@Test
	void ruleIgnoresNullText() {
		IntentClassifier<SampleIntent> classifier = IntentClassifiers.rule(
				IntentRule.of(SampleIntent.HUMAN, "转人工"));

		assertThat(classifier.classify(null).matched()).isFalse();
	}

	@Test
	void cascadeFallsThroughToLaterTier() {
		IntentClassifier<SampleIntent> miss = context -> IntentResult.none(IntentSource.EMBEDDING);
		IntentClassifier<SampleIntent> hit = context ->
				IntentResult.of(SampleIntent.REFUND, 0.9d, IntentSource.LLM);

		IntentResult<SampleIntent> result =
				IntentClassifiers.cascading(miss, hit).classify(IntentContext.of("退款"));

		assertThat(result.intent()).isEqualTo(SampleIntent.REFUND);
		assertThat(result.source()).isEqualTo(IntentSource.LLM);
	}

	@Test
	void cascadeSkipsTierBelowMinConfidence() {
		IntentClassifier<SampleIntent> low = context ->
				IntentResult.of(SampleIntent.REFUND, 0.3d, IntentSource.EMBEDDING);
		IntentClassifier<SampleIntent> high = context ->
				IntentResult.of(SampleIntent.HUMAN, 0.8d, IntentSource.LLM);

		IntentResult<SampleIntent> result =
				IntentClassifiers.cascading(0.6d, low, high).classify(IntentContext.of("x"));

		assertThat(result.intent()).isEqualTo(SampleIntent.HUMAN);
		assertThat(result.source()).isEqualTo(IntentSource.LLM);
	}

	@Test
	void cascadeSkipsThrowingTier() {
		IntentClassifier<SampleIntent> broken = context -> {
			throw new IllegalStateException("模型不可用");
		};
		IntentClassifier<SampleIntent> hit = context ->
				IntentResult.of(SampleIntent.LOGISTICS, 0.9d, IntentSource.LLM);

		IntentResult<SampleIntent> result =
				IntentClassifiers.cascading(broken, hit).classify(IntentContext.of("x"));

		assertThat(result.intent()).isEqualTo(SampleIntent.LOGISTICS);
	}

	@Test
	void cascadeReturnsFallbackWhenAllMiss() {
		IntentClassifier<SampleIntent> miss = context -> IntentResult.none(IntentSource.RULE);

		IntentResult<SampleIntent> result =
				IntentClassifiers.cascading(miss, miss).classify(IntentContext.of("x"));

		assertThat(result.matched()).isFalse();
		assertThat(result.source()).isEqualTo(IntentSource.FALLBACK);
	}
}
