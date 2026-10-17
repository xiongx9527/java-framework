package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope;

import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.EphemeralChatMemory;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 会话记忆契约测试：读历史快照（只读、不含系统提示）、轮次结束写回「用户原文 + 结果摘要」、
 * 失败轮次写失败说明且校验错误原文永不进历史、按窗口条数截断，并且直写对既有 AiServices 通道可见。
 */
class EnvelopeSessionMemoryTest {

	private static final String MEMORY_ID = "session-1";

	private final ChatMemoryStore store = new InMemoryChatMemoryStore();

	private EnvelopeSessionMemory memory(int window) {
		return new EnvelopeSessionMemory(store, window);
	}

	private static ChatEnvelope structured(String business, String json) {
		return ChatEnvelope.structured(business, JSONObject.parseObject(json));
	}

	/** 既有 AiServices 通道的记忆形态：MessageWindowChatMemory + 同一个 store */
	private ChatMemory existingWindowMemory(String memoryId) {
		return MessageWindowChatMemory.builder()
				.id(memoryId)
				.maxMessages(20)
				.chatMemoryStore(store)
				.build();
	}

	// -------------------------------------------------- 读历史快照

	@Test
	void snapshotOfUnknownSessionIsEmpty() {
		assertThat(memory(20).snapshot("never-seen")).isEmpty();
	}

	@Test
	void snapshotSeesDirectStoreWrites() {
		// 直写 store 必须立刻能读到（既有通道写进来的历史也要能看到）
		store.updateMessages(MEMORY_ID, Arrays.asList(UserMessage.from("历史原话")));

		List<ChatMessage> snapshot = memory(20).snapshot(MEMORY_ID);

		assertThat(snapshot).hasSize(1);
		assertThat(((UserMessage) snapshot.get(0)).singleText()).isEqualTo("历史原话");
	}

	@Test
	void snapshotHidesSystemMessages() {
		// 结构化轮次自带系统提示，历史里的系统提示不参与拼接
		store.updateMessages(MEMORY_ID, Arrays.asList(SystemMessage.from("你是助手"), UserMessage.from("查订单")));

		List<ChatMessage> snapshot = memory(20).snapshot(MEMORY_ID);

		assertThat(snapshot).hasSize(1);
		assertThat(snapshot.get(0).type()).isEqualTo(ChatMessageType.USER);
	}

	@Test
	void snapshotKeepsUserAndAiOrder() {
		EnvelopeSessionMemory memory = memory(20);
		memory.recordTurn(MEMORY_ID, "第一轮", structured("order", "{\"orderId\":\"A1\"}"));
		memory.recordTurn(MEMORY_ID, "第二轮", structured("refund", "{\"refundId\":\"R1\"}"));

		List<ChatMessage> snapshot = memory.snapshot(MEMORY_ID);

		assertThat(snapshot).hasSize(4);
		assertThat(snapshot.get(0).type()).isEqualTo(ChatMessageType.USER);
		assertThat(((UserMessage) snapshot.get(0)).singleText()).isEqualTo("第一轮");
		assertThat(snapshot.get(1).type()).isEqualTo(ChatMessageType.AI);
		assertThat(((UserMessage) snapshot.get(2)).singleText()).isEqualTo("第二轮");
		assertThat(snapshot.get(3).type()).isEqualTo(ChatMessageType.AI);
	}

	@Test
	void snapshotIsReadOnly() {
		EnvelopeSessionMemory memory = memory(20);
		memory.recordTurn(MEMORY_ID, "第一轮", structured("order", "{\"orderId\":\"A1\"}"));

		List<ChatMessage> snapshot = memory.snapshot(MEMORY_ID);

		assertThatThrownBy(() -> snapshot.add(UserMessage.from("脏写")))
				.isInstanceOf(UnsupportedOperationException.class);
		assertThat(memory.snapshot(MEMORY_ID)).hasSize(2);
	}

	// -------------------------------------------------- 轮次写回

	@Test
	void recordTurnAppendsUserTextAndSummary() {
		EnvelopeSessionMemory memory = memory(20);

		memory.recordTurn(MEMORY_ID, "帮我查订单 A1", structured("order", "{\"orderId\":\"A1\"}"));

		List<ChatMessage> snapshot = memory.snapshot(MEMORY_ID);
		assertThat(snapshot).hasSize(2);
		assertThat(((UserMessage) snapshot.get(0)).singleText()).isEqualTo("帮我查订单 A1");
		String summary = ((AiMessage) snapshot.get(1)).text();
		assertThat(summary).contains("order").contains("A1");
	}

