package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.AssistantBuilder;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.SimpleAssistant;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
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
 * 非流式信封入口：入口只接收会话标识与用户消息；结构化轮次不触碰既有通道；
 * 非结构化轮次委托既有通道后收敛；任何失败都以 {@code ok=false} 收场而不是抛异常。
 */
class EnvelopeAssistantTest {

	private static final String MEMORY_ID = "session-1";

	private static final String ORDER_SCHEMA = "{"
			+ "\"type\":\"object\","
			+ "\"properties\":{\"orderId\":{\"type\":\"string\"}},"
			+ "\"required\":[\"orderId\"]}";

	private final ChatMemoryStore store = new InMemoryChatMemoryStore();

	// -------------------------------------------------- 结构化轮次

	@Test
	void structuredRoundDoesNotTouchExistingChannel() {
		RecordingChatCaller caller = new RecordingChatCaller();
		EnvelopeAssistant entry = entry(new ScriptedModel(classifiedAs("order"), "{\"orderId\":\"A1\"}"),
				caller, order());

		ChatEnvelope envelope = entry.chat(MEMORY_ID, "帮我查订单 A1");

		assertThat(envelope.isOk()).isTrue();
		assertThat(envelope.getBusiness()).isEqualTo("order");
		assertThat(envelope.getResult().getString("orderId")).isEqualTo("A1");
		assertThat(envelope.getText()).isEmpty();
		assertThat(caller.calls).isEmpty();
	}

	@Test
	void structuredRoundRetriesAndFailsIntoEnvelope() {
		// 首轮校验失败、重试仍失败：入口给 ok=false 信封，不抛异常
		EnvelopeAssistant entry = entry(new ScriptedModel(classifiedAs("order"), "{\"orderId\":1}", "{\"orderId\":2}"),
				new RecordingChatCaller(), order());

		ChatEnvelope envelope = entry.chat(MEMORY_ID, "帮我查订单 A1");

		assertThat(envelope.isOk()).isFalse();
		assertThat(envelope.getBusiness()).isEqualTo("order");
		assertThat(envelope.getResult()).isNull();
		assertThat(envelope.getText()).isEqualTo(ChatEnvelope.DEFAULT_FAILURE_TEXT);
	}

	// -------------------------------------------------- 单次调用覆盖（三层覆盖顺序）

	@Test
	void callOverrideOffSuppressesStructuredEvenWhenContractEnablesIt() {
		// 规格「单次调用覆盖业务契约」：契约开了结构化开关，本次调用显式关闭 → 本次不产出 result
		RecordingChatCaller caller = new RecordingChatCaller();
		caller.reply = "这次先不用结构化";
		EnvelopeAssistant entry = entry(new ScriptedModel(classifiedAs("order")), caller, order());

		ChatEnvelope envelope = entry.chat(MEMORY_ID, "帮我查订单 A1", Boolean.FALSE);

		assertThat(envelope.isOk()).isTrue();
		assertThat(envelope.getBusiness()).isEqualTo("order");
		assertThat(envelope.getResult()).isNull();
		assertThat(envelope.getText()).isEqualTo("这次先不用结构化");
		assertThat(caller.calls).containsExactly("session-1|帮我查订单 A1");
	}

	@Test
	void callOverrideOnForcesStructuredWhenContractOptsOut() {
		// 覆盖顺序：单次调用 > 业务契约（契约关闭、本次调用开启 → 仍产出 result）
		EnvelopeAssistant entry = entry(new ScriptedModel(classifiedAs("refund"), "{\"orderId\":\"R9\"}"),
				new RecordingChatCaller(), refund());

		ChatEnvelope envelope = entry.chat(MEMORY_ID, "我要退款", Boolean.TRUE);

		assertThat(envelope.isOk()).isTrue();
		assertThat(envelope.getBusiness()).isEqualTo("refund");
		assertThat(envelope.getResult().getString("orderId")).isEqualTo("R9");
		assertThat(envelope.getText()).isEmpty();
	}

	// -------------------------------------------------- 非结构化轮次

	@Test
	void chatRoundDelegatesToExistingChannelAndWrapsText() {
		RecordingChatCaller caller = new RecordingChatCaller();
		caller.reply = "今天天气不错";
		EnvelopeAssistant entry = entry(new ScriptedModel(classifiedAs("chat")), caller, order());

		ChatEnvelope envelope = entry.chat(MEMORY_ID, "随便聊聊");

		assertThat(envelope.isOk()).isTrue();
		assertThat(envelope.getBusiness()).isEqualTo(BusinessRegistry.FALLBACK);
		assertThat(envelope.getResult()).isNull();
		assertThat(envelope.getText()).isEqualTo("今天天气不错");
		assertThat(caller.calls).containsExactly("session-1|随便聊聊");
		assertThat(store.getMessages(MEMORY_ID)).isEmpty();
	}

