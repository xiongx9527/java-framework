package com.xiongx9527.fw.plus.ai.sample.chat.ai;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.Assistant;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.AssistantBuilder;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.EphemeralChatMemory;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 真实 {@code AiServices} 链路上的记忆行为回归。
 *
 * <p>用可编排的假模型跑通「模型请求工具 → 框架执行 → 模型收尾」的完整循环。存在的理由：
 * {@link EphemeralChatMemoryTest} 直接驱动 wrapper，绕开了 langchain4j 自己拼装请求消息的
 * 顺序——而顺序正是这两个坑的来源：</p>
 * <ul>
 *   <li>框架每轮先读 {@code chatMemory.messages()} 取历史，<b>之后</b>才把本轮用户消息写进记忆。
 *       所以「上一轮过程数据的清理」不能只靠 {@code add(UserMessage)}，否则上一轮的工具往返
 *       会随历史进入本轮首个请求；</li>
 *   <li>检索增强后的用户消息由框架接到请求末尾，记忆里只应留用户原话。</li>
 * </ul>
 */
class EphemeralChatMemoryIntegrationTest {

	private static final String SESSION = "s-1";

	@Test
	void secondTurnDoesNotSeeAnyToolDerivedOutputSoModelRetools() {
		WeatherTools weatherTools = new WeatherTools();
		RecordingChatModel model = new RecordingChatModel().reply(
				// 第一轮：先请求工具，拿到结果后收尾
				AiMessage.from(ToolExecutionRequest.builder()
						.name("queryWeather").arguments("{\"city\":\"北京\"}").build()),
				AiMessage.from("北京当前 32℃。"),
				// 第二轮：再次请求工具
				AiMessage.from(ToolExecutionRequest.builder()
						.name("queryWeather").arguments("{\"city\":\"北京\"}").build()),
				// 第二轮收尾
				AiMessage.from("北京当前 31℃。"));

		// 经框架 AssistantBuilder 构建：默认策略 PERSIST_NON_TOOL_FINAL_ANSWER——
		// 上一轮调过工具，工具往返与工具派生的最终回答整轮都不沉淀
		Assistant assistant = new AssistantBuilder()
				.chatModel(model)
				.memoryStore(new InMemoryChatMemoryStore())
				.tools(weatherTools)
				.toAssistant();

		assistant.chat(SESSION, "", "北京天气怎么样");
		assistant.chat(SESSION, "", "北京天气怎么样");

		assertEquals(2, weatherTools.getCallCount(), "第二轮应重新调用工具而不是复用上一轮结果");
		assertEquals(Arrays.asList(ChatMessageType.USER, ChatMessageType.USER),
				typesOf(model.requests().get(2)),
				"第二轮请求不带第一轮的工具往返，也不带工具派生的最终回答");
	}

	/**
	 * 未调用工具的轮次仍沉淀最终回答——上下文连续性不受影响。
	 */
	@Test
	void nonToolTurnKeepsFinalAnswerForNextRound() {
		WeatherTools weatherTools = new WeatherTools();
		RecordingChatModel model = new RecordingChatModel().reply(
				AiMessage.from("西湖是杭州著名景点。"),
				AiMessage.from("西湖位于杭州西面。"));

		Assistant assistant = new AssistantBuilder()
				.chatModel(model)
				.memoryStore(new InMemoryChatMemoryStore())
				.tools(weatherTools)
				.toAssistant();

		assistant.chat(SESSION, "", "介绍一下西湖");
		assistant.chat(SESSION, "", "它在哪里");

		assertEquals(0, weatherTools.getCallCount(), "本例不触发工具");
		assertEquals(Arrays.asList(ChatMessageType.USER, ChatMessageType.AI, ChatMessageType.USER),
				typesOf(model.requests().get(1)),
				"未调工具的上一轮结论要带到下一轮");
	}

	@Test
	void toolContinuationKeepsSingleAugmentedUserMessage() {
		String question = "杭州有什么著名景点";
		String rag = "杭州著名景点有西湖、灵隐寺、西溪湿地";
		RecordingChatModel model = new RecordingChatModel().reply(
				AiMessage.from(Arrays.asList(
						ToolExecutionRequest.builder().id("weather-hangzhou")
								.name("queryWeather").arguments("{\"city\":\"杭州\"}").build(),
						ToolExecutionRequest.builder().id("weather-beijing")
								.name("queryWeather").arguments("{\"city\":\"北京\"}").build())),
				AiMessage.from("杭州有西湖。"));

		Assistant assistant = new AssistantBuilder()
				.chatModel(model)
				.memoryStore(new InMemoryChatMemoryStore())
				.systemMessage("系统提示")
				.tools(new WeatherTools())
				.contentRetriever(query -> Arrays.asList(Content.from(rag)))
				.toAssistant();

		assistant.chat(SESSION, "", question);
		assertEquals(2, model.requests().size(), "批量调用两个工具后只应产生一次续调模型请求");

		List<ChatMessage> firstRequest = model.requests().get(0);
		assertEquals(Arrays.asList(ChatMessageType.SYSTEM, ChatMessageType.USER), typesOf(firstRequest));
		assertTrue(textOf(firstRequest.get(1)).contains(rag), "首轮模型请求仍应包含 RAG");

		// 工具续调：框架把增强后的用户消息换回本轮提问所在的位置，RAG 全程只出现一次
		List<ChatMessage> followUpRequest = model.requests().get(1);
		assertEquals(Arrays.asList(ChatMessageType.SYSTEM, ChatMessageType.USER, ChatMessageType.AI,
				ChatMessageType.TOOL_EXECUTION_RESULT, ChatMessageType.TOOL_EXECUTION_RESULT),
				typesOf(followUpRequest), followUpRequest.toString());
		String continuedQuestion = textOf(followUpRequest.get(1));
		assertTrue(continuedQuestion.contains(question), continuedQuestion);
		assertTrue(continuedQuestion.contains(rag), "工具续调仍应包含本轮 RAG：" + continuedQuestion);
		assertEquals(1, countRagUserMessages(followUpRequest), "一条请求里只能有一条带 RAG 的 user");
	}

