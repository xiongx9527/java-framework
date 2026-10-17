package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.facts;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.AssistantBuilder;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.SimpleAssistant;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context.ContextStrategy;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageJsonCodec;
import dev.langchain4j.data.message.JacksonChatMessageJsonCodec;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
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
 * 任务 5.1–5.3：事实通道端到端集成（内存 store + rule 抽取 + keyword 召回，零外部依赖）。
 *
 * <p>装配层直连 {@link AssistantBuilder}：上下文注入发生在 {@code chatRequestTransformer}
 * （请求装配后、模型调用前），断言以捕获发往模型的消息为准；写入路径仍由
 * {@code ContextAwareChatMemory} 的维护钩子承担。</p>
 *
 * <p>覆盖三条语义：跨会话沉淀与召回（5.1）、事实与消息记忆物理隔离（5.2）、
 * 事实关闭时基线（会话记忆链路）不变（5.3）。</p>
 */
class FactMemoryIntegrationTest {

	private static final String USER = "u1";

	private static final String MEM_S1 = "t1:crm:bot:u1:s1";

	private static final String MEM_S2 = "t1:crm:bot:u1:s2";

	private static final ChatMessageJsonCodec CODEC = new JacksonChatMessageJsonCodec();

	private final InMemoryFactStore factStore = new InMemoryFactStore();

	private final InMemoryChatMemoryStore messageStore = new InMemoryChatMemoryStore();

	// -------------------------------------------------- 5.1 跨会话沉淀 → 召回

	@Test
	void factSinksInSessionOneAndRecallsInSessionTwo() {
		List<List<ChatMessage>> captured = new ArrayList<List<ChatMessage>>();
		SimpleAssistant assistant = factWiredAssistant(captured, "你好，张三！");

		assistant.chat(MEM_S1, "大家好，我叫张三");

		assertThat(factStore.facts).as("s1 轮末沉淀事实").contains("我叫张三");

		assistant.chat(MEM_S2, "我叫什么名字？");

		List<ChatMessage> request = captured.get(1);
		assertThat(request.get(0)).isInstanceOf(SystemMessage.class);
		String context = ((SystemMessage) request.get(0)).text();
		assertThat(context).as("s2 跨会话召回命中").startsWith("【用户长期记忆】");
		assertThat(context).contains("我叫张三");
		assertThat(request.get(1)).isInstanceOf(UserMessage.class);
	}

	// -------------------------------------------------- 5.2 事实不污染消息记忆

	@Test
	void factWritesDoNotPolluteMessageMemory() {
		List<List<ChatMessage>> captured = new ArrayList<List<ChatMessage>>();
		SimpleAssistant assistant = factWiredAssistant(captured, "你好，张三！");

		assistant.chat(MEM_S1, "大家好，我叫张三");

		assertThat(factStore.facts).isNotEmpty();

		List<ChatMessage> stored = messageStore.getMessages(MEM_S1);
		assertThat(stored).as("消息记忆只含消息（无注入块、无事实条目）").hasSize(2);
		assertThat(stored).allSatisfy(message ->
				assertThat(message).isInstanceOfAny(UserMessage.class, AiMessage.class));

		String json = CODEC.messagesToJson(stored);
		assertThat(CODEC.messagesFromJson(json)).as("消息体 JSON 解编往返正常").isEqualTo(stored);
	}

	// -------------------------------------------------- 5.3 事实关闭/不可用 → 基线不变

	@Test
	void factsDisabledKeepsBaselineContext() {
		List<List<ChatMessage>> captured = new ArrayList<List<ChatMessage>>();
		SimpleAssistant assistant = new AssistantBuilder()
				.chatModel(capturingModel(captured, "你好，有什么可以帮你？"))
				.memoryStore(new InMemoryChatMemoryStore())
				.contextStrategy(ContextStrategy.MEMORY_AND_ENRICHED)
				.contextProvider(request -> "【会话摘要】\n- 基线摘要")
				.build();

		assistant.chat(MEM_S1, "你好");

		List<ChatMessage> request = captured.get(0);
		assertThat(request.get(0)).isInstanceOf(SystemMessage.class);
		String context = ((SystemMessage) request.get(0)).text();
		assertThat(context).isEqualTo("【会话摘要】\n- 基线摘要");
		assertThat(context).doesNotContain("【用户长期记忆】");
		assertThat(request.get(1)).isInstanceOf(UserMessage.class);
		assertThat(factStore.facts).as("事实通道未被触碰").isEmpty();
	}

	// -------------------------------------------------- 基础设施

	/** 组装真实链路：写路径 FactMaintenance（rule 抽取，内联） + 读路径 FactContextProvider（keyword 召回） */
	private SimpleAssistant factWiredAssistant(List<List<ChatMessage>> captured, String reply) {
		return new AssistantBuilder()
				.chatModel(capturingModel(captured, reply))
				.memoryStore(messageStore)
				.contextStrategy(ContextStrategy.MEMORY_AND_ENRICHED)
				.contextProvider(new FactContextProvider(FactRetrievers.resolve("keyword", factStore, null), 5, 500))
				.contextMaintenance(new FactMaintenance(FactExtractors.resolve("rule", factStore, null),
						FactWriteMode.PER_TURN, 5, false, new FactTurnBuffer()))
				.build();
	}

	/** mock 模型：捕获每轮发往模型的消息（含注入块），固定回给定回复 */
	private static ChatModel capturingModel(List<List<ChatMessage>> captured, String reply) {
		ChatModel model = mock(ChatModel.class);
		when(model.chat(any(ChatRequest.class))).thenAnswer(invocation -> {
			ChatRequest request = invocation.getArgument(0);
			captured.add(new ArrayList<ChatMessage>(request.messages()));
			return ChatResponse.builder().aiMessage(AiMessage.from(reply)).build();
		});
		return model;
	}

	/** 用户维度内存事实存储：集成测试不依赖真实 mem0 服务端 */
	static final class InMemoryFactStore implements FactStore {

		final List<String> facts = new ArrayList<String>();

		@Override
		public void addDialogue(List<ChatMessage> turnMessages, String userId) {
		}

		@Override
		public void addFacts(List<String> facts, String userId) {
			for (String fact : facts) {
				if (fact != null && !fact.trim().isEmpty() && !this.facts.contains(fact.trim())) {
					this.facts.add(fact.trim());
				}
			}
		}

		@Override
		public List<String> search(String query, String userId, int topK) {
			return new ArrayList<String>(facts);
		}

		@Override
		public List<String> listAll(String userId, int limit) {
			return new ArrayList<String>(facts);
		}
	}

}
