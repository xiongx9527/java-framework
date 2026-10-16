package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 业务分类判定契约测试：命中、未命中、追问轮次命中、开关关闭不产生调用、失败回落 `chat`。
 *
 * <p>模型调用经 {@code StructuredOutput.ChatFunction} 注入，因此全部离线可测（无需真实模型）。</p>
 */
class BusinessClassifierTest {

	private static final String SCHEMA = "{\"type\":\"object\",\"properties\":{\"orderId\":{\"type\":\"string\"}}}";

	private static BusinessRegistry registry() {
		return new BusinessRegistry()
				.register(BusinessContract.builder("order").schema(SCHEMA).build())
				.register(BusinessContract.builder("refund").schema(SCHEMA).build());
	}

	private static Langchain4jAssistantProperties.Classifier config() {
		return new Langchain4jAssistantProperties().getClassifier();
	}

	private static BusinessClassifier classifier(Langchain4jAssistantProperties.Classifier config) {
		return new BusinessClassifier(registry(), config);
	}

	// -------------------------------------------------- 判定

	@Test
	void hitsRegisteredBusiness() {
		String business = classifier(config())
				.classify(prompt -> "{\"business\":\"order\"}", Collections.emptyList(), "帮我查一下订单 A1");

		assertThat(business).isEqualTo("order");
	}

	@Test
	void promptCarriesRegisteredNamesAndCurrentMessage() {
		List<String> prompts = new ArrayList<>();

		String business = classifier(config()).classify(prompt -> {
			prompts.add(prompt);
			return "{\"business\":\"refund\"}";
		}, Collections.emptyList(), "我要退款");

		assertThat(business).isEqualTo("refund");
		assertThat(prompts).hasSize(1);
		// 已注册业务名清单必须进提示词（分类的枚举）
		assertThat(prompts.get(0)).contains("order").contains("refund").contains("我要退款");
		assertThat(prompts.get(0)).contains("只输出 JSON");
	}

	@Test
	void unregisteredResultFallsBackToChat() {
		String business = classifier(config())
				.classify(prompt -> "{\"business\":\"unknown\"}", Collections.emptyList(), "随便聊聊");

		assertThat(business).isEqualTo(BusinessRegistry.FALLBACK);
	}

	@Test
	void unparsableReplyFallsBackToChat() {
		String business = classifier(config())
				.classify(prompt -> "我觉得应该是订单吧", Collections.emptyList(), "帮我查订单");

		assertThat(business).isEqualTo(BusinessRegistry.FALLBACK);
	}

	@Test
	void emptyReplyFallsBackToChat() {
		String business = classifier(config())
				.classify(prompt -> "", Collections.emptyList(), "帮我查订单");

		assertThat(business).isEqualTo(BusinessRegistry.FALLBACK);
	}

	@Test
	void thrownFailureFallsBackToChat() {
		String business = classifier(config()).classify(prompt -> {
			throw new IllegalStateException("模型网关超时");
		}, Collections.emptyList(), "帮我查订单");

		assertThat(business).isEqualTo(BusinessRegistry.FALLBACK);
	}

	// -------------------------------------------------- 追问轮次

	@Test
	void thrownErrorFallsBackToChat() {
		// 连 Error 也收敛：分类阶段不得成为整链路的可抛点（否则装配件没有机会产出信封）
		assertThat(classifier(config()).classify(prompt -> {
			throw new StackOverflowError("schema 太深");
		}, Collections.emptyList(), "帮我查订单")).isEqualTo(BusinessRegistry.FALLBACK);

		assertThat(classifier(config()).classify(prompt -> {
			throw new OutOfMemoryError("堆外内存耗尽");
		}, Collections.emptyList(), "帮我查订单")).isEqualTo(BusinessRegistry.FALLBACK);
	}

	// -------------------------------------------------- 追问轮次

