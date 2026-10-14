package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope;

import com.alibaba.fastjson2.JSONObject;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 编排契约测试：每轮分类决定业务、结构化轮次读历史 + 直连生成 + 失败回喂重试（重试零写入）、
 * 非结构化轮次不写记忆、任何失败都收敛成信封而不是抛异常、`start` 帧数据形态。
 */
class EnvelopeRuntimeTest {

	private static final String MEMORY_ID = "session-1";

	private static final String ORDER_SCHEMA = "{"
			+ "\"type\":\"object\","
			+ "\"properties\":{\"orderId\":{\"type\":\"string\"}},"
			+ "\"required\":[\"orderId\"]}";

	private final ChatMemoryStore store = new InMemoryChatMemoryStore();

	// -------------------------------------------------- 公共装配

	private static Langchain4jAssistantProperties properties() {
		return new Langchain4jAssistantProperties();
	}

	/** 命中即结构化产出的业务契约（v=2，自带系统提示） */
	private static BusinessContract order() {
		return BusinessContract.builder("order")
				.schema(ORDER_SCHEMA)
				.structured(true)
				.systemHint("你是订单助手")
				.version("2")
				.build();
	}

	/** 命中但不产出结构化结果的业务契约（结构化开关保持全局默认 false） */
	private static BusinessContract refund() {
		return BusinessContract.builder("refund")
				.schema(ORDER_SCHEMA)
				.structured(false)
				.build();
	}

	private EnvelopeRuntime runtime(ChatLanguageModel model, BusinessContract... contracts) {
		return runtime(model, properties(), contracts);
	}

	private EnvelopeRuntime runtime(ChatLanguageModel model, Langchain4jAssistantProperties properties,
			BusinessContract... contracts) {
		BusinessRegistry registry = new BusinessRegistry();
		for (BusinessContract contract : contracts) {
			registry.register(contract);
		}
		return new EnvelopeRuntime(model, store, registry, properties);
	}

	private static String classifiedAs(String business) {
		return "{\"business\":\"" + business + "\"}";
	}

	// -------------------------------------------------- 分类与路由（decide）

	@Test
	void decideFallsBackWhenNoContractRegistered() {
		ScriptedModel model = new ScriptedModel();

		EnvelopeRuntime.Round round = runtime(model).decide(MEMORY_ID, "你好");

		assertThat(round.getBusiness()).isEqualTo(BusinessRegistry.FALLBACK);
		assertThat(round.getContract()).isNull();
		assertThat(round.isStructured()).isFalse();
		assertThat(round.getVersion()).isEqualTo(ChatEnvelope.DEFAULT_VERSION);
		assertThat(round.getRequestId()).isNotBlank();
		// 注册表为空 → 不产生分类调用
		assertThat(model.requests).isEmpty();
	}

	@Test
	void decideClassifiesToRegisteredBusiness() {
		ScriptedModel model = new ScriptedModel(classifiedAs("order"));

		EnvelopeRuntime.Round round = runtime(model, order()).decide(MEMORY_ID, "帮我查订单 A1");

		assertThat(round.getBusiness()).isEqualTo("order");
		assertThat(round.getContract()).isNotNull();
		assertThat(round.isStructured()).isTrue();
		assertThat(round.getVersion()).isEqualTo("2");
		assertThat(model.requests).hasSize(1);
		String prompt = allText(model.requests.get(0));
		assertThat(prompt).contains("order").contains("帮我查订单 A1");
	}

	@Test
	void decideIsPerTurnNotSticky() {
		// 每轮重新判定：不缓存、不沿用上一轮结果
		ScriptedModel model = new ScriptedModel(classifiedAs("order"), classifiedAs("chat"));

		EnvelopeRuntime runtime = runtime(model, order());
		assertThat(runtime.decide(MEMORY_ID, "帮我查订单 A1").getBusiness()).isEqualTo("order");
		assertThat(runtime.decide(MEMORY_ID, "随便聊聊").getBusiness()).isEqualTo(BusinessRegistry.FALLBACK);
	}

