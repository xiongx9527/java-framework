package com.xiongx9527.fw.plus.ai.sample.chat.springai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spring AI 版 {@link EphemeralChatMemory} 契约测试：模拟 {@code MessageChatMemoryAdvisor}
 * 的真实时序（每轮 before 先 {@code get} 再 {@code add(user)}、after {@code add(ai)}），
 * 验证「上一轮输出按策略沉淀 / RAG 与工具往返不落记忆 / 读取与持久两视图一致」。
 *
 * <p>与 langchain4j 链路的 {@code EphemeralChatMemoryTest} 对应；本测试纯单元、不启 Spring。</p>
 */
class EphemeralChatMemoryTest {

	private static ChatMemory window() {
		return MessageWindowChatMemory.builder().maxMessages(20).build();
	}

	private static List<String> texts(List<Message> messages) {
		return messages.stream().map(Message::getText).toList();
	}

	private static AssistantMessage toolCallAi() {
		return AssistantMessage.builder()
				.content("")
				.toolCalls(Collections.singletonList(
						new AssistantMessage.ToolCall("call-1", "function", "getWeather", "{\"city\":\"杭州\"}")))
				.build();
	}

	private static ToolResponseMessage toolResponse() {
		return ToolResponseMessage.builder()
				.responses(Collections.singletonList(
						new ToolResponseMessage.ToolResponse("call-1", "getWeather", "28度")))
				.build();
	}

	@Test
	void persistsSelectedOutputOnNewRound() {
		ChatMemory memory = EphemeralChatMemory.builder(window()).build();

		// 轮1 before：空记忆
		assertTrue(memory.get("s").isEmpty());
		memory.add("s", new UserMessage("杭州有什么景点"));
		memory.add("s", new AssistantMessage("杭州有西湖十景。"));

		// 轮2 before：输出段按默认策略回放（非工具最终回答）
		assertEquals(List.of("杭州有什么景点", "杭州有西湖十景。"), texts(memory.get("s")));

		// 轮2继续：新 user 触发固化，之后读取视图与持久视图一致
		memory.add("s", new UserMessage("天气呢"));
		memory.add("s", new AssistantMessage("28度。"));

		assertEquals(List.of("杭州有什么景点", "杭州有西湖十景。", "天气呢", "28度。"),
				texts(memory.get("s")));
	}

	@Test
	void dropsToolRoundTripTurnByDefaultStrategy() {
		ChatMemory memory = EphemeralChatMemory.builder(window()).build();

		memory.add("s", new UserMessage("杭州天气"));
		memory.add("s", toolCallAi());
		memory.add("s", toolResponse());
		memory.add("s", new AssistantMessage("28度。"));

		// 读取：工具消息默认随本轮开关回放，但工具派生的结论不回放（默认策略判定整轮含工具往返）
		List<Message> round2 = memory.get("s");
		assertEquals("杭州天气", round2.get(0).getText());
		assertTrue(round2.stream().anyMatch(m -> m instanceof ToolResponseMessage), "工具结果默认回放");
		assertFalse(texts(round2).contains("28度。"), "工具派生结论不回放（会过期，促使模型重查）");

		// 固化：工具轮整轮不沉淀——持久历史只剩用户原话
		memory.add("s", new UserMessage("再想想"));
		memory.add("s", new AssistantMessage("好的。"));
		List<Message> round3 = memory.get("s");
		assertFalse(texts(round3).contains("28度。"));
		assertFalse(round3.stream().anyMatch(m -> m instanceof ToolResponseMessage),
				"工具往返不沉淀进持久历史");
	}

