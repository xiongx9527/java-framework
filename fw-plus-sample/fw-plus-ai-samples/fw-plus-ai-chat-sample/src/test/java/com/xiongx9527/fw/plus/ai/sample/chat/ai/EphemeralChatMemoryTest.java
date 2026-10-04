package com.xiongx9527.fw.plus.ai.sample.chat.ai;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.EphemeralChatMemory;
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
 * 本轮的工具往返在「本轮未收尾」时照常带给模型，收尾后一律不再带出。
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
	void neverEmitsRagSegmentFromMemory() {
		EphemeralChatMemory memory = memory();

		String firstRag = "《杭州热门景点》杭州著名景点有西湖";
		memory.add(UserMessage.from("杭州的天气适合玩什么" + EphemeralChatMemory.RAG_SEPARATOR + firstRag));
		memory.add(AiMessage.from("适合。"));

		String secondRag = "《未来一周天气》明天有雨";
		memory.add(UserMessage.from("那明天呢" + EphemeralChatMemory.RAG_SEPARATOR + secondRag));
		List<ChatMessage> messages = memory.messages();

		// 检索段只在请求里由框架补回（AssistantBuilder 已关掉 storeRetrievedContentInChatMemory），
		// 记忆侧两条用户消息都只剩原话
		assertEquals(Arrays.asList(ChatMessageType.USER, ChatMessageType.USER), typesOf(messages));
		assertEquals("杭州的天气适合玩什么", ((UserMessage) messages.get(0)).singleText());
		assertEquals("那明天呢", ((UserMessage) messages.get(1)).singleText());
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
		assertEquals("北京天气怎么样", ((UserMessage) messages.get(1)).singleText());
	}

	@Test
	void keepsToolRoundTripOfUnfinishedRound() {
		EphemeralChatMemory memory = memory();

		memory.add(UserMessage.from("第一个问题"));
		memory.add(toolCall("北京"));
		memory.add(ToolExecutionResultMessage.from("c1", "queryWeather", "32"));
		memory.add(AiMessage.from("第一个回答"));

		memory.add(UserMessage.from("第二个问题"));
		memory.add(toolCall("杭州"));
		memory.add(ToolExecutionResultMessage.from("c2", "queryWeather", "29"));

		// 本轮尚未收尾（还没有最终回复），工具往返随请求带出；上一轮的往返已被剔除
		assertEquals(Arrays.asList(
				ChatMessageType.USER,
				ChatMessageType.USER,
				ChatMessageType.AI,
				ChatMessageType.TOOL_EXECUTION_RESULT), typesOf(memory.messages()));
	}

	@Test
	void dropsToolRoundTripOnceRoundFinished() {
		EphemeralChatMemory memory = memory();

		memory.add(UserMessage.from("第一个问题"));
		memory.add(toolCall("北京"));
		memory.add(ToolExecutionResultMessage.from("c1", "queryWeather", "32"));
		memory.add(AiMessage.from("第一个回答"));

		memory.add(UserMessage.from("第二个问题"));
		memory.add(toolCall("杭州"));
		memory.add(ToolExecutionResultMessage.from("c2", "queryWeather", "29"));
		memory.add(AiMessage.from("第二个回答"));

		// 上一轮已收尾：新一轮的首个请求不能带上上一轮的工具往返，
		// 否则模型看到现成结果就直接作答、不再调用工具
		assertEquals(Arrays.asList(
				ChatMessageType.USER,
				ChatMessageType.USER), typesOf(memory.messages()));
	}

	@Test
	void treatsHalfFinishedToolRoundTripAsFinished() {
		EphemeralChatMemory memory = memory();

		// 工具抛错提前中断：工具请求与结果数对不上，不算「进行中」
		memory.add(UserMessage.from("第一个问题"));
		AiMessage twoCalls = AiMessage.from(Arrays.asList(
				ToolExecutionRequest.builder().id("r1").name("queryWeather").arguments("{\"city\":\"北京\"}").build(),
				ToolExecutionRequest.builder().id("r2").name("queryWeather").arguments("{\"city\":\"杭州\"}").build()));
		memory.add(twoCalls);
		memory.add(ToolExecutionResultMessage.from("r1", "queryWeather", "32"));

		memory.add(UserMessage.from("第二个问题"));

		assertEquals(Arrays.asList(
				ChatMessageType.USER,
				ChatMessageType.USER), typesOf(memory.messages()));
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
		assertEquals("普通消息", ((UserMessage) messages.get(1)).singleText());
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