	@Test
	void decideFallsBackWhenClassificationMisses() {
		ScriptedModel model = new ScriptedModel(classifiedAs("not-registered"));

		EnvelopeRuntime.Round round = runtime(model, order()).decide(MEMORY_ID, "帮我查订单 A1");

		assertThat(round.getBusiness()).isEqualTo(BusinessRegistry.FALLBACK);
		assertThat(round.getContract()).isNull();
	}

	@Test
	void decidePassesRecentUserMessagesAndDoesNotWriteMemory() {
		ScriptedModel model = new ScriptedModel(classifiedAs("order"));
		EnvelopeRuntime runtime = runtime(model, order());
		new EnvelopeSessionMemory(store, 20).recordTurn(MEMORY_ID, "帮我查订单 A1",
				ChatEnvelope.structured("order", JSONObject.parseObject("{\"orderId\":\"A1\"}")));

		EnvelopeRuntime.Round round = runtime.decide(MEMORY_ID, "那第二单呢");

		String prompt = allText(model.requests.get(0));
		assertThat(prompt).contains("帮我查订单 A1").contains("那第二单呢");
		// 分类输入只带历史用户消息（结果摘要不进分类提示）
		assertThat(prompt).doesNotContain("已处理业务");
		// 分类不写记忆：store 仍是刚才写回的两条
		assertThat(store.getMessages(MEMORY_ID)).hasSize(2);
		assertThat(round.getBusiness()).isEqualTo("order");
	}

	@Test
	void decideFallsBackWhenClassifierDisabled() {
		Langchain4jAssistantProperties properties = properties();
		properties.getClassifier().setEnabled(false);
		ScriptedModel model = new ScriptedModel(classifiedAs("order"));

		EnvelopeRuntime.Round round = runtime(model, properties, order()).decide(MEMORY_ID, "帮我查订单 A1");

		assertThat(round.getBusiness()).isEqualTo(BusinessRegistry.FALLBACK);
		assertThat(model.requests).isEmpty();
	}

	@Test
	void decideFallsBackWhenClassificationCallFails() {
		ChatLanguageModel failing = new ChatLanguageModel() {
			@Override
			public Response<AiMessage> generate(List<ChatMessage> messages) {
				throw new RuntimeException("模型不可用");
			}
		};

		EnvelopeRuntime.Round round = runtime(failing, order()).decide(MEMORY_ID, "帮我查订单 A1");

		assertThat(round.getBusiness()).isEqualTo(BusinessRegistry.FALLBACK);
		assertThat(round.isStructured()).isFalse();
	}

	@Test
	void decideFallsBackWhenClassificationThrowsError() {
		ChatLanguageModel failing = new ChatLanguageModel() {
			@Override
			public Response<AiMessage> generate(List<ChatMessage> messages) {
				throw new StackOverflowError("schema 太深");
			}
		};

		assertThat(runtime(failing, order()).decide(MEMORY_ID, "帮我查订单 A1").getBusiness())
				.isEqualTo(BusinessRegistry.FALLBACK);
	}

	@Test
	void decideKeepsBusinessButNotStructuredWhenContractOptsOut() {
		// 业务命中但结构化开关未打开：business 仍为契约名，只是本轮不产出 result
		ScriptedModel model = new ScriptedModel(classifiedAs("refund"));

		EnvelopeRuntime.Round round = runtime(model, refund()).decide(MEMORY_ID, "我要退款");

		assertThat(round.getBusiness()).isEqualTo("refund");
		assertThat(round.getContract()).isNotNull();
		assertThat(round.isStructured()).isFalse();
	}

