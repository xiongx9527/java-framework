package com.xiongx9527.fw.plus.ai.sample.chat.ai;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.AiPersistencePosition;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.AiPersistenceStrategy;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.EphemeralChatMemory;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.PersistedAiItem;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link EphemeralChatMemory} 的行为锁定：历史只沉淀系统提示与用户原话，
 * 本轮的工具往返在「本轮未收尾」时照常带给模型，收尾后一律不再带出。
 * {@link EphemeralChatMemory} 的行为锁定。
 *
 * <p>覆盖两条主轴：</p>
 * <ul>
 *   <li>默认策略 {@link AiPersistenceStrategy#PERSIST_LAST_FINAL_ANSWER}：上一轮的最终回答会沉淀进历史、带到下一轮；</li>
 *   <li>{@link AiPersistenceStrategy#DROP_ALL_AI}：历史只留系统提示与用户原话（旧行为）。</li>
 * </ul>
 *
 * <p>多轮用例复用<b>同一个实例</b>——真实链路里框架只调用一次 {@code ChatMemoryProvider.get}
 * （{@code AiServiceContext.chatMemories} 的 {@code computeIfAbsent}），实例不随请求重建。</p>
 */
class EphemeralChatMemoryTest {

	private final InMemoryChatMemoryStore store = new InMemoryChatMemoryStore();

	private EphemeralChatMemory memory() {
		return memory(AiPersistenceStrategy.PERSIST_NON_TOOL_FINAL_ANSWER);
	}

	private EphemeralChatMemory memory(AiPersistenceStrategy strategy) {
		MessageWindowChatMemory delegate = MessageWindowChatMemory.builder()
				.id("s-1")
				.maxMessages(20)
				.chatMemoryStore(store)
				.build();
		return EphemeralChatMemory.builder(delegate, store)
				.aiPersistenceStrategy(strategy)
				.build();
	}

	private static AiMessage toolCall(String city) {
		return AiMessage.from(ToolExecutionRequest.builder()
				.name("queryWeather").arguments("{\"city\":\"" + city + "\"}").build());
	}

	private static List<ChatMessageType> typesOf(List<ChatMessage> messages) {
		List<ChatMessageType> types = new ArrayList<>();
		for (ChatMessage message : messages) {
			types.add(message.type());
		}
		return types;
	}

	@Test
	void keepsOrderOfMultipleToolRoundTrips() {
		EphemeralChatMemory memory = memory();

		memory.add(UserMessage.from("比较北京和杭州"));
		memory.add(toolCall("北京"));
		memory.add(ToolExecutionResultMessage.from("c1", "queryWeather", "32"));
		memory.add(toolCall("杭州"));
		memory.add(ToolExecutionResultMessage.from("c2", "queryWeather", "29"));

		// 本轮 user 之后紧跟两组「call → result」，顺序不倒
		assertEquals(Arrays.asList(
				ChatMessageType.USER,
				ChatMessageType.AI,
				ChatMessageType.TOOL_EXECUTION_RESULT,
				ChatMessageType.AI,
				ChatMessageType.TOOL_EXECUTION_RESULT), typesOf(memory.messages()));
	}

	@Test
	void neverEmitsRagSegmentFromMemory() {
		EphemeralChatMemory memory = memory();

		String firstRag = "《杭州热门景点》杭州著名景点有西湖";
		memory.add(UserMessage.from("杭州的天气适合玩什么" + EphemeralChatMemory.DEFAULT_RAG_SEPARATOR + firstRag));
		memory.add(AiMessage.from("适合。"));

		String secondRag = "《未来一周天气》明天有雨";
		memory.add(UserMessage.from("那明天呢" + EphemeralChatMemory.DEFAULT_RAG_SEPARATOR + secondRag));
		List<ChatMessage> messages = memory.messages();

		// 检索段只在请求里由框架补回（AssistantBuilder 已关掉 storeRetrievedContentInChatMemory），
		// 记忆侧两条用户消息都只剩原话
		assertEquals(Arrays.asList(ChatMessageType.USER, ChatMessageType.USER), typesOf(messages));
		assertEquals("杭州的天气适合玩什么", ((UserMessage) messages.get(0)).singleText());
		assertEquals("那明天呢", ((UserMessage) messages.get(1)).singleText());
		// 历史那条只剩原话；上一轮最终回答沉淀在中间；本轮那条带新检索内容
		assertEquals("杭州的天气适合玩什么", ((UserMessage) messages.get(0)).text());
		assertEquals(ChatMessageType.AI, messages.get(1).type());
		String current = ((UserMessage) messages.get(messages.size() - 1)).text();
		assertTrue(current.contains("那明天呢") && current.contains(secondRag), current);
	}

	@Test
	void defaultStrategyKeepsSystemUserAndPreviousFinalAnswer() {
		EphemeralChatMemory memory = memory();

		memory.add(SystemMessage.from("系统提示"));
		memory.add(UserMessage.from("介绍一下西湖"));
		memory.add(AiMessage.from("西湖是杭州著名景点。"));

		// 第二轮：框架每轮都会重新产出系统提示与用户消息
		memory.add(SystemMessage.from("系统提示"));
		memory.add(UserMessage.from("再问一次"));

		List<ChatMessage> messages = memory.messages();

		// 系统提示 + 第一轮用户原话 + 沉淀下来的最终回答 + 本轮用户消息
		assertEquals(Arrays.asList(
				ChatMessageType.SYSTEM,
				ChatMessageType.USER,
				ChatMessageType.AI,
				ChatMessageType.USER), typesOf(messages));
		assertEquals("北京天气怎么样", ((UserMessage) messages.get(1)).singleText());
	}

	@Test
	void keepsToolRoundTripOfUnfinishedRound() {
		EphemeralChatMemory memory = memory();

		memory.add(UserMessage.from("第一个问题"));
		memory.add(toolCall("北京"));
		memory.add(ToolExecutionResultMessage.from("c1", "queryWeather", "32"));
		memory.add(AiMessage.from("第一个回答"));

		memory.add(UserMessage.from("第二个问题"));
		memory.add(toolCall("杭州"));
		memory.add(ToolExecutionResultMessage.from("c2", "queryWeather", "29"));

		// 本轮尚未收尾（还没有最终回复），工具往返随请求带出；上一轮的往返已被剔除
		assertEquals(Arrays.asList(
				ChatMessageType.USER,
				ChatMessageType.USER,
				ChatMessageType.AI,
				ChatMessageType.TOOL_EXECUTION_RESULT), typesOf(memory.messages()));
		assertEquals("西湖是杭州著名景点。", ((AiMessage) messages.get(2)).text());
		assertEquals("再问一次", ((UserMessage) messages.get(3)).text());
	}

	/**
	 * 默认策略：上一轮调过工具 → 整轮不沉淀（含最终回答）。
	 *
	 * <p>这是为「模型看到现成的工具结果就不再重新调用工具」准备的：天气这类实时数据
	 * 一旦沉淀成历史，下一轮模型会直接复述旧值。</p>
	 */
	@Test
	void defaultStrategyDropsToolDerivedAnswerSoToolIsCalledAgain() {
		EphemeralChatMemory memory = memory();

		memory.add(UserMessage.from("北京天气怎么样"));
		memory.add(toolCall("北京"));
		memory.add(ToolExecutionResultMessage.from("c1", "queryWeather", "32"));
		memory.add(AiMessage.from("北京当前 32℃。"));

		memory.add(UserMessage.from("北京天气怎么样"));

		// 沉淀下来的只有用户原话；工具派生的 32℃ 不进历史
		assertEquals(Arrays.asList(ChatMessageType.USER, ChatMessageType.USER), typesOf(memory.messages()));
	}

	/**
	 * 显式选 {@link AiPersistenceStrategy#PERSIST_LAST_FINAL_ANSWER} 时，
	 * 即使上一轮用过工具，最终回答也会沉淀（旧行为，需业务上接受数据陈旧）。
	 */
	@Test
	void explicitLastFinalAnswerStrategyKeepsToolDerivedAnswer() {
		EphemeralChatMemory memory = memory(AiPersistenceStrategy.PERSIST_LAST_FINAL_ANSWER);

		memory.add(UserMessage.from("北京天气怎么样"));
		memory.add(toolCall("北京"));
		memory.add(ToolExecutionResultMessage.from("c1", "queryWeather", "32"));
		memory.add(AiMessage.from("北京当前 32℃。"));

		memory.add(UserMessage.from("北京天气怎么样"));

		assertEquals(Arrays.asList(ChatMessageType.USER, ChatMessageType.AI, ChatMessageType.USER),
				typesOf(memory.messages()));
	}

	@Test
	void dropAllAiStrategyKeepsOnlySystemAndUser() {
		EphemeralChatMemory memory = memory(AiPersistenceStrategy.DROP_ALL_AI);

		memory.add(SystemMessage.from("系统提示"));
		memory.add(UserMessage.from("北京天气怎么样"));
		memory.add(toolCall("北京"));
		memory.add(ToolExecutionResultMessage.from("c1", "queryWeather", "32"));
		memory.add(AiMessage.from("北京当前 32℃，注意防晒。"));

		memory.add(SystemMessage.from("系统提示"));
		memory.add(UserMessage.from("再问一次"));

		assertEquals(Arrays.asList(
				ChatMessageType.SYSTEM,
				ChatMessageType.USER,
				ChatMessageType.USER), typesOf(memory.messages()));
	}

	@Test
	void dropAllAiStrategyOnlyLastRoundGetsToolMessagesReattached() {
		EphemeralChatMemory memory = memory(AiPersistenceStrategy.DROP_ALL_AI);

		memory.add(UserMessage.from("第一个问题"));
		memory.add(toolCall("北京"));
		memory.add(ToolExecutionResultMessage.from("c1", "queryWeather", "32"));
		memory.add(AiMessage.from("第一个回答"));

		memory.add(UserMessage.from("第二个问题"));
		memory.add(toolCall("杭州"));
		memory.add(ToolExecutionResultMessage.from("c2", "queryWeather", "29"));
		memory.add(AiMessage.from("第二个回答"));

		// 上一轮已收尾：新一轮的首个请求不能带上上一轮的工具往返，
		// 否则模型看到现成结果就直接作答、不再调用工具
		// 只有本轮的 tool 对留下；上一轮的工具往返与回答都不在
		assertEquals(Arrays.asList(
				ChatMessageType.USER,
				ChatMessageType.USER), typesOf(memory.messages()));
	}

	@Test
	void treatsHalfFinishedToolRoundTripAsFinished() {
		EphemeralChatMemory memory = memory();

		// 工具抛错提前中断：工具请求与结果数对不上，不算「进行中」
		memory.add(UserMessage.from("第一个问题"));
		AiMessage twoCalls = AiMessage.from(Arrays.asList(
				ToolExecutionRequest.builder().id("r1").name("queryWeather").arguments("{\"city\":\"北京\"}").build(),
				ToolExecutionRequest.builder().id("r2").name("queryWeather").arguments("{\"city\":\"杭州\"}").build()));
		memory.add(twoCalls);
		memory.add(ToolExecutionResultMessage.from("r1", "queryWeather", "32"));

		memory.add(UserMessage.from("第二个问题"));

		assertEquals(Arrays.asList(
				ChatMessageType.USER,
				ChatMessageType.AI,                     // 本轮 tool call
				ChatMessageType.TOOL_EXECUTION_RESULT), typesOf(memory.messages()));
				ChatMessageType.USER), typesOf(memory.messages()));
	}

	@Test
	void dropAllAiStrategyDropsPlainAiReplyFromHistory() {
		EphemeralChatMemory memory = memory(AiPersistenceStrategy.DROP_ALL_AI);

		memory.add(SystemMessage.from("系统提示"));
		memory.add(UserMessage.from("普通消息"));
		memory.add(AiMessage.from("普通回复"));

		memory.add(UserMessage.from("下一个问题"));

		assertEquals(Arrays.asList(
				ChatMessageType.SYSTEM,
				ChatMessageType.USER,
				ChatMessageType.USER), typesOf(memory.messages()));
	}

	@Test
	void blankAiReplyIsNeverPersisted() {
		EphemeralChatMemory memory = memory(AiPersistenceStrategy.PERSIST_NON_TOOL_FINAL_ANSWER);

		memory.add(SystemMessage.from("系统提示"));
		memory.add(UserMessage.from("普通消息"));
		// 纯空白回复：无有效字符，不带到下一轮
		memory.add(AiMessage.from("   \n\t  "));

		memory.add(UserMessage.from("下一个问题"));

		assertEquals(Arrays.asList(
				ChatMessageType.SYSTEM,
				ChatMessageType.USER,
				ChatMessageType.USER), typesOf(memory.messages()));
	}

	@Test
	void finalAnswerIsPlacedAtTailBeforeCurrentUser() {
		EphemeralChatMemory memory = memory(AiPersistenceStrategy.PERSIST_NON_TOOL_FINAL_ANSWER);

		memory.add(UserMessage.from("第一问"));
		memory.add(AiMessage.from("第一答"));

		memory.add(UserMessage.from("第二问"));

		// 最终回答在「历史用户」与「本轮用户」之间（TAIL）
		assertEquals(Arrays.asList(ChatMessageType.USER, ChatMessageType.AI, ChatMessageType.USER),
				typesOf(memory.messages()));
		assertEquals("第一答", ((AiMessage) memory.messages().get(1)).text());
	}

	@Test
	void positionIsHonoredByStrategy() {
		// 自定义策略：把上一轮最终回答放到 AFTER_SYSTEM（前部）
		AiPersistenceStrategy afterSystem = new AiPersistenceStrategy() {
			@Override
			public List<PersistedAiItem> selectForPersistenceItems(List<ChatMessage> previousTurnMessages) {
				for (ChatMessage m : previousTurnMessages) {
					if (m instanceof AiMessage && !((AiMessage) m).hasToolExecutionRequests()) {
						return Collections.singletonList(PersistedAiItem.builder(m)
								.position(AiPersistencePosition.AFTER_SYSTEM)
								.priority(100)
								.build());
					}
				}
				return Collections.emptyList();
			}
		};
		EphemeralChatMemory memory = memory(afterSystem);

		memory.add(SystemMessage.from("系统提示"));
		memory.add(UserMessage.from("第一问"));
		memory.add(AiMessage.from("第一答"));

		memory.add(UserMessage.from("第二问"));

		// 前部位置：System → AI → 历史 USER → 本轮 USER
		assertEquals(Arrays.asList(
				ChatMessageType.SYSTEM,
				ChatMessageType.AI,
				ChatMessageType.USER,
				ChatMessageType.USER), typesOf(messages));
		assertEquals("普通消息", ((UserMessage) messages.get(1)).singleText());
				ChatMessageType.USER), typesOf(memory.messages()));
	}

	@Test
	void clearDropsEverything() {
		EphemeralChatMemory memory = memory();

		memory.add(UserMessage.from("北京天气怎么样"));
		memory.add(toolCall("北京"));
		memory.add(ToolExecutionResultMessage.from("c1", "queryWeather", "32"));
		memory.clear();

		assertTrue(memory.messages().isEmpty());
	}
}
