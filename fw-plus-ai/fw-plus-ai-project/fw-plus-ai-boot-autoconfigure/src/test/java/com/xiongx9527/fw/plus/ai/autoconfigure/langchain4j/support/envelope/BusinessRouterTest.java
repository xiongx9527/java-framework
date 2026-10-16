package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 分类驱动路由契约测试：每轮重新判定（不沿用上一轮、不缓存）、未注册/失败回落 `chat`、
 * 同一会话共用一份历史且切换业务不清空。
 */
class BusinessRouterTest {

	private static final String SCHEMA = "{\"type\":\"object\",\"properties\":{\"orderId\":{\"type\":\"string\"}}}";

	private static BusinessRegistry registry() {
		return new BusinessRegistry()
				.register(BusinessContract.builder("order").schema(SCHEMA).build())
				.register(BusinessContract.builder("refund").schema(SCHEMA).build());
	}

	private static BusinessRouter router() {
		return new BusinessRouter(registry(), new Langchain4jAssistantProperties().getClassifier());
	}

	// -------------------------------------------------- 判定与契约解析

	@Test
	void routesToRegisteredBusiness() {
		BusinessRouter.RoutedTurn turn = router()
				.route(prompt -> "{\"business\":\"order\"}", Collections.emptyList(), "帮我查订单 A1");

		assertThat(turn.getBusiness()).isEqualTo("order");
		assertThat(turn.getContract()).isNotNull();
		assertThat(turn.getContract().getName()).isEqualTo("order");
		assertThat(turn.isFallback()).isFalse();
	}

	@Test
	void unregisteredBusinessFallsBackToChat() {
		// 只注册了 order/refund，分类给出 refund 时能命中；给出未注册名则回落
		BusinessRouter.RoutedTurn turn = router()
				.route(prompt -> "{\"business\":\"shipping\"}", Collections.emptyList(), "帮我查物流");

		assertThat(turn.getBusiness()).isEqualTo(BusinessRegistry.FALLBACK);
		assertThat(turn.getContract()).isNull();
		assertThat(turn.isFallback()).isTrue();
	}

	@Test
	void classificationFailureFallsBackToChat() {
		BusinessRouter.RoutedTurn turn = router().route(prompt -> {
			throw new IllegalStateException("模型网关超时");
		}, Collections.emptyList(), "帮我查订单 A1");

		assertThat(turn.isFallback()).isTrue();
		assertThat(turn.getBusiness()).isEqualTo(BusinessRegistry.FALLBACK);
	}

	@Test
	void fallbackTurnIsStillAValidEnvelopeSource() {
		// 兜底轮次仍然要能产出合法信封（业务名 chat、无结构结果）
		BusinessRouter.RoutedTurn turn = router().route(prompt -> "不是 JSON", Collections.emptyList(), "随便聊聊");

		EnvelopeAssembler assembler = new EnvelopeAssembler();
		ChatEnvelope envelope = assembler.ensure(turn.getBusiness(), "1", EnvelopeAssembler.RoundMode.CHAT, "你好呀");
		assertThat(envelope.isOk()).isTrue();
		assertThat(envelope.getBusiness()).isEqualTo("chat");
		assertThat(envelope.getResult()).isNull();
	}

	// -------------------------------------------------- 每轮重新判定

	@Test
	void adjacentTurnsWithDifferentTopicsAreJudgedSeparately() {
		BusinessRouter router = router();
		int[] calls = {0};

		BusinessRouter.RoutedTurn first = router.route(prompt -> {
			calls[0]++;
			return "{\"business\":\"order\"}";
		}, Collections.emptyList(), "帮我查订单 A1");

		BusinessRouter.RoutedTurn second = router.route(prompt -> {
			calls[0]++;
			return "{\"business\":\"refund\"}";
		}, Collections.singletonList("帮我查订单 A1"), "我要退款");

		assertThat(first.getBusiness()).isEqualTo("order");
		assertThat(second.getBusiness()).isEqualTo("refund");
		// 两轮各判定一次：没有缓存、没有复用
		assertThat(calls[0]).isEqualTo(2);
	}