	@Test
	void failureTurnRecordsFailureNarrative() {
		EnvelopeSessionMemory memory = memory(20);

		memory.recordTurn(MEMORY_ID, "帮我查订单 A1", ChatEnvelope.failure("order", ChatEnvelope.DEFAULT_FAILURE_TEXT));

		List<ChatMessage> snapshot = memory.snapshot(MEMORY_ID);
		assertThat(((AiMessage) snapshot.get(1)).text()).isEqualTo(ChatEnvelope.DEFAULT_FAILURE_TEXT);
	}

	@Test
	void validationErrorsNeverEnterHistory() {
		// 失败轮次只写失败说明：模型原文与校验错误清单一律不进历史
		EnvelopeSessionMemory memory = memory(20);

		memory.recordTurn(MEMORY_ID, "帮我查订单 A1", ChatEnvelope.failure("order", ChatEnvelope.DEFAULT_FAILURE_TEXT));

		List<ChatMessage> snapshot = memory.snapshot(MEMORY_ID);
		for (ChatMessage message : snapshot) {
			assertThat(text(message)).doesNotContain("required").doesNotContain("items").doesNotContain("schema");
		}
	}

	@Test
	void blankSummaryWritesOnlyTheUserText() {
		EnvelopeSessionMemory memory = memory(20);

		memory.recordTurn(MEMORY_ID, "只有原文", null);

		List<ChatMessage> snapshot = memory.snapshot(MEMORY_ID);
		assertThat(snapshot).hasSize(1);
		assertThat(snapshot.get(0).type()).isEqualTo(ChatMessageType.USER);
	}

	@Test
	void blankMemoryIdIsNoOp() {
		EnvelopeSessionMemory memory = memory(20);

		memory.recordTurn(null, "帮我查订单 A1", structured("order", "{\"orderId\":\"A1\"}"));
		memory.recordTurn("   ", "帮我查订单 A1", structured("order", "{\"orderId\":\"A1\"}"));

		assertThat(memory.snapshot(MEMORY_ID)).isEmpty();
		assertThat(store.getMessages("   ")).isEmpty();
	}

	@Test
	void windowTrimsOldTurns() {
		// 窗口按条数截断：每次写回都会把超出窗口的旧消息挤掉
		EnvelopeSessionMemory memory = memory(2);

		memory.recordTurn(MEMORY_ID, "第一轮", structured("order", "{\"orderId\":\"A1\"}"));
		memory.recordTurn(MEMORY_ID, "第二轮", structured("refund", "{\"refundId\":\"R1\"}"));

		List<ChatMessage> snapshot = memory.snapshot(MEMORY_ID);
		assertThat(snapshot).hasSize(2);
		assertThat(((UserMessage) snapshot.get(0)).singleText()).isEqualTo("第二轮");
	}

	@Test
	void windowComesFromConfigurationDefaults() {
		// 默认窗口（配置默认 20）足以容纳多轮
		int window = new Langchain4jAssistantProperties().getSession().getWindowSize();
		EnvelopeSessionMemory memory = memory(window);

		memory.recordTurn(MEMORY_ID, "第一轮", structured("order", "{\"orderId\":\"A1\"}"));
		memory.recordTurn(MEMORY_ID, "第二轮", structured("order", "{\"orderId\":\"A2\"}"));

		assertThat(memory.snapshot(MEMORY_ID)).hasSize(4);
	}

	// -------------------------------------------------- 摘要内容

	@Test
	void summaryOfStructuredSuccessCarriesBusinessAndResult() {
		String summary = EnvelopeSessionMemory.summarize(structured("order", "{\"orderId\":\"A1\"}"));

		assertThat(summary).contains("order").contains("A1");
	}

	@Test
	void summaryOfFailureIsTheFailureNarrative() {
		assertThat(EnvelopeSessionMemory.summarize(ChatEnvelope.failure("order", ChatEnvelope.DEFAULT_FAILURE_TEXT)))
				.isEqualTo(ChatEnvelope.DEFAULT_FAILURE_TEXT);
	}

	@Test
	void summaryOfMissingEnvelopeIsEmpty() {
		// 本轮没有产出任何结果：没有可写回的摘要（只写用户原文）
		assertThat(EnvelopeSessionMemory.summarize(null)).isEmpty();
	}

	@Test
	void summaryOfChatRoundIsTheReplyText() {
		assertThat(EnvelopeSessionMemory.summarize(ChatEnvelope.chat("chat", "你好呀"))).isEqualTo("你好呀");
	}

	@Test
	void summaryIsBoundedForLongResults() {
		StringBuilder longText = new StringBuilder();
		for (int i = 0; i < 2000; i++) {
			longText.append('x');
		}
		ChatEnvelope envelope = ChatEnvelope.structured("order",
				JSONObject.parseObject("{\"orderId\":\"" + longText + "\"}"));

		String summary = EnvelopeSessionMemory.summarize(envelope);

		assertThat(summary.length()).isLessThanOrEqualTo(EnvelopeSessionMemory.MAX_SUMMARY_LENGTH);
		assertThat(summary).contains("order");
	}

