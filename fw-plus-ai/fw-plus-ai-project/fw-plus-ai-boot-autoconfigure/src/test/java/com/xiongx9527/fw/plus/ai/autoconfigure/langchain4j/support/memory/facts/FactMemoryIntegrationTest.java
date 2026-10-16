package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.facts;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context.ContextAwareChatMemory;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context.ContextInjector;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context.ContextMaintenance;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context.ContextProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context.ContextRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context.ContextStrategy;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageJsonCodec;
import dev.langchain4j.data.message.GsonChatMessageJsonCodec;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 任务 5.1–5.3：事实通道端到端集成（内存 store + rule 抽取 + keyword 召回，零外部依赖）。
 *
 * <p>覆盖三条语义：跨会话沉淀与召回（5.1）、事实与消息记忆物理隔离（5.2）、
 * 事实关闭时基线（会话记忆链路）不变（5.3）。</p>
 */
class FactMemoryIntegrationTest {

	private static final String USER = "u1";

	private static final String MEM_S1 = "t1:crm:bot:u1:s1";

	private static final String MEM_S2 = "t1:crm:bot:u1:s2";

	private static final ChatMessageJsonCodec CODEC = new GsonChatMessageJsonCodec();

	private final InMemoryFactStore factStore = new InMemoryFactStore();

	private final InMemoryChatMemoryStore messageStore = new InMemoryChatMemoryStore();

	// -------------------------------------------------- 5.1 跨会话沉淀 → 召回

	@Test
	void factSinksInSessionOneAndRecallsInSessionTwo() {
		ContextAwareChatMemory session1 = factWiredMemory(MEM_S1);
		session1.add(UserMessage.from("大家好，我叫张三"));
		session1.add(AiMessage.from("你好，张三！"));

		assertThat(factStore.facts).as("s1 轮末沉淀事实").contains("我叫张三");

		ContextAwareChatMemory session2 = factWiredMemory(MEM_S2);
		session2.add(UserMessage.from("我叫什么名字？"));
		List<ChatMessage> messages = session2.messages();

		assertThat(messages.get(0)).isInstanceOf(SystemMessage.class);
		String context = ((SystemMessage) messages.get(0)).text();
		assertThat(context).as("s2 跨会话召回命中").startsWith("【用户长期记忆】");
		assertThat(context).contains("我叫张三");
		assertThat(messages.get(1)).isInstanceOf(UserMessage.class);
	}

	// -------------------------------------------------- 5.2 事实不污染消息记忆

	@Test
	void factWritesDoNotPolluteMessageMemory() {
		ContextAwareChatMemory session1 = factWiredMemory(MEM_S1);
		session1.add(UserMessage.from("大家好，我叫张三"));
		session1.add(AiMessage.from("你好，张三！"));

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
		List<ContextProvider> providers = new ArrayList<ContextProvider>();
		providers.add(request -> "【会话摘要】\n- 基线摘要");
		ContextInjector injector = new ContextInjector(providers, ContextStrategy.MEMORY_AND_ENRICHED);
		ChatMemory delegate = MessageWindowChatMemory.builder()
				.id(MEM_S1)
				.maxMessages(20)
				.chatMemoryStore(new InMemoryChatMemoryStore())
				.build();
		ContextAwareChatMemory memory = new ContextAwareChatMemory(delegate, injector,
				latest -> new ContextRequest(MEM_S1, MEM_S1, USER, latest));

		memory.add(UserMessage.from("你好"));
		memory.add(AiMessage.from("你好，有什么可以帮你？"));
		List<ChatMessage> messages = memory.messages();

		assertThat(messages.get(0)).isInstanceOf(SystemMessage.class);
		String context = ((SystemMessage) messages.get(0)).text();
		assertThat(context).isEqualTo("【会话摘要】\n- 基线摘要");
		assertThat(context).doesNotContain("【用户长期记忆】");
		assertThat(messages).hasSize(3);
		assertThat(factStore.facts).as("事实通道未被触碰").isEmpty();
	}

	// -------------------------------------------------- 基础设施

	/** 组装真实链路：写路径 FactMaintenance（rule 抽取，内联） + 读路径 FactContextProvider（keyword 召回） */
	private ContextAwareChatMemory factWiredMemory(String memId) {
		List<ContextProvider> providers = new ArrayList<ContextProvider>();
		providers.add(new FactContextProvider(FactRetrievers.resolve("keyword", factStore, null), 5, 500));
		ContextInjector injector = new ContextInjector(providers, ContextStrategy.MEMORY_AND_ENRICHED);
		ChatMemory delegate = MessageWindowChatMemory.builder()
				.id(memId)
				.maxMessages(20)
				.chatMemoryStore(messageStore)
				.build();
		List<ContextMaintenance> maintenances = new ArrayList<ContextMaintenance>();
		maintenances.add(new FactMaintenance(FactExtractors.resolve("rule", factStore, null),
				FactWriteMode.PER_TURN, 5, false, new FactTurnBuffer()));
		return new ContextAwareChatMemory(delegate, injector,
				latest -> new ContextRequest(memId, memId, USER, latest), maintenances);
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
