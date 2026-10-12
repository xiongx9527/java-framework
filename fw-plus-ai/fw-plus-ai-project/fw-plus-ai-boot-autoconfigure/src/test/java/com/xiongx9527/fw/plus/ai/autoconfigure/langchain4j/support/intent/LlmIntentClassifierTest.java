package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 大模型分类器：JSON 解析（含代码块/噪声容错）、大小写兼容、NONE/未知/低置信/抛错降级。
 */
class LlmIntentClassifierTest {

	@Test
	void parsesIntentConfidenceAndSlots() {
		IntentClassifier<SampleIntent> classifier = IntentClassifiers.llm(
				(system, user) -> "{\"intent\":\"LOGISTICS\",\"confidence\":0.9,\"slots\":{\"orderNo\":\"A1\"}}",
				SampleIntent.class);

		IntentResult<SampleIntent> result = classifier.classify(IntentContext.of("查一下物流"));

		assertThat(result.intent()).isEqualTo(SampleIntent.LOGISTICS);
		assertThat(result.confidence()).isEqualTo(0.9d);
		assertThat(result.slot("orderNo")).isEqualTo("A1");
		assertThat(result.source()).isEqualTo(IntentSource.LLM);
	}

	@Test
	void toleratesMarkdownFenceAndSurroundingNoise() {
		IntentClassifier<SampleIntent> classifier = IntentClassifiers.llm(
				(system, user) -> "好的，结果如下：\n```json\n{\"intent\":\"refund\",\"confidence\":0.8}\n```",
				SampleIntent.class);

		IntentResult<SampleIntent> result = classifier.classify(IntentContext.of("我要退款"));

		assertThat(result.intent()).isEqualTo(SampleIntent.REFUND);
	}

	@Test
	void rejectsNoneIntent() {
		IntentClassifier<SampleIntent> classifier = IntentClassifiers.llm(
				(system, user) -> "{\"intent\":\"NONE\",\"confidence\":0}", SampleIntent.class);

		assertThat(classifier.classify(IntentContext.of("随便聊聊")).matched()).isFalse();
	}

	@Test
	void rejectsUnknownIntentName() {
		IntentClassifier<SampleIntent> classifier = IntentClassifiers.llm(
				(system, user) -> "{\"intent\":\"FOO\",\"confidence\":0.9}", SampleIntent.class);

		assertThat(classifier.classify(IntentContext.of("x")).matched()).isFalse();
	}

	@Test
	void rejectsWhenConfidenceBelowThreshold() {
		IntentClassifier<SampleIntent> classifier = IntentClassifiers.llm(
				(system, user) -> "{\"intent\":\"REFUND\",\"confidence\":0.2}", SampleIntent.class);

		assertThat(classifier.classify(IntentContext.of("x")).matched()).isFalse();
	}

	@Test
	void degradesWhenLlmThrows() {
		IntentClassifier<SampleIntent> classifier = IntentClassifiers.llm(
				(system, user) -> {
					throw new RuntimeException("模型不可用");
				}, SampleIntent.class);

		assertThat(classifier.classify(IntentContext.of("x")).matched()).isFalse();
	}

	@Test
	void promptListsAllIntentNames() {
		StringBuilder captured = new StringBuilder();
		IntentClassifier<SampleIntent> classifier = IntentClassifiers.llm(
				(system, user) -> {
					captured.append(system);
					return "{\"intent\":\"NONE\",\"confidence\":0}";
				}, SampleIntent.class);

		classifier.classify(IntentContext.of("x"));

		assertThat(captured.toString())
				.contains("LOGISTICS")
				.contains("REFUND")
				.contains("CHITCHAT")
				.contains("HUMAN");
	}
}
