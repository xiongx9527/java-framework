package com.xiongx9527.fw.plus.ai.sample.chat.ai;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.Assistant;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 真实 {@code AiServices} 链路上的记忆行为回归。
 *
 * <p>用可编排的假模型跑通「模型请求工具 → 框架执行 → 模型收尾」的完整循环。存在的理由：
 * {@link EphemeralChatMemoryTest} 每个用例都新建 wrapper，而真实链路里
 * {@code AiServiceContext.chatMemories} 用 {@code computeIfAbsent} 按 memoryId 缓存实例、
 * provider 只会被调用一次——两者语义不同，暂存跨轮残留的缺陷只有在本文件这种跑法下才暴露。</p>
 */
class EphemeralChatMemoryIntegrationTest {

	private static final String SESSION = "s-1";

	@Test
	void secondTurnDoesNotSeePreviousToolRoundTrip() {
		WeatherTools weatherTools = new WeatherTools();
		RecordingChatModel model = new RecordingChatModel().reply(
				// 第一轮：先请求工具，拿到结果后收尾
				AiMessage.from(ToolExecutionRequest.builder()
						.name("queryWeather").arguments("{\"city\":\"北京\"}").build()),
				AiMessage.from("北京当前 32℃。"),
				// 第二轮：直接收尾
				AiMessage.from("北京当前 32℃，和刚才一样。"));

		Assistant assistant = AiServices.builder(Assistant.class)
				.chatLanguageModel(model)
				.chatMemoryProvider(new ChatAssistantConfig()
						.chatMemoryProvider(new InMemoryChatMemoryStore()))
				.tools(weatherTools)
				.build();

		assistant.chat(SESSION, "", "北京天气怎么样");
		assistant.chat(SESSION, "", "再问一次");

		assertEquals(1, weatherTools.getCallCount(), "第一轮应触发一次工具调用");
		assertEquals(Arrays.asList(ChatMessageType.USER, ChatMessageType.USER),
				typesOf(model.requests().get(2)),
				"第二轮请求不得带上第一轮的工具往返，否则模型看到现成结果就不再调用工具");
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
	 * <p>带工具的重载必须一并实现——{@code ChatLanguageModel} 对它的默认实现是直接抛
	 * {@code IllegalArgumentException: Tools are currently not supported by this model}，
	 * 而框架走的正是这个重载。</p>
	 */
	private static class RecordingChatModel implements ChatLanguageModel {

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
		public Response<AiMessage> generate(List<ChatMessage> messages) {
			return record(messages);
		}

		@Override
		public Response<AiMessage> generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) {
			return record(messages);
		}

		private Response<AiMessage> record(List<ChatMessage> messages) {
			requests.add(new ArrayList<>(messages));
			AiMessage reply = replies.poll();
			return Response.from(reply == null ? AiMessage.from("（无预设回复）") : reply);
		}
	}
}