	@Test
	void startDataCarriesRequestIdBusinessAndTokenFlag() {
		ScriptedModel model = new ScriptedModel(classifiedAs("order"), classifiedAs("chat"));
		EnvelopeRuntime runtime = runtime(model, order());

		EnvelopeRuntime.Round structured = runtime.decide(MEMORY_ID, "帮我查订单 A1");
		JSONObject structuredStart = structured.getStartData();
		assertThat(structuredStart.getString("requestId")).isEqualTo(structured.getRequestId());
		assertThat(structuredStart.getString("business")).isEqualTo("order");
		assertThat(structuredStart.getBooleanValue("tokens")).isFalse();

		EnvelopeRuntime.Round chat = runtime.decide(MEMORY_ID, "随便聊聊");
		JSONObject chatStart = chat.getStartData();
		assertThat(chatStart.getString("requestId")).isEqualTo(chat.getRequestId());
		assertThat(chatStart.getString("business")).isEqualTo(BusinessRegistry.FALLBACK);
		assertThat(chatStart.getBooleanValue("tokens")).isTrue();
	}

	@Test
	void requestIdDiffersBetweenRounds() {
		ScriptedModel model = new ScriptedModel(classifiedAs("chat"), classifiedAs("chat"));
		EnvelopeRuntime runtime = runtime(model, order());

		assertThat(runtime.decide(MEMORY_ID, "一").getRequestId())
				.isNotEqualTo(runtime.decide(MEMORY_ID, "二").getRequestId());
	}

	// -------------------------------------------------- 结构化轮次

	@Test
	void structuredRoundProducesEnvelopeAndWritesHistory() {
		ScriptedModel model = new ScriptedModel(classifiedAs("order"), "{\"orderId\":\"A1\"}");
		EnvelopeRuntime runtime = runtime(model, order());

		EnvelopeRuntime.Round round = runtime.decide(MEMORY_ID, "帮我查订单 A1");
		ChatEnvelope envelope = runtime.structuredRound(round, MEMORY_ID, "帮我查订单 A1");

		assertThat(envelope.isOk()).isTrue();
		assertThat(envelope.getBusiness()).isEqualTo("order");
		assertThat(envelope.getVersion()).isEqualTo("2");
		assertThat(envelope.getResult().getString("orderId")).isEqualTo("A1");
		assertThat(envelope.getText()).isEmpty();

		// 消息拼接：系统提示（契约声明）+ schema 指令 + 本轮文本
		List<ChatMessage> messages = model.requests.get(1);
		assertThat(messages.get(0)).isInstanceOf(SystemMessage.class);
		assertThat(((SystemMessage) messages.get(0)).text()).isEqualTo("你是订单助手");
		String user = allText(messages.subList(1, messages.size()));
		assertThat(user).contains("你必须只输出 JSON").contains("orderId").contains("帮我查订单 A1");

		// 写回：本轮用户原文 + 一条结果摘要
		List<ChatMessage> history = store.getMessages(MEMORY_ID);
		assertThat(history).hasSize(2);
		assertThat(text(history.get(0))).isEqualTo("帮我查订单 A1");
		assertThat(text(history.get(1))).isEqualTo(EnvelopeSessionMemory.summarize(envelope));
	}

	@Test
	void structuredRoundUsesDefaultSystemHintWhenContractOmitsIt() {
		ScriptedModel model = new ScriptedModel(classifiedAs("refund"), "{\"orderId\":\"A1\"}");
		EnvelopeRuntime runtime = runtime(model, refund());

		EnvelopeRuntime.Round round = runtime.decide(MEMORY_ID, "我要退款");
		EnvelopeRuntime.Round structuredRound = new EnvelopeRuntime.Round(round.getRequestId(), round.getBusiness(),
				round.getContract(), true, round.getVersion());
		runtime.structuredRound(structuredRound, MEMORY_ID, "我要退款");

		assertThat(((SystemMessage) model.requests.get(1).get(0)).text())
				.isEqualTo(EnvelopeRuntime.DEFAULT_SYSTEM_HINT);
	}