	@Test
	void chatRoundKeepsBusinessOfContractWithoutStructuredResult() {
		RecordingChatCaller caller = new RecordingChatCaller();
		caller.reply = "请提供订单号";
		EnvelopeAssistant entry = entry(new ScriptedModel(classifiedAs("refund")), caller, refund());

		ChatEnvelope envelope = entry.chat(MEMORY_ID, "我要退款");

		assertThat(envelope.getBusiness()).isEqualTo("refund");
		assertThat(envelope.isOk()).isTrue();
		assertThat(envelope.getResult()).isNull();
		assertThat(caller.calls).hasSize(1);
	}

	@Test
	void chatRoundBlankReplyBecomesFailureEnvelope() {
		RecordingChatCaller caller = new RecordingChatCaller();
		caller.reply = "";
		EnvelopeAssistant entry = entry(new ScriptedModel(classifiedAs("chat")), caller, order());

		ChatEnvelope envelope = entry.chat(MEMORY_ID, "随便聊聊");

		assertThat(envelope.isOk()).isFalse();
		assertThat(envelope.getText()).isEqualTo(ChatEnvelope.DEFAULT_FAILURE_TEXT);
	}

	@Test
	void chatRoundFailureBecomesFailureEnvelopeInsteadOfThrowing() {
		RecordingChatCaller caller = new RecordingChatCaller();
		caller.failure = new IllegalStateException("模型网关超时");
		EnvelopeAssistant entry = entry(new ScriptedModel(classifiedAs("chat")), caller, order());

		ChatEnvelope envelope = entry.chat(MEMORY_ID, "随便聊聊");

		assertThat(envelope.isOk()).isFalse();
		assertThat(envelope.getBusiness()).isEqualTo(BusinessRegistry.FALLBACK);
		assertThat(envelope.getText()).isEqualTo(ChatEnvelope.DEFAULT_FAILURE_TEXT);
	}

	// -------------------------------------------------- 既有通道便利构造

	@Test
	void convenienceConstructorDelegatesChatRoundToSimpleAssistant() {
		ScriptedModel model = new ScriptedModel("你好呀");
		SimpleAssistant assistant = new AssistantBuilder().chatModel(model).memoryStore(store).build();
		EnvelopeAssistant entry = new EnvelopeAssistant(runtime(model), assistant);

		ChatEnvelope envelope = entry.chat(MEMORY_ID, "随便聊聊");

		assertThat(envelope.isOk()).isTrue();
		assertThat(envelope.getText()).isEqualTo("你好呀");
		assertThat(envelope.getBusiness()).isEqualTo(BusinessRegistry.FALLBACK);
	}

	// -------------------------------------------------- 构造约束

	@Test
	void nullArgumentsAreRejected() {
		EnvelopeRuntime runtime = runtime(new ScriptedModel());

		assertThatThrownBy(() -> new EnvelopeAssistant(null, new RecordingChatCaller()))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new EnvelopeAssistant(runtime, (EnvelopeAssistant.ChatCaller) null))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new EnvelopeAssistant(runtime, (SimpleAssistant) null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	// -------------------------------------------------- 装配工具

	private EnvelopeRuntime runtime(ChatLanguageModel model, BusinessContract... contracts) {
		BusinessRegistry registry = new BusinessRegistry();
		for (BusinessContract contract : contracts) {
			registry.register(contract);
		}
		return new EnvelopeRuntime(model, store, registry, new Langchain4jAssistantProperties());
	}

	private EnvelopeAssistant entry(ChatLanguageModel model, EnvelopeAssistant.ChatCaller caller,
			BusinessContract... contracts) {
		return new EnvelopeAssistant(runtime(model, contracts), caller);
	}

	private static BusinessContract order() {
		return BusinessContract.builder("order").schema(ORDER_SCHEMA).structured(true).build();
	}

	private static BusinessContract refund() {
		return BusinessContract.builder("refund").schema(ORDER_SCHEMA).structured(false).build();
	}

	private static String classifiedAs(String business) {
		return "{\"business\":\"" + business + "\"}";
	}

	/** 既有通道替身：记录调用，可配置回复与失败 */
	private static final class RecordingChatCaller implements EnvelopeAssistant.ChatCaller {

		private final List<String> calls = new ArrayList<String>();

		private String reply = "";

		private RuntimeException failure;

		@Override
		public String chat(String memoryId, String message) {
			calls.add(memoryId + "|" + message);
			if (failure != null) {
				throw failure;
			}
			return reply;
		}
	}

	/** 按脚本依次返回回复的假模型 */
	private static final class ScriptedModel implements ChatLanguageModel {

		private final Deque<String> replies;

		private ScriptedModel(String... replies) {
			this.replies = new ArrayDeque<String>(Arrays.asList(replies));
		}

		@Override
		public Response<AiMessage> generate(List<ChatMessage> messages) {
			return Response.from(AiMessage.from(replies.isEmpty() ? "" : replies.poll()));
		}
	}
}
