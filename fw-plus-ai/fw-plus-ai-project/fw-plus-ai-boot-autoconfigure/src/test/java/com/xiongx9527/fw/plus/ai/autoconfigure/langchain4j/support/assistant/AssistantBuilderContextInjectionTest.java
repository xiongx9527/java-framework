package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context.ContextStrategy;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary.InMemorySummaryStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary.SummaryContextProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary.SummaryRecord;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 装配层：启用 {@code MEMORY_AND_ENRICHED} 时注入链包含摘要来源，{@code MEMORY} 时不含。
 *
 * <p>用 {@code thenAnswer} 捕获发往模型的消息，规避 0.35.0 单/双参 {@code generate} 重载差异。</p>
 */
class AssistantBuilderContextInjectionTest {

	private static final String SESSION = "t1:crm:bot:u1:s1";

	/** mock 模型：捕获每轮发往模型的消息，固定回「回复」 */
	private ChatLanguageModel capturingModel(List<List<ChatMessage>> captured) {
		ChatLanguageModel model = mock(ChatLanguageModel.class);
		when(model.generate(anyList())).thenAnswer(invocation -> {
			captured.add(new ArrayList<ChatMessage>(invocation.getArgument(0)));
			return Response.from(AiMessage.from("回复"));
		});
		when(model.generate(anyList(), anyList())).thenAnswer(invocation -> {
			captured.add(new ArrayList<ChatMessage>(invocation.getArgument(0)));
			return Response.from(AiMessage.from("回复"));
		});
		return model;
	}

	private InMemorySummaryStore summaryStoreWith(String summary) {
		InMemorySummaryStore store = new InMemorySummaryStore();
		store.put(SESSION, new SummaryRecord(summary, 1));
		return store;
	}

	private String firstSystemMessage(List<List<ChatMessage>> captured) {
		assertThat(captured).isNotEmpty();
		for (ChatMessage message : captured.get(0)) {
			if (message instanceof SystemMessage) {
				return ((SystemMessage) message).text();
			}
		}
		return null;
	}

	@Test
	void enrichedStrategyInjectsSummaryProvider() {
		List<List<ChatMessage>> captured = new ArrayList<List<ChatMessage>>();
		SimpleAssistant assistant = new AssistantBuilder()
				.chatModel(capturingModel(captured))
				.memoryStore(new InMemoryChatMemoryStore())
				.contextStrategy(ContextStrategy.MEMORY_AND_ENRICHED)
				.contextProvider(new SummaryContextProvider(summaryStoreWith("用户偏好简洁回答")))
				.build();

		assistant.chat(SESSION, "你好");

		assertThat(firstSystemMessage(captured)).contains("用户偏好简洁回答");
	}

	@Test
	void memoryStrategyDoesNotInjectProviders() {
		List<List<ChatMessage>> captured = new ArrayList<List<ChatMessage>>();
		SimpleAssistant assistant = new AssistantBuilder()
				.chatModel(capturingModel(captured))
				.memoryStore(new InMemoryChatMemoryStore())
				.contextStrategy(ContextStrategy.MEMORY)
				.contextProvider(new SummaryContextProvider(summaryStoreWith("用户偏好简洁回答")))
				.build();

		assistant.chat(SESSION, "你好");

		String systemText = firstSystemMessage(captured);
		assertThat(systemText == null ? "" : systemText).doesNotContain("用户偏好简洁回答");
	}

	@Test
	void latestUserMessageIsCapturedAsProviderInput() {
		List<List<ChatMessage>> captured = new ArrayList<List<ChatMessage>>();
		List<String> seenQuery = new ArrayList<String>();
		SimpleAssistant assistant = new AssistantBuilder()
				.chatModel(capturingModel(captured))
				.memoryStore(new InMemoryChatMemoryStore())
				.contextStrategy(ContextStrategy.MEMORY_AND_ENRICHED)
				.contextProvider(request -> {
					seenQuery.add(request.getLatestUserMessage());
					return "";
				})
				.build();

		assistant.chat(SESSION, "杭州今天天气");

		assertThat(seenQuery).contains("杭州今天天气");
	}

	@Test
	void providerExceptionDoesNotBreakChat() {
		List<List<ChatMessage>> captured = new ArrayList<List<ChatMessage>>();
		SimpleAssistant assistant = new AssistantBuilder()
				.chatModel(capturingModel(captured))
				.memoryStore(new InMemoryChatMemoryStore())
				.contextStrategy(ContextStrategy.MEMORY_AND_ENRICHED)
				.contextProvider(request -> {
					throw new IllegalStateException("provider 挂了");
				})
				.build();

		String answer = assistant.chat(SESSION, "你好");

		assertThat(answer).isEqualTo("回复");
	}

	@Test
	void noProvidersLeavesMemoryUntouched() {
		List<List<ChatMessage>> captured = new ArrayList<List<ChatMessage>>();
		SimpleAssistant assistant = new AssistantBuilder()
				.chatModel(capturingModel(captured))
				.memoryStore(new InMemoryChatMemoryStore())
				.contextStrategy(ContextStrategy.MEMORY_AND_ENRICHED)
				.build();

		assistant.chat(SESSION, "你好");

		assertThat(captured).isNotEmpty();
		assertThat(captured.get(0))
				.as("无来源时不注入前置系统消息")
				.noneMatch(m -> m instanceof SystemMessage && ((SystemMessage) m).text().contains("用户偏好"));
	}

	@SuppressWarnings("unused")
	private static List<ChatMessage> empty() {
		return Collections.emptyList();
	}
}