	@Test
	void structuredRoundRetriesWithErrorFeedbackAndKeepsHistoryClean() {
		ScriptedModel model = new ScriptedModel(classifiedAs("order"), "{\"orderId\":123}", "{\"orderId\":\"A1\"}");
		EnvelopeRuntime runtime = runtime(model, order());

		EnvelopeRuntime.Round round = runtime.decide(MEMORY_ID, "帮我查订单 A1");
		ChatEnvelope envelope = runtime.structuredRound(round, MEMORY_ID, "帮我查订单 A1");

		assertThat(envelope.isOk()).isTrue();
		assertThat(model.requests).hasSize(3);
		// 重试提示把上一轮的错误清单回喂给模型
		assertThat(allText(model.requests.get(2))).contains("上一次的输出未通过校验");

		// 重试期间零写入：历史只有最终结果那一条
		List<ChatMessage> history = store.getMessages(MEMORY_ID);
		assertThat(history).hasSize(2);
		for (ChatMessage message : history) {
			assertThat(text(message)).doesNotContain("上一次").doesNotContain("未通过校验");
		}
		assertThat(text(history.get(1))).isEqualTo(EnvelopeSessionMemory.summarize(envelope));
	}

	@Test
	void structuredRoundRepeatsHistoryInRetryPrompt() {
		ScriptedModel model = new ScriptedModel(classifiedAs("order"), "{\"orderId\":123}", "{\"orderId\":\"A1\"}");
		EnvelopeRuntime runtime = runtime(model, order());
		new EnvelopeSessionMemory(store, 20).recordTurn(MEMORY_ID, "上一轮说过的订单",
				ChatEnvelope.structured("order", JSONObject.parseObject("{\"orderId\":\"A0\"}")));

		EnvelopeRuntime.Round round = runtime.decide(MEMORY_ID, "帮我查订单 A1");
		runtime.structuredRound(round, MEMORY_ID, "帮我查订单 A1");

		// 历史快照在重试循环之外取一次，因此每次尝试看到的上下文一致
		assertThat(allText(model.requests.get(1))).contains("上一轮说过的订单");
		assertThat(allText(model.requests.get(2))).contains("上一轮说过的订单");
	}

	@Test
	void structuredRoundFailsAfterContractMaxAttempts() {
		BusinessContract twoAttempts = BusinessContract.builder("order")
				.schema(ORDER_SCHEMA)
				.structured(true)
				.maxAttempts(2)
				.build();
		ScriptedModel model = new ScriptedModel(classifiedAs("order"), "{\"orderId\":123}", "{\"orderId\":456}");
		EnvelopeRuntime runtime = runtime(model, twoAttempts);

		EnvelopeRuntime.Round round = runtime.decide(MEMORY_ID, "帮我查订单 A1");
		ChatEnvelope envelope = runtime.structuredRound(round, MEMORY_ID, "帮我查订单 A1");

		assertThat(envelope.isOk()).isFalse();
		assertThat(envelope.getBusiness()).isEqualTo("order");
		assertThat(envelope.getResult()).isNull();
		assertThat(envelope.getText()).isEqualTo(ChatEnvelope.DEFAULT_FAILURE_TEXT);
		// 1 次分类 + 2 次生成（契约声明的上限）
		assertThat(model.requests).hasSize(3);
		// 失败轮次也只写「用户原文 + 失败说明」，校验错误原文不进历史
		List<ChatMessage> history = store.getMessages(MEMORY_ID);
		assertThat(history).hasSize(2);
		assertThat(text(history.get(1))).isEqualTo(ChatEnvelope.DEFAULT_FAILURE_TEXT);
	}

	@Test
	void structuredRoundUsesGlobalMaxAttemptsWhenContractOmitsIt() {
		Langchain4jAssistantProperties properties = properties();
		properties.getStructured().setMaxAttempts(2);
		BusinessContract contract = BusinessContract.builder("order").schema(ORDER_SCHEMA).structured(true).build();
		ScriptedModel model = new ScriptedModel(classifiedAs("order"), "{\"orderId\":1}", "{\"orderId\":2}");
		EnvelopeRuntime runtime = runtime(model, properties, contract);

		EnvelopeRuntime.Round round = runtime.decide(MEMORY_ID, "帮我查订单 A1");
		ChatEnvelope envelope = runtime.structuredRound(round, MEMORY_ID, "帮我查订单 A1");

		assertThat(envelope.isOk()).isFalse();
		assertThat(model.requests).hasSize(3);
	}