	@Test
	void doesNotCarryOverPreviousTurnResult() {
		BusinessRouter router = router();
		int[] calls = {0};

		BusinessRouter.RoutedTurn first = router.route(prompt -> {
			calls[0]++;
			return "{\"business\":\"order\"}";
		}, Collections.emptyList(), "帮我查订单 A1");

		// 第二轮分类判不出来（未注册结果）：不得沿用上一轮的 order
		BusinessRouter.RoutedTurn second = router.route(prompt -> {
			calls[0]++;
			return "{\"business\":\"unknown\"}";
		}, Collections.singletonList("帮我查订单 A1"), "嗯，那个怎么样了");

		assertThat(first.getBusiness()).isEqualTo("order");
		assertThat(second.getBusiness()).isEqualTo(BusinessRegistry.FALLBACK);
		assertThat(calls[0]).isEqualTo(2);
	}

	@Test
	void topicSwitchingBackStillRoutesCorrectly() {
		BusinessRouter router = router();

		assertThat(router.route(prompt -> "{\"business\":\"order\"}", Collections.emptyList(), "查订单")
				.getBusiness()).isEqualTo("order");
		assertThat(router.route(prompt -> "{\"business\":\"unknown\"}", Collections.emptyList(), "随便聊聊")
				.getBusiness()).isEqualTo("chat");
		assertThat(router.route(prompt -> "{\"business\":\"order\"}", Collections.emptyList(), "还有一单")
				.getBusiness()).isEqualTo("order");
	}

	@Test
	void disabledClassifierRoutesEverythingToChatWithoutModelCall() {
		Langchain4jAssistantProperties.Classifier config = new Langchain4jAssistantProperties().getClassifier();
		config.setEnabled(false);
		int[] calls = {0};
		BusinessRouter router = new BusinessRouter(registry(), config);

		BusinessRouter.RoutedTurn turn = router.route(prompt -> {
			calls[0]++;
			return "{\"business\":\"order\"}";
		}, Collections.emptyList(), "帮我查订单 A1");

		assertThat(turn.isFallback()).isTrue();
		assertThat(calls[0]).isZero();
	}

	// -------------------------------------------------- 共用历史

	@Test
	void historyIsSharedAcrossBusinessSwitch() {
		BusinessRouter router = router();
		List<String> prompts = new ArrayList<>();
		// 同一会话的历史：先订单轮次的用户原文，再本轮切换后的退款诉求
		List<String> sessionHistory = new ArrayList<>(Collections.singletonList("帮我查订单 A1"));

		BusinessRouter.RoutedTurn turn = router.route(prompt -> {
			prompts.add(prompt);
			return "{\"business\":\"refund\"}";
		}, sessionHistory, "我要退款");

		assertThat(turn.getBusiness()).isEqualTo("refund");
		// 切换业务不清空历史：退款轮次依然能看到该会话此前的消息
		assertThat(prompts.get(0)).contains("帮我查订单 A1").contains("我要退款");
	}

	@Test
	void historyIsSharedAcrossThreeTurns() {
		BusinessRouter router = router();
		List<String> prompts = new ArrayList<>();
		List<String> history = new ArrayList<>();

		history.add("帮我查订单 A1");
		router.route(prompt -> {
			prompts.add(prompt);
			return "{\"business\":\"refund\"}";
		}, history, "我要退款");

		history.add("我要退款");
		router.route(prompt -> {
			prompts.add(prompt);
			return "{\"business\":\"order\"}";
		}, history, "还有一单");

		// 第三轮（切回 order）能看到前两轮的用户消息
		assertThat(prompts.get(1)).contains("帮我查订单 A1").contains("我要退款").contains("还有一单");
	}

	@Test
	void routingNeverMutatesProvidedHistory() {
		BusinessRouter router = router();
		List<String> sessionHistory = new ArrayList<>(Arrays.asList("帮我查订单 A1", "我要退款"));

		router.route(prompt -> "{\"business\":\"order\"}", sessionHistory, "那第二单呢");

		assertThat(sessionHistory).containsExactly("帮我查订单 A1", "我要退款");
	}

	@Test
	void nullHistoryIsTolerated() {
		BusinessRouter.RoutedTurn turn = router()
				.route(prompt -> "{\"business\":\"order\"}", null, "帮我查订单 A1");

		assertThat(turn.getBusiness()).isEqualTo("order");
	}
}