	@Test
	void dropAllAiStrategyKeepsOnlyUserMessages() {
		ChatMemory memory = EphemeralChatMemory.builder(window())
				.aiPersistenceStrategy(AiPersistenceStrategy.DROP_ALL_AI)
				.build();

		memory.add("s", new UserMessage("问题一"));
		memory.add("s", new AssistantMessage("回答一"));
		assertEquals(List.of("问题一"), texts(memory.get("s")), "DROP_ALL_AI：AI 不回放");

		memory.add("s", new UserMessage("问题二"));
		memory.add("s", new AssistantMessage("回答二"));
		assertEquals(List.of("问题一", "问题二"), texts(memory.get("s")), "持久历史只剩用户原话");
	}

	@Test
	void blankAssistantNotPersisted() {
		ChatMemory memory = EphemeralChatMemory.builder(window()).build();

		memory.add("s", new UserMessage("问题"));
		memory.add("s", new AssistantMessage("   "));
		memory.add("s", new UserMessage("下一个"));

		assertEquals(List.of("问题", "下一个"), texts(memory.get("s")), "无有效字符的回复不沉淀");
	}

	@Test
	void stripRagFromHistoryOnPersist() {
		ChatMemory memory = EphemeralChatMemory.builder(window())
				.ephemeralSeparators("\n\n参考资料：\n")
				.build();

		// 模拟历史 user 被注入模板污染（后缀注入形态）
		memory.add("s", new UserMessage("原始问题\n\n参考资料：\n片段内容"));
		memory.add("s", new AssistantMessage("回复"));
		memory.add("s", new UserMessage("下一个问题")); // 触发固化

		assertEquals("原始问题", memory.get("s").get(0).getText(), "固化时历史 user 应剥离检索段");
	}

	@Test
	void extractsQueryFromContextualTemplate() {
		ChatMemory memory = EphemeralChatMemory.builder(window()).build();
		String template = "Context information is below.\n\n--------------------\n西湖十景。\n"
				+ "--------------------\n\nGiven the context information and no prior knowledge,"
				+ " answer the query.\n\nQuery: 杭州有什么景点\n\nAnswer:";

		// 模拟本轮 user 带 Spring AI 官方 ContextualQueryAugmenter 前缀模板
		memory.add("s", new UserMessage(template));
		memory.add("s", new AssistantMessage("杭州有西湖十景。"));
		memory.add("s", new UserMessage("下一个问题"));

		assertEquals("杭州有什么景点", memory.get("s").get(0).getText(),
				"前缀模板应从 Query 段提取用户原话");
	}

	@Test
	void toolMessagesFilteredByCurrentTurnSwitches() {
		ChatMemory memory = EphemeralChatMemory.builder(window())
				.sendToolRequestsInCurrentTurn(false)
				.sendToolResultsInCurrentTurn(false)
				.build();

		memory.add("s", new UserMessage("杭州天气"));
		memory.add("s", toolCallAi());
		memory.add("s", toolResponse());
		memory.add("s", new AssistantMessage("28度。"));

		// 开关关闭：工具中间步与结果都不回放；默认策略下结论也不回放
		assertEquals(List.of("杭州天气"), texts(memory.get("s")));
	}

	@Test
	void includeCurrentUserSwitchControlsLastUserOnRead() {
		ChatMemory memory = EphemeralChatMemory.builder(window())
				.ephemeralSeparators("\n\n参考资料：\n")
				.includeCurrentUserWithEphemeral(false)
				.build();

		memory.add("s", new UserMessage("问题\n\n参考资料：\n片段"));
		memory.add("s", new AssistantMessage("回复"));

		// 开关 false：最后一条 user 读取时也剥离检索段
		assertEquals("问题", memory.get("s").get(0).getText());

		// 默认 true 的对照：保留原样
		ChatMemory defaultMemory = EphemeralChatMemory.builder(window())
				.ephemeralSeparators("\n\n参考资料：\n")
				.build();
		defaultMemory.add("s", new UserMessage("问题\n\n参考资料：\n片段"));
		defaultMemory.add("s", new AssistantMessage("回复"));
		assertEquals("问题\n\n参考资料：\n片段", defaultMemory.get("s").get(0).getText());
	}
}