	@Test
	void structuredRoundOnceDoesNotRetry() {
		// 流式：模型输出已逐帧下发，不重发请求
		ScriptedModel model = new ScriptedModel(classifiedAs("order"), "{\"orderId\":123}");
		EnvelopeRuntime runtime = runtime(model, order());

		EnvelopeRuntime.Round round = runtime.decide(MEMORY_ID, "帮我查订单 A1");
		ChatEnvelope envelope = runtime.structuredRoundOnce(round, MEMORY_ID, "帮我查订单 A1");

		assertThat(envelope.isOk()).isFalse();
		assertThat(envelope.getText()).isEqualTo(ChatEnvelope.DEFAULT_FAILURE_TEXT);
		assertThat(model.requests).hasSize(2);
	}

	@Test
	void structuredRoundSeesPreviousTurnHistory() {
		ScriptedModel model = new ScriptedModel(classifiedAs("order"), "{\"orderId\":\"A1\"}",
				classifiedAs("order"), "{\"orderId\":\"A2\"}");
		EnvelopeRuntime runtime = runtime(model, order());

		EnvelopeRuntime.Round first = runtime.decide(MEMORY_ID, "帮我查订单 A1");
		runtime.structuredRound(first, MEMORY_ID, "帮我查订单 A1");
		EnvelopeRuntime.Round second = runtime.decide(MEMORY_ID, "那第二单呢");
		ChatEnvelope envelope = runtime.structuredRound(second, MEMORY_ID, "那第二单呢");

		// 同一会话共用一份历史：第二轮能看到第一轮的用户原文与结果摘要
		String context = allText(model.requests.get(3));
		assertThat(context).contains("帮我查订单 A1").contains("A1").contains("那第二单呢");
		assertThat(envelope.getResult().getString("orderId")).isEqualTo("A2");
		assertThat(store.getMessages(MEMORY_ID)).hasSize(4);
	}

	@Test
	void structuredRoundNeverThrowsWhenGenerationFails() {
		ChatLanguageModel failing = new ChatLanguageModel() {
			private int calls = 0;

			@Override
			public Response<AiMessage> generate(List<ChatMessage> messages) {
				calls++;
				if (calls == 1) {
					return Response.from(AiMessage.from(classifiedAs("order")));
				}
				throw new RuntimeException("模型挂了");
			}
		};
		EnvelopeRuntime runtime = runtime(failing, order());

		EnvelopeRuntime.Round round = runtime.decide(MEMORY_ID, "帮我查订单 A1");
		ChatEnvelope envelope = runtime.structuredRound(round, MEMORY_ID, "帮我查订单 A1");

		assertThat(envelope.isOk()).isFalse();
		assertThat(envelope.getText()).isEqualTo(ChatEnvelope.DEFAULT_FAILURE_TEXT);
		assertThat(store.getMessages(MEMORY_ID)).hasSize(2);
	}

