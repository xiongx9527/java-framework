package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 注入点落在 ChatMemory 装饰器：上下文前置为系统消息，最近消息窗口语义不变。
 */
class ContextAwareChatMemoryTest {

	private ContextAwareChatMemory memory(ContextInjector injector) {
		return memory(injector, new ContextMaintenance[0]);
	}

	private ContextAwareChatMemory memory(ContextInjector injector, ContextMaintenance... maintenances) {
		ChatMemory delegate = MessageWindowChatMemory.builder()
				.id("t1:crm:bot:u1:s1")
				.maxMessages(20)
				.chatMemoryStore(new InMemoryChatMemoryStore())
				.build();
		return new ContextAwareChatMemory(delegate, injector,
				latest -> new ContextRequest("t1:crm:bot:u1:s1", "t1:crm:bot:u1:s1", "u1", latest),
				java.util.Arrays.asList(maintenances));
	}

	private ContextInjector emptyInjector() {
		return new ContextInjector(
				java.util.Collections.singletonList(r -> ""), ContextStrategy.MEMORY_AND_ENRICHED);
	}

	@Test
	void contextIsPrependedBeforeRecentMessages() {
		ContextInjector injector = new ContextInjector(
				java.util.Collections.singletonList(r -> "会话摘要：用户喜欢简洁回答"),
				ContextStrategy.MEMORY_AND_ENRICHED);
		ContextAwareChatMemory memory = memory(injector);

		memory.add(UserMessage.from("你好"));
		memory.add(AiMessage.from("你好，请问有什么可以帮你？"));

		List<ChatMessage> messages = memory.messages();
		assertThat(messages).hasSizeGreaterThanOrEqualTo(3);
		assertThat(messages.get(0)).isInstanceOf(SystemMessage.class);
		assertThat(((SystemMessage) messages.get(0)).text()).isEqualTo("会话摘要：用户喜欢简洁回答");
		// 最近原文仍按窗口保留在注入块之后
		assertThat(messages.get(1)).isInstanceOf(UserMessage.class);
	}

	@Test
	void noContextLeavesWindowUntouched() {
		ContextInjector injector = new ContextInjector(
				java.util.Collections.singletonList(r -> ""), ContextStrategy.MEMORY_AND_ENRICHED);
		ContextAwareChatMemory memory = memory(injector);

		memory.add(UserMessage.from("你好"));
		memory.add(AiMessage.from("回复"));

		List<ChatMessage> messages = memory.messages();
		assertThat(messages).hasSize(2);
		assertThat(messages.get(0)).isInstanceOf(UserMessage.class);
	}

	@Test
	void capturesLatestUserMessageForProviders() {
		java.util.concurrent.atomic.AtomicReference<String> captured = new java.util.concurrent.atomic.AtomicReference<String>();
		ContextInjector injector = new ContextInjector(
				java.util.Collections.singletonList(r -> {
					captured.set(r.getLatestUserMessage());
					return "";
				}), ContextStrategy.MEMORY_AND_ENRICHED);
		ContextAwareChatMemory memory = memory(injector);

		memory.add(UserMessage.from("杭州天气怎么样"));
		memory.messages();

		assertThat(captured.get()).isEqualTo("杭州天气怎么样");
	}

	// -------------------------------------------------- 写时维护 + 读时兜底

	@Test
	void maintenanceRunsOnWriteNotOnEveryRead() {
		java.util.List<String> calls = new java.util.ArrayList<String>();
		ContextMaintenance recorder = (request, messages) -> calls.add(request.getSessionKey());

		ContextAwareChatMemory memory = memory(emptyInjector(), recorder);
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
	void readPathFallbackRunsOnceWhenHistoryLoadedWithoutWrite() {
		java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger();
		ContextMaintenance recorder = (request, messages) -> counter.incrementAndGet();

		ContextAwareChatMemory memory = memory(emptyInjector(), recorder);
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

		ContextAwareChatMemory memory = memory(
				new ContextInjector(java.util.Collections.singletonList(r -> "上下文"),
						ContextStrategy.MEMORY_AND_ENRICHED), boom);
		memory.add(UserMessage.from("你好"));
		memory.add(AiMessage.from("回复"));

		// 维护抛异常被吞掉：消息窗口与注入上下文照常返回
		assertThat(memory.messages()).isNotEmpty();
	}

	@Test
	void clearResetsMaintenanceState() {
		java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger();
		ContextMaintenance recorder = (request, messages) -> counter.incrementAndGet();

		ContextAwareChatMemory memory = memory(emptyInjector(), recorder);
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

		ContextAwareChatMemory memory = memory(emptyInjector(), recorder);
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

		ContextAwareChatMemory memory = memory(emptyInjector(), recorder);
		memory.messages();

		assertThat(received).hasSize(1);
		assertThat(received.get(0)).as("读时兜底无当轮消息").isNull();
	}
}