	@Test
	void followUpTurnHitsBusinessViaRecentUserMessages() {
		List<String> prompts = new ArrayList<>();

		String business = classifier(config()).classify(prompt -> {
			prompts.add(prompt);
			return "{\"business\":\"order\"}";
		}, Arrays.asList("帮我查一下订单 A1"), "那第二单呢");

		assertThat(business).isEqualTo("order");
		assertThat(prompts.get(0)).contains("帮我查一下订单 A1").contains("那第二单呢");
	}

	@Test
	void historyLengthIsLimitedByConfig() {
		List<String> prompts = new ArrayList<>();
		Langchain4jAssistantProperties.Classifier config = config();
		config.setHistoryUserMessages(2);

		classifier(config).classify(prompt -> {
			prompts.add(prompt);
			return "{\"business\":\"order\"}";
		}, Arrays.asList("第一条", "第二条", "第三条", "第四条"), "那第二单呢");

		assertThat(prompts.get(0)).contains("第三条").contains("第四条");
		assertThat(prompts.get(0)).doesNotContain("第一条").doesNotContain("第二条");
	}

	@Test
	void historyIsSkippedWhenConfiguredToZero() {
		List<String> prompts = new ArrayList<>();
		Langchain4jAssistantProperties.Classifier config = config();
		config.setHistoryUserMessages(0);

		classifier(config).classify(prompt -> {
			prompts.add(prompt);
			return "{\"business\":\"order\"}";
		}, Arrays.asList("帮我查一下订单 A1"), "那第二单呢");

		assertThat(prompts.get(0)).contains("那第二单呢").doesNotContain("帮我查一下订单 A1");
	}

	// -------------------------------------------------- 开关与降级

	@Test
	void disabledClassifierMakesNoModelCall() {
		Langchain4jAssistantProperties.Classifier config = config();
		config.setEnabled(false);
		int[] calls = {0};

		String business = classifier(config).classify(prompt -> {
			calls[0]++;
			return "{\"business\":\"order\"}";
		}, Arrays.asList("帮我查一下订单 A1"), "那第二单呢");

		assertThat(business).isEqualTo(BusinessRegistry.FALLBACK);
		assertThat(calls[0]).isZero();
	}

	@Test
	void emptyRegistryMakesNoModelCall() {
		int[] calls = {0};
		BusinessClassifier classifier = new BusinessClassifier(new BusinessRegistry(), config());

		String business = classifier.classify(prompt -> {
			calls[0]++;
			return "{\"business\":\"order\"}";
		}, Collections.emptyList(), "帮我查订单");

		assertThat(business).isEqualTo(BusinessRegistry.FALLBACK);
		assertThat(calls[0]).isZero();
	}

	@Test
	void classificationDoesNotRetryByDefault() {
		int[] calls = {0};

		String business = classifier(config()).classify(prompt -> {
			calls[0]++;
			return "不是 JSON";
		}, Collections.emptyList(), "帮我查订单");

		assertThat(business).isEqualTo(BusinessRegistry.FALLBACK);
		assertThat(calls[0]).isEqualTo(1);
	}

	@Test
	void configuredAttemptsAreUsedWhenAskedToRetry() {
		int[] calls = {0};
		Langchain4jAssistantProperties.Classifier config = config();
		config.setMaxAttempts(2);

		String business = classifier(config).classify(prompt -> {
			calls[0]++;
			return calls[0] == 1 ? "不是 JSON" : "{\"business\":\"refund\"}";
		}, Collections.emptyList(), "我要退款");

		assertThat(business).isEqualTo("refund");
		assertThat(calls[0]).isEqualTo(2);
	}

	@Test
	void blankMessageFallsBackToChatWithoutModelCall() {
		int[] calls = {0};

		String business = classifier(config()).classify(prompt -> {
			calls[0]++;
			return "{\"business\":\"order\"}";
		}, Collections.emptyList(), "   ");

		assertThat(business).isEqualTo(BusinessRegistry.FALLBACK);
		assertThat(calls[0]).isZero();
	}

	@Test
	void nullHistoryIsTolerated() {
		String business = classifier(config())
				.classify(prompt -> "{\"business\":\"order\"}", null, "帮我查订单");

		assertThat(business).isEqualTo("order");
	}
}
