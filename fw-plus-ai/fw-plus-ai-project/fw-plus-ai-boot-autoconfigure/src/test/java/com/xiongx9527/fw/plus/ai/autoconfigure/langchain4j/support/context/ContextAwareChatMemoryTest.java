package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 维护装饰器：写时滚动维护 + 读时兜底，捕获本轮用户输入；
 * 上下文「注入」不在记忆读侧承担（langchain4j 1.x 一轮内先读后写），
 * 由 {@code AssistantBuilder} 的 {@code chatRequestTransformer} 完成，
 * 集成覆盖见 {@code AssistantBuilderContextInjectionTest}。
 */
class ContextAwareChatMemoryTest {

	private ContextAwareChatMemory memory(ContextMaintenance... maintenances) {
		ChatMemory delegate = MessageWindowChatMemory.builder()
				.id("t1:crm:bot:u1:s1")
				.maxMessages(20)
				.chatMemoryStore(new InMemoryChatMemoryStore())
				.build();
		return new ContextAwareChatMemory(delegate,
				latest -> new ContextRequest("t1:crm:bot:u1:s1", "t1:crm:bot:u1:s1", "u1", latest),
				java.util.Arrays.asList(maintenances));
	}

	@Test
	void messagesReturnStoredWindowUnchanged() {
		ContextAwareChatMemory memory = memory();

		memory.add(UserMessage.from("你好"));
		memory.add(AiMessage.from("回复"));

		List<ChatMessage> messages = memory.messages();
		assertThat(messages).hasSize(2);
		assertThat(messages.get(0)).isInstanceOf(UserMessage.class);
		assertThat(messages.get(1)).isInstanceOf(AiMessage.class);
	}

	@Test
	void capturesLatestUserMessageForMaintenance() {
		java.util.concurrent.atomic.AtomicReference<String> captured =
				new java.util.concurrent.atomic.AtomicReference<String>();
		ContextMaintenance recorder = (request, messages) -> captured.set(request.getLatestUserMessage());

		ContextAwareChatMemory memory = memory(recorder);
		memory.add(UserMessage.from("杭州天气怎么样"));
		// 写时维护在 AI 回复落库后触发，请求携带本轮用户输入
		memory.add(AiMessage.from("晴"));

		assertThat(captured.get()).isEqualTo("杭州天气怎么样");
	}

	// -------------------------------------------------- 写时维护 + 读时兜底

	@Test
	void maintenanceRunsOnWriteNotOnEveryRead() {
		java.util.List<String> calls = new java.util.ArrayList<String>();
		ContextMaintenance recorder = (request, messages) -> calls.add(request.getSessionKey());

		ContextAwareChatMemory memory = memory(recorder);
		// 首轮：messages() 之前从未写过 → 读时兜底一次
		memory.messages();
		assertThat(calls).as("首轮读时兜底").hasSize(1);

		// 写：用户消息不触发；AI 回复落库后触发一次
		memory.add(UserMessage.from("你好"));
		assertThat(calls).as("仅用户消息不触发维护").hasSize(1);
		memory.add(AiMessage.from("回复"));
		assertThat(calls).as("AI 回复落库触发写时维护").hasSize(2);

		// 读：写路径已维护，读取不再重复维护
		memory.messages();
		memory.messages();
		assertThat(calls).as("写后读取不重复维护").hasSize(2);

		// 下一轮：AI 回复再次触发
		memory.add(UserMessage.from("再来"));
		memory.add(AiMessage.from("好的"));
		assertThat(calls).as("每轮写时维护一次").hasSize(3);
	}

	@Test
	void writeTimeMaintenanceSeesHistoryWithLatestReply() {
		java.util.List<List<ChatMessage>> seen = new java.util.ArrayList<List<ChatMessage>>();
		ContextMaintenance recorder = (request, messages) ->
				seen.add(new java.util.ArrayList<ChatMessage>(messages));

		ContextAwareChatMemory memory = memory(recorder);
		memory.add(UserMessage.from("你好"));
		memory.add(AiMessage.from("回复一"));
		memory.add(UserMessage.from("再来"));
		memory.add(AiMessage.from("回复二"));

		// 维护输入 = 写前历史 + 本轮 AI 回复（抢在窗口驱逐前，回复不丢）
		assertThat(seen).hasSize(2);
		assertThat(seen.get(0)).hasSize(2);
		assertThat(seen.get(1)).hasSize(4);
		assertThat(((AiMessage) seen.get(1).get(3)).text()).isEqualTo("回复二");
	}

	@Test
	void readPathFallbackRunsOnceWhenHistoryLoadedWithoutWrite() {
		java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger();
		ContextMaintenance recorder = (request, messages) -> counter.incrementAndGet();

		ContextAwareChatMemory memory = memory(recorder);
		// 模拟进程重启后从存储加载历史（无 add 调用）
		memory.messages();
		memory.messages();

		assertThat(counter.get()).as("读时兜底只跑一次").isEqualTo(1);
	}

	@Test
	void maintenanceFailureDoesNotBreakConversation() {
		ContextMaintenance boom = (request, messages) -> {
			throw new IllegalStateException("摘要服务不可用");
		};

		ContextAwareChatMemory memory = memory(boom);
		memory.add(UserMessage.from("你好"));
		memory.add(AiMessage.from("回复"));

		// 维护抛异常被吞掉：消息窗口照常返回
		assertThat(memory.messages()).isNotEmpty();
	}

	@Test
	void clearResetsMaintenanceState() {
		java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger();
		ContextMaintenance recorder = (request, messages) -> counter.incrementAndGet();

		ContextAwareChatMemory memory = memory(recorder);
		memory.messages();
		assertThat(counter.get()).isEqualTo(1);

		memory.clear();
		memory.messages();
		assertThat(counter.get()).as("清空后读时兜底重新生效").isEqualTo(2);
		assertThat(memory.messages()).isEmpty();
	}

	// -------------------------------------------------- 3 参钩子（当轮消息，纯增量）

	@Test
	void writePathCarriesTurnAiMessageToThreeArgMaintenance() {
		java.util.List<ChatMessage> received = new java.util.ArrayList<ChatMessage>();
		ContextMaintenance recorder = new ContextMaintenance() {
			@Override
			public void maintain(ContextRequest request, List<ChatMessage> messages) {
				received.add(UserMessage.from("不应走 2 参转发"));
			}

			@Override
			public void maintain(ContextRequest request, List<ChatMessage> messages, ChatMessage turnMessage) {
				received.add(turnMessage);
			}
		};

		ContextAwareChatMemory memory = memory(recorder);
		memory.add(UserMessage.from("你好"));
		memory.add(AiMessage.from("回复"));

		assertThat(received).hasSize(1);
		assertThat(received.get(0)).isInstanceOf(AiMessage.class);
		assertThat(((AiMessage) received.get(0)).text()).isEqualTo("回复");
	}

	@Test
	void readPathFallbackPassesNullTurnMessage() {
		java.util.List<ChatMessage> received = new java.util.ArrayList<ChatMessage>();
		ContextMaintenance recorder = new ContextMaintenance() {
			@Override
			public void maintain(ContextRequest request, List<ChatMessage> messages) {
				received.add(UserMessage.from("不应走 2 参转发"));
			}

			@Override
			public void maintain(ContextRequest request, List<ChatMessage> messages, ChatMessage turnMessage) {
				received.add(turnMessage);
			}
		};

		ContextAwareChatMemory memory = memory(recorder);
		memory.messages();

		assertThat(received).hasSize(1);
		assertThat(received.get(0)).as("读时兜底无当轮消息").isNull();
	}
}