	@Test
	void structuredRoundRejectsNonStructuredRound() {
		ScriptedModel model = new ScriptedModel(classifiedAs("chat"));
		EnvelopeRuntime runtime = runtime(model, order());
		EnvelopeRuntime.Round round = runtime.decide(MEMORY_ID, "随便聊聊");

		assertThatThrownBy(() -> runtime.structuredRound(round, MEMORY_ID, "随便聊聊"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	// -------------------------------------------------- 非结构化轮次（闲聊）

	@Test
	void chatEnvelopeWrapsTextAndDoesNotWriteMemory() {
		ScriptedModel model = new ScriptedModel(classifiedAs("chat"));
		EnvelopeRuntime runtime = runtime(model, order());
		EnvelopeRuntime.Round round = runtime.decide(MEMORY_ID, "随便聊聊");

		ChatEnvelope envelope = runtime.chatEnvelope(round, "你好呀");

		assertThat(envelope.isOk()).isTrue();
		assertThat(envelope.getBusiness()).isEqualTo(BusinessRegistry.FALLBACK);
		assertThat(envelope.getResult()).isNull();
		assertThat(envelope.getText()).isEqualTo("你好呀");
		// 闲聊轮次由既有通道写记忆，本层不重复写
		assertThat(store.getMessages(MEMORY_ID)).isEmpty();
	}

	@Test
	void chatEnvelopeKeepsBusinessForContractWithoutStructuredResult() {
		ScriptedModel model = new ScriptedModel(classifiedAs("refund"));
		EnvelopeRuntime runtime = runtime(model, refund());
		EnvelopeRuntime.Round round = runtime.decide(MEMORY_ID, "我要退款");

		ChatEnvelope envelope = runtime.chatEnvelope(round, "请提供订单号");

		assertThat(envelope.isOk()).isTrue();
		assertThat(envelope.getBusiness()).isEqualTo("refund");
		assertThat(envelope.getResult()).isNull();
		assertThat(envelope.getText()).isEqualTo("请提供订单号");
	}

	@Test
	void chatEnvelopeDegradesBlankReplyToFailure() {
		EnvelopeRuntime runtime = runtime(new ScriptedModel(), order());
		EnvelopeRuntime.Round round = runtime.decide(MEMORY_ID, "你好");

		assertThat(runtime.chatEnvelope(round, "").isOk()).isFalse();
		assertThat(runtime.chatEnvelope(round, "   ").isOk()).isFalse();
		assertThat(runtime.chatEnvelope(round, null).isOk()).isFalse();
		assertThat(runtime.chatEnvelope(round, SimpleAssistantText.DONE).isOk()).isFalse();
	}

	// -------------------------------------------------- 构造约束

	@Test
	void nullModelOrStoreIsRejected() {
		assertThatThrownBy(() -> new EnvelopeRuntime(null, store, new BusinessRegistry(), properties()))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new EnvelopeRuntime(new ScriptedModel(), null, new BusinessRegistry(), properties()))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new EnvelopeRuntime(new ScriptedModel(), store, new BusinessRegistry(), null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void nullRegistryMeansEveryRoundFallsBack() {
		EnvelopeRuntime runtime = new EnvelopeRuntime(new ScriptedModel(), store, null, properties());

		assertThat(runtime.decide(MEMORY_ID, "帮我查订单 A1").getBusiness()).isEqualTo(BusinessRegistry.FALLBACK);
	}

	// -------------------------------------------------- 测试替身

	/** 按脚本依次返回回复的假模型：同时记录每次请求的消息序列 */
	private static final class ScriptedModel implements ChatLanguageModel {

		private final Deque<String> replies;

		private final List<List<ChatMessage>> requests = new ArrayList<List<ChatMessage>>();

		private ScriptedModel(String... replies) {
			this.replies = new ArrayDeque<String>(Arrays.asList(replies));
		}

		@Override
		public Response<AiMessage> generate(List<ChatMessage> messages) {
			requests.add(new ArrayList<ChatMessage>(messages));
			return Response.from(AiMessage.from(replies.isEmpty() ? "" : replies.poll()));
		}
	}

	/** 既有流式通道对空回复使用的兜底标记（不引 SimpleAssistant，避免测试耦合） */
	private static final class SimpleAssistantText {

		private static final String DONE = "[DONE]";
	}

	private static String allText(List<ChatMessage> messages) {
		StringBuilder text = new StringBuilder();
		for (ChatMessage message : messages) {
			text.append(text(message)).append('\n');
		}
		return text.toString();
	}

	private static String text(ChatMessage message) {
		if (message instanceof UserMessage) {
			return ((UserMessage) message).text();
		}
		if (message instanceof SystemMessage) {
			return ((SystemMessage) message).text();
		}
		if (message instanceof AiMessage) {
			return ((AiMessage) message).text();
		}
		return String.valueOf(message);
	}
}