	@Test
	void ragSegmentIsNotPersistedInChatMemoryStore() {
		String question = "杭州有什么著名景点";
		String rag = "杭州著名景点有西湖、灵隐寺、西溪湿地";
		RecordingChatModel model = new RecordingChatModel().reply(
				AiMessage.from(ToolExecutionRequest.builder().id("w1")
						.name("queryWeather").arguments("{\"city\":\"杭州\"}").build()),
				AiMessage.from("杭州有西湖。"));
		InMemoryChatMemoryStore store = new InMemoryChatMemoryStore();

		Assistant assistant = new AssistantBuilder()
				.chatModel(model)
				.memoryStore(store)
				.systemMessage("系统提示")
				.tools(new WeatherTools())
				.contentRetriever(query -> Arrays.asList(Content.from(rag)))
				.toAssistant();

		assistant.chat(SESSION, "", question);

		List<ChatMessage> persisted = store.getMessages(SESSION);
		List<ChatMessageType> persistedTypes = typesOf(persisted);
		assertTrue(persistedTypes.get(0) == ChatMessageType.SYSTEM
				&& persistedTypes.get(1) == ChatMessageType.USER, persisted.toString());
		assertEquals(question, textOf(persisted.get(1)), "记忆里的用户消息应只剩原话：" + persisted);
		assertEquals(0, countRagUserMessages(persisted), "检索段不该写进 ChatMemoryStore：" + persisted);
	}

	@Test
	void repeatedQuestionStillTriggersToolCall() {
		WeatherTools weatherTools = new WeatherTools();
		RecordingChatModel model = new RecordingChatModel().reply(
				AiMessage.from(Arrays.asList(
						ToolExecutionRequest.builder().id("b1")
								.name("queryWeather").arguments("{\"city\":\"北京\"}").build(),
						ToolExecutionRequest.builder().id("h1")
								.name("queryWeather").arguments("{\"city\":\"杭州\"}").build())),
				AiMessage.from("北京 32℃，杭州 29℃。"),
				AiMessage.from(ToolExecutionRequest.builder().id("b2")
						.name("queryWeather").arguments("{\"city\":\"北京\"}").build()),
				AiMessage.from("北京 32℃。"));

		Assistant assistant = new AssistantBuilder()
				.chatModel(model)
				.memoryStore(new InMemoryChatMemoryStore())
				.tools(weatherTools)
				.contentRetriever(query -> Arrays.asList(Content.from("杭州有西湖；北京有故宫。")))
				.toAssistant();

		String question = "北京和杭州的天气怎么样";
		assistant.chat(SESSION, "", question);
		assistant.chat(SESSION, "", question);

		assertEquals(3, weatherTools.getCallCount(), "同一问题问第二次，模型应重新调用工具");
		List<ChatMessage> secondRoundFirstRequest = model.requests().get(2);
		assertEquals(Arrays.asList(ChatMessageType.USER, ChatMessageType.USER),
				typesOf(secondRoundFirstRequest),
				"第二轮首个请求应是「上一轮原话 + 本轮原话带 RAG」，不得带上上一轮的工具往返："
						+ secondRoundFirstRequest);
		assertEquals(question, textOf(secondRoundFirstRequest.get(0)));
		assertEquals(1, countRagUserMessages(secondRoundFirstRequest),
				"本轮 RAG 只出现一次，不能与框架追加的那条重复");
	}

	private static String textOf(ChatMessage message) {
		return ((UserMessage) message).singleText();
	}

	private static long countRagUserMessages(List<ChatMessage> messages) {
		return messages.stream()
				.filter(UserMessage.class::isInstance)
				.map(UserMessage.class::cast)
				.filter(user -> user.singleText().contains(EphemeralChatMemory.RAG_SEPARATOR))
				.count();
	}

	private static List<ChatMessageType> typesOf(List<ChatMessage> messages) {
		List<ChatMessageType> types = new ArrayList<>();
		for (ChatMessage message : messages) {
			types.add(message.type());
		}
		return types;
	}

	/**
	 * 可编排模型：记录每次收到的消息，按序回放预设回复。
	 *
	 * <p>通过 {@link ChatModel#doChat(ChatRequest)} 同时记录消息与工具请求。</p>
	 */
	private static class RecordingChatModel implements ChatModel {

		private final List<List<ChatMessage>> requests = new ArrayList<>();
		private final Deque<AiMessage> replies = new ArrayDeque<>();

		RecordingChatModel reply(AiMessage... messages) {
			replies.addAll(Arrays.asList(messages));
			return this;
		}

		List<List<ChatMessage>> requests() {
			return requests;
		}

		@Override
		public ChatResponse doChat(ChatRequest request) {
			requests.add(new ArrayList<>(request.messages()));
			AiMessage reply = replies.poll();
			return ChatResponse.builder()
					.aiMessage(reply == null ? AiMessage.from("（无预设回复）") : reply)
					.build();
		}
	}
}