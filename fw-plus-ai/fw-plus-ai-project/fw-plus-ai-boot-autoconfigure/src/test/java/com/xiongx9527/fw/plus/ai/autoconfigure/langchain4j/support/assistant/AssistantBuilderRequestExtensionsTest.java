package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context.ContextStrategy;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary.InMemorySummaryStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary.SummaryContextProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary.SummaryRecord;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 装配层：系统提示变换（{@code systemMessageTransformer}）、请求变换（{@code chatRequestTransformer}）、
 * 助手级请求参数（{@code chatRequestParameters}）三个原生扩展点的封装与组合顺序。
 *
 * <p>用 {@code thenAnswer} 捕获发往模型的完整请求（消息 + 参数），逐点断言。</p>
 */
class AssistantBuilderRequestExtensionsTest {

	private static final String SESSION = "t1:crm:bot:u1:s1";

	static class EchoTool {
		@Tool(name = "echo", value = "回声")
		public String echo(String text) {
			return text;
		}
	}

	/** mock 模型：捕获每轮发往模型的请求，固定回「回复」 */
	private ChatModel capturingModel(List<ChatRequest> captured) {
		ChatModel model = mock(ChatModel.class);
		when(model.chat(any(ChatRequest.class))).thenAnswer(invocation -> {
			ChatRequest request = invocation.getArgument(0);
			captured.add(request);
			return ChatResponse.builder().aiMessage(AiMessage.from("回复")).build();
		});
		return model;
	}

	private InMemorySummaryStore summaryStoreWith(String summary) {
		InMemorySummaryStore store = new InMemorySummaryStore();
		store.put(SESSION, new SummaryRecord(summary, 1));
		return store;
	}

	private static String firstSystemMessage(ChatRequest request) {
		for (ChatMessage message : request.messages()) {
			if (message instanceof SystemMessage) {
				return ((SystemMessage) message).text();
			}
		}
		return null;
	}

	private static ChatRequestParameters withTemperature(ChatRequest request, double temperature) {
		return request.parameters().overrideWith(
				ChatRequestParameters.builder().temperature(temperature).build());
	}

	@Test
	void systemMessageTransformerTransformsEvaluatedText() {
		List<ChatRequest> captured = new ArrayList<ChatRequest>();
		SimpleAssistant assistant = new AssistantBuilder()
				.chatModel(capturingModel(captured))
				.systemMessage("基础提示")
				.systemMessageTransformer(message -> message + "|追加")
				.build();

		assistant.chat(SESSION, "你好");

		assertThat(firstSystemMessage(captured.get(0))).isEqualTo("基础提示|追加");
	}

	@Test
	void systemMessageTransformerWithInvocationContextSeesMemoryId() {
		List<ChatRequest> captured = new ArrayList<ChatRequest>();
		SimpleAssistant assistant = new AssistantBuilder()
				.chatModel(capturingModel(captured))
				.systemMessage("提示")
				.systemMessageTransformer((message, invocation) -> message + "|" + invocation.chatMemoryId())
				.build();

		assistant.chat(SESSION, "你好");

		assertThat(firstSystemMessage(captured.get(0))).isEqualTo("提示|" + SESSION);
	}

	@Test
	void chatRequestParametersOverridePerTurnParametersAndPreserveTools() {
		List<ChatRequest> captured = new ArrayList<ChatRequest>();
		SimpleAssistant assistant = new AssistantBuilder()
				.chatModel(capturingModel(captured))
				.tools(new EchoTool())
				.chatRequestParameters(ChatRequestParameters.builder()
						.temperature(0.25)
						.maxOutputTokens(321)
						.build())
				.build();

		assistant.chat(SESSION, "你好");

		ChatRequestParameters parameters = captured.get(0).parameters();
		assertThat(parameters.temperature()).isEqualTo(0.25);
		assertThat(parameters.maxOutputTokens()).isEqualTo(321);
		assertThat(parameters.toolSpecifications()).extracting(ToolSpecification::name)
				.containsExactly("echo");
	}

	@Test
	void chatRequestTransformerSeesInjectedContextAndRunsLast() {
		List<ChatRequest> captured = new ArrayList<ChatRequest>();
		List<String> seenByTransformer = new ArrayList<String>();
		SimpleAssistant assistant = new AssistantBuilder()
				.chatModel(capturingModel(captured))
				.memoryStore(new InMemoryChatMemoryStore())
				.contextStrategy(ContextStrategy.MEMORY_AND_ENRICHED)
				.contextProvider(new SummaryContextProvider(summaryStoreWith("用户偏好简洁回答")))
				.chatRequestTransformer(request -> {
					seenByTransformer.add(firstSystemMessage(request));
					return request.toBuilder()
							.parameters(withTemperature(request, 0.9))
							.build();
				})
				.build();

		assistant.chat(SESSION, "你好");

		// 用户变换执行时，内部上下文注入已完成（链中注入在前、用户变换最后）
		assertThat(seenByTransformer.get(0)).contains("用户偏好简洁回答");
		assertThat(captured.get(0).parameters().temperature()).isEqualTo(0.9);
	}

	@Test
	void customChatRequestTransformerHasFinalSayWithoutContextProviders() {
		List<ChatRequest> captured = new ArrayList<ChatRequest>();
		SimpleAssistant assistant = new AssistantBuilder()
				.chatModel(capturingModel(captured))
				.chatRequestTransformer(request -> {
					List<ChatMessage> messages = new ArrayList<ChatMessage>(request.messages());
					messages.add(SystemMessage.from("补充约束"));
					return request.toBuilder()
							.messages(messages)
							.parameters(withTemperature(request, 0.9))
							.build();
				})
				.build();

		assistant.chat(SESSION, "你好");

		// 即使未启用上下文注入，用户请求变换同样注册生效：消息可增删改、参数可覆盖
		ChatRequest request = captured.get(0);
		ChatMessage last = request.messages().get(request.messages().size() - 1);
		assertThat(last).isInstanceOf(SystemMessage.class);
		assertThat(((SystemMessage) last).text()).isEqualTo("补充约束");
		assertThat(request.parameters().temperature()).isEqualTo(0.9);
	}
}
