package com.xiongx9527.fw.plus.ai.sample.chat.ai;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link EphemeralChatMemory} 的行为锁定：历史只沉淀系统提示与用户原话，
 * 本轮的检索段与工具往返照常带给模型。
 *
 * <p>多轮用例复用<b>同一个实例</b>——真实链路里框架只调用一次 {@code ChatMemoryProvider.get}
 * （{@code AiServiceContext.chatMemories} 的 {@code computeIfAbsent}），实例不随请求重建；
 * 用「每轮新建 wrapper」来模拟轮次会绕开这个语义。</p>
 */
class EphemeralChatMemoryTest {

	private final InMemoryChatMemoryStore store = new InMemoryChatMemoryStore();

	private EphemeralChatMemory memory() {
		MessageWindowChatMemory delegate = MessageWindowChatMemory.builder()
				.id("s-1")
				.maxMessages(20)
				.chatMemoryStore(store)
				.build();
		return new EphemeralChatMemory(delegate, store);
	}

	private static AiMessage toolCall(String city) {
		return AiMessage.from(ToolExecutionRequest.builder()
				.name("queryWeather").arguments("{\"city\":\"" + city + "\"}").build());
	}

	private static List<ChatMessageType> typesOf(List<ChatMessage> messages) {
		List<ChatMessageType> types = new ArrayList<>();
		for (ChatMessage message : messages) {
			types.add(message.type());
		}
		return types;
	}

	@Test
	void keepsOrderOfMultipleToolRoundTrips() {
		EphemeralChatMemory memory = memory();

		memory.add(UserMessage.from("比较北京和杭州"));
		memory.add(toolCall("北京"));
		memory.add(ToolExecutionResultMessage.from("c1", "queryWeather", "32"));
		memory.add(toolCall("杭州"));
		memory.add(ToolExecutionResultMessage.from("c2", "queryWeather", "29"));

		// 本轮 user 之后紧跟两组「call → result」，顺序不倒
		assertEquals(Arrays.asList(
				ChatMessageType.USER,
				ChatMessageType.AI,
				ChatMessageType.TOOL_EXECUTION_RESULT,
				ChatMessageType.AI,
				ChatMessageType.TOOL_EXECUTION_RESULT), typesOf(memory.messages()));
	}

	@Test
	void stripsRagFromHistoryButKeepsItInCurrentRequest() {
		EphemeralChatMemory memory = memory();

		String firstRag = "《杭州热门景点》杭州著名景点有西湖";
		memory.add(UserMessage.from("杭州的天气适合玩什么" + RagContentInjector.SEPARATOR + firstRag));
		memory.add(AiMessage.from("适合。"));

		// 第二轮：新检索内容照常带给模型
		String secondRag = "《未来一周天气》明天有雨";
		memory.add(UserMessage.from("那明天呢" + RagContentInjector.SEPARATOR + secondRag));
		List<ChatMessage> messages = memory.messages();

		// 历史那条只剩原话，本轮那条带新检索内容
		assertEquals("杭州的天气适合玩什么", ((UserMessage) messages.get(0)).text());
		String current = ((UserMessage) messages.get(1)).text();
		assertTrue(current.contains("那明天呢") && current.contains(secondRag), current);
	}

	@Test
	void historyKeepsOnlySystemAndUserMessages() {
		EphemeralChatMemory memory = memory();

		memory.add(SystemMessage.from("系统提示"));
		memory.add(UserMessage.from("北京天气怎么样"));
		memory.add(toolCall("北京"));
		memory.add(ToolExecutionResultMessage.from("c1", "queryWeather", "32"));
		memory.add(AiMessage.from("北京当前 32℃，注意防晒。"));

		// 第二轮：框架每轮都会重新产出系统提示与用户消息
		memory.add(SystemMessage.from("系统提示"));
		memory.add(UserMessage.from("再问一次"));

		List<ChatMessage> messages = memory.messages();

		// 系统提示按内容去重；工具往返与最终回答都不留
		assertEquals(Arrays.asList(
				ChatMessageType.SYSTEM,
				ChatMessageType.USER,
				ChatMessageType.USER), typesOf(messages));
		assertEquals("北京天气怎么样", ((UserMessage) messages.get(1)).text());
	}

	@Test
	void onlyLastRoundGetsToolMessagesReattached() {
		EphemeralChatMemory memory = memory();

		memory.add(UserMessage.from("第一个问题"));
		memory.add(toolCall("北京"));
		memory.add(ToolExecutionResultMessage.from("c1", "queryWeather", "32"));
		memory.add(AiMessage.from("第一个回答"));

		memory.add(UserMessage.from("第二个问题"));
		memory.add(toolCall("杭州"));
		memory.add(ToolExecutionResultMessage.from("c2", "queryWeather", "29"));
		memory.add(AiMessage.from("第二个回答"));

		List<ChatMessage> messages = memory.messages();

		// 只有本轮的 tool 对留下；上一轮的工具往返与回答都不在
		assertEquals(Arrays.asList(
				ChatMessageType.USER,
				ChatMessageType.USER,
				ChatMessageType.AI,                     // 本轮 tool call
				ChatMessageType.TOOL_EXECUTION_RESULT), typesOf(messages));
	}

	@Test
	void dropsPlainAiReplyFromHistory() {
		EphemeralChatMemory memory = memory();

		memory.add(SystemMessage.from("系统提示"));
		memory.add(UserMessage.from("普通消息"));
		memory.add(AiMessage.from("普通回复"));

		memory.add(UserMessage.from("下一个问题"));
		List<ChatMessage> messages = memory.messages();

		assertEquals(Arrays.asList(
				ChatMessageType.SYSTEM,
				ChatMessageType.USER,
				ChatMessageType.USER), typesOf(messages));
		assertEquals("普通消息", ((UserMessage) messages.get(1)).text());
	}

	@Test
	void clearDropsEverything() {
		EphemeralChatMemory memory = memory();

		memory.add(UserMessage.from("北京天气怎么样"));
		memory.add(toolCall("北京"));
		memory.add(ToolExecutionResultMessage.from("c1", "queryWeather", "32"));
		memory.clear();

		assertTrue(memory.messages().isEmpty());
	}
}