	@Test
	void summaryIsBoundedForLongChatText() {
		StringBuilder longText = new StringBuilder();
		for (int i = 0; i < 2000; i++) {
			longText.append('y');
		}

		String summary = EnvelopeSessionMemory.summarize(ChatEnvelope.chat("chat", longText.toString()));

		assertThat(summary.length()).isLessThanOrEqualTo(EnvelopeSessionMemory.MAX_SUMMARY_LENGTH);
	}

	// -------------------------------------------------- 与既有通道共用一份历史

	@Test
	void structuredWriteBackIsVisibleToExistingChatMemory() {
		EnvelopeSessionMemory memory = memory(20);
		ChatMemory existing = existingWindowMemory(MEMORY_ID);

		memory.recordTurn(MEMORY_ID, "帮我查订单 A1", structured("order", "{\"orderId\":\"A1\"}"));

		List<ChatMessage> seen = existing.messages();
		assertThat(seen).hasSize(2);
		assertThat(((UserMessage) seen.get(0)).singleText()).isEqualTo("帮我查订单 A1");
		assertThat(((AiMessage) seen.get(1)).text()).contains("A1");
	}

	@Test
	void ephemeralChatMemoryReadPathSeesStructuredWriteBack() {
		// 既有 AiServices 通道用的是 EphemeralChatMemory(MessageWindowChatMemory, store)：其 messages()
		// 每次都从 store 重新加载。langchain4j 1.x 一轮内先读 messages()（此时 store 尚无本轮用户消息）、
		// 再写本轮 UserMessage：结构化轮次的直写因此在下一轮「读请求上下文」时可见——用户原文与结果
		// 摘要先被固化为持久历史再进入请求
		ChatMemory delegate = existingWindowMemory(MEMORY_ID);
		ChatMemory ephemeral = EphemeralChatMemory.builder(delegate, store).build();
		EnvelopeSessionMemory memory = memory(20);

		memory.recordTurn(MEMORY_ID, "帮我查订单 A1", structured("order", "{\"orderId\":\"A1\"}"));

		// 下一轮开始：先读本轮请求上下文（不含本轮用户消息），结构化轮的原文与摘要可见
		List<ChatMessage> seen = ephemeral.messages();
		assertThat(seen).hasSize(2);
		assertThat(((UserMessage) seen.get(0)).singleText()).isEqualTo("帮我查订单 A1");
		assertThat(((AiMessage) seen.get(1)).text()).contains("A1");

		// 随后本轮 UserMessage 到达：追加进共享历史，结构化轮（原文 + 摘要）与本轮用户消息同在
		ephemeral.add(UserMessage.from("那第二单呢"));
		List<ChatMessage> stored = store.getMessages(MEMORY_ID);
		assertThat(stored).hasSize(3);
		assertThat(((UserMessage) stored.get(0)).singleText()).isEqualTo("帮我查订单 A1");
		assertThat(((AiMessage) stored.get(1)).text()).contains("A1");
		assertThat(((UserMessage) stored.get(2)).singleText()).isEqualTo("那第二单呢");
	}

	@Test
	void existingChatTurnIsVisibleToTheNextStructuredRound() {
		// 反向也要成立：闲聊轮次经既有通道写入的历史，结构化轮次要能读到（同一会话共用一份历史）
		ChatMemory existing = existingWindowMemory(MEMORY_ID);
		existing.add(UserMessage.from("随便聊聊"));
		existing.add(AiMessage.from("你好呀"));
		EnvelopeSessionMemory memory = memory(20);

		memory.recordTurn(MEMORY_ID, "帮我查订单 A1", structured("order", "{\"orderId\":\"A1\"}"));

		List<ChatMessage> snapshot = memory.snapshot(MEMORY_ID);
		assertThat(snapshot).hasSize(4);
		assertThat(((UserMessage) snapshot.get(0)).singleText()).isEqualTo("随便聊聊");
		assertThat(((UserMessage) snapshot.get(2)).singleText()).isEqualTo("帮我查订单 A1");
	}

	// -------------------------------------------------- 构造约束

	@Test
	void nullStoreIsRejected() {
		assertThatThrownBy(() -> new EnvelopeSessionMemory(null, 20))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void invalidWindowIsRejected() {
		assertThatThrownBy(() -> new EnvelopeSessionMemory(store, 0))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new EnvelopeSessionMemory(store, -1))
				.isInstanceOf(IllegalArgumentException.class);
	}

	private static String text(ChatMessage message) {
		if (message instanceof UserMessage) {
			return ((UserMessage) message).singleText();
		}
		if (message instanceof AiMessage) {
			return ((AiMessage) message).text();
		}
		return String.valueOf(message);
	}
}
