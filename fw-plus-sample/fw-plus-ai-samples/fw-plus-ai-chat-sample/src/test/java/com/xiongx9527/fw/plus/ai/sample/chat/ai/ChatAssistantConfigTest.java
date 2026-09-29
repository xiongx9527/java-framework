package com.xiongx9527.fw.plus.ai.sample.chat.ai;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 装配层测试：记忆的 store 共享与会话隔离、系统提示由 provider 拼装。
 *
 * <p>注意：{@code chatMemoryProvider} 不再预写 SystemMessage——系统提示改由
 * 由 {@link SystemPromptBuilder} 拼装、{@code systemMessageProvider} 每轮生成。</p>
 */
class ChatAssistantConfigTest {

	private final ChatAssistantConfig config = new ChatAssistantConfig();

	@Test
	void chatMemoryProviderSharesStoreAcrossCalls() {
		ChatMemoryStore store = config.chatMemoryStore();
		ChatMemoryProvider provider = config.chatMemoryProvider(store);

		provider.get("s-1").add(UserMessage.from("第一句"));

		ChatMemory again = provider.get("s-1");

		assertNotSame(provider.get("s-1"), again, "每次 get 返回新实例是预期，靠共享 store 串起历史");
		assertEquals(1, again.messages().size());
		assertEquals("第一句", ((UserMessage) again.messages().get(0)).singleText());
	}

	@Test
	void chatMemoryProviderIsolatesDifferentSessionIds() {
		ChatMemoryProvider provider = config.chatMemoryProvider(new InMemoryChatMemoryStore());

		provider.get("s-1").add(UserMessage.from("会话一的话"));
		provider.get("s-2").add(UserMessage.from("会话二的话"));

		assertEquals("会话一的话", ((UserMessage) provider.get("s-1").messages().get(0)).singleText());
		assertEquals("会话二的话", ((UserMessage) provider.get("s-2").messages().get(0)).singleText());
	}

	@Test
	void providerDoesNotPrewriteSystemMessage() {
		ChatMemoryProvider provider = config.chatMemoryProvider(new InMemoryChatMemoryStore());

		List<ChatMessage> messages = provider.get("s-1").messages();

		// provider 只建记忆容器；系统提示由 systemMessageProvider 每轮生成
		assertTrue(messages.isEmpty(), "provider 不应预写任何消息，实际：" + messages.size());
	}

	@Test
	void windowTrimsMessagesBeyondMax() {
		ChatMemoryProvider provider = config.chatMemoryProvider(new InMemoryChatMemoryStore());
		ChatMemory memory = provider.get("s-1");

		for (int i = 0; i < 30; i++) {
			memory.add(UserMessage.from("第 " + i + " 句"));
		}

		assertEquals(20, memory.messages().size());
		assertEquals("第 10 句", ((UserMessage) memory.messages().get(0)).singleText());
	}



	@Test
	void plainMessageKeepsUserType() {
		ChatMemoryProvider provider = config.chatMemoryProvider(new InMemoryChatMemoryStore());

		provider.get("s-1").add(UserMessage.from("普通消息"));

		assertEquals(ChatMessageType.USER, provider.get("s-1").messages().get(0).type());
	}
}
