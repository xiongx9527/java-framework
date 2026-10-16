package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context.ContextStrategy;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary.InMemorySummaryStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary.RollingSummaryEngine;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary.Summarizer;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary.Summarizers;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary.SummaryContextProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary.SummaryMaintenance;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
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
 * 集成验证：
 * <ul>
 *   <li>5.1 同一用户两个会话并行对话，记忆互不可见；</li>
 *   <li>5.2 长会话累计超出 {@code trigger-tokens} 后，上下文包含会话摘要，原始消息未被删除。</li>
 * </ul>
 */
class SessionMemoryIntegrationTest {

	private static final String S1 = "t1:crm:bot:u1:s1";
	private static final String S2 = "t1:crm:bot:u1:s2";

	private ChatModel capturingModel(List<List<ChatMessage>> captured) {
		ChatModel model = mock(ChatModel.class);
		when(model.chat(any(ChatRequest.class))).thenAnswer(invocation -> {
			ChatRequest request = invocation.getArgument(0);
			captured.add(new ArrayList<ChatMessage>(request.messages()));
			return ChatResponse.builder().aiMessage(AiMessage.from("回复")).build();
		});
		return model;
	}

	private static boolean containsText(List<ChatMessage> messages, String needle) {
		for (ChatMessage message : messages) {
			if (message instanceof dev.langchain4j.data.message.UserMessage
					&& ((dev.langchain4j.data.message.UserMessage) message).singleText().contains(needle)) {
				return true;
			}
			if (message instanceof AiMessage && ((AiMessage) message).text().contains(needle)) {
				return true;
			}
		}
		return false;
	}

	private static String firstSystemText(List<ChatMessage> messages) {
		for (ChatMessage message : messages) {
			if (message instanceof SystemMessage) {
				return ((SystemMessage) message).text();
			}
		}
		return null;
	}

	@Test
	void sessionsAreIsolated() {
		List<List<ChatMessage>> captured = new ArrayList<List<ChatMessage>>();
		InMemoryChatMemoryStore store = new InMemoryChatMemoryStore();
		SimpleAssistant assistant = new AssistantBuilder()
				.chatModel(capturingModel(captured))
				.memoryStore(store)
				.build();

		assistant.chat(S1, "会话一的秘密A");
		assistant.chat(S2, "会话二的问候B");

		// 第二次调用（会话二）发往模型的消息不得包含会话一内容
		List<ChatMessage> secondTurn = captured.get(1);
		assertThat(containsText(secondTurn, "秘密A")).isFalse();
		assertThat(containsText(secondTurn, "问候B")).isTrue();

		// 回到会话一：只看到自己的历史，看不到会话二内容
		assistant.chat(S1, "会话一继续");
		List<ChatMessage> thirdTurn = captured.get(2);
		assertThat(containsText(thirdTurn, "秘密A")).isTrue();
		assertThat(containsText(thirdTurn, "问候B")).isFalse();
	}

	@Test
	void longSessionProducesSummaryWithoutDeletingMessages() {
		List<List<ChatMessage>> captured = new ArrayList<List<ChatMessage>>();
		InMemoryChatMemoryStore messageStore = new InMemoryChatMemoryStore();
		InMemorySummaryStore summaryStore = new InMemorySummaryStore();

		Summarizer refine = Summarizers.resolve(Summarizers.REFINE, null);
		Summarizer rebuild = Summarizers.resolve(Summarizers.REBUILD, null);
		// 触发阈值极小，保证长会话必然触发；keep 足够大，摘要不被截空
		RollingSummaryEngine engine = new RollingSummaryEngine(summaryStore, refine, rebuild,
				null, 5, 1000, 1000, 0);

		SimpleAssistant assistant = new AssistantBuilder()
				.chatModel(capturingModel(captured))
				.memoryStore(messageStore)
				.contextStrategy(ContextStrategy.MEMORY_AND_ENRICHED)
				.contextProvider(new SummaryContextProvider(summaryStore))
				.contextMaintenance(new SummaryMaintenance(engine, 4))
				.build();

		for (int i = 1; i <= 8; i++) {
			assistant.chat(S1, "第" + i + "轮内容");
		}

		// 摘要已生成
		assertThat(summaryStore.get(S1).getSummary()).isNotBlank();
		// 后续轮次的上下文包含摘要段（前置系统消息），且仍保留最近原文
		List<ChatMessage> lastTurn = captured.get(captured.size() - 1);
		assertThat(firstSystemText(lastTurn)).isNotBlank();
		assertThat(containsText(lastTurn, "第8轮内容")).isTrue();
		// 原始消息未被摘要删除（消息存储仍持有最近窗口原文）
		assertThat(messageStore.getMessages(S1)).isNotEmpty();
	}
}
