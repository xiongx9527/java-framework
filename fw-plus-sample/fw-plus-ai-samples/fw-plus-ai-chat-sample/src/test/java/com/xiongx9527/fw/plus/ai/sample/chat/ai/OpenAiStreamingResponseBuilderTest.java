package com.xiongx9527.fw.plus.ai.sample.chat.ai;

import dev.ai4j.openai4j.chat.ChatCompletionChoice;
import dev.ai4j.openai4j.chat.ChatCompletionResponse;
import dev.ai4j.openai4j.chat.Delta;
import dev.ai4j.openai4j.chat.FunctionCall;
import dev.ai4j.openai4j.chat.ToolCall;
import dev.ai4j.openai4j.shared.Usage;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.openai.OpenAiStreamingResponseBuilder;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 修复版流式累积器单测：核心是 Ollama 同帧 content + toolCalls 不再被丢弃，
 * {@code build()} 永不返回 null（官方 0.35.0 在该场景返回 null，
 * 挂 listener 时抛 {@code response cannot be null}）。标准分帧场景一并提供回归。
 */
class OpenAiStreamingResponseBuilderTest {

	/** Ollama 同帧格式：content:"" 与 tool_calls 同帧，arguments 分多帧增量到达 */
	@Test
	void appendsToolCallsArrivingInSameFrameAsContent() {
		OpenAiStreamingResponseBuilder builder = new OpenAiStreamingResponseBuilder();

		// 首帧：role + content:"" + toolCalls[id/name 同帧]（Ollama 实测行为）
		builder.append(response(
				Delta.builder().role(dev.ai4j.openai4j.chat.Role.ASSISTANT).content("")
						.toolCalls(Collections.singletonList(ToolCall.builder()
								.index(0).id("call_abc123")
								.function(FunctionCall.builder().name("queryWeather").arguments("").build())
								.build()))
						.build()));
		// 增量帧：arguments 分片，同样带 content:""
		builder.append(response(
				Delta.builder().content("")
						.toolCalls(Collections.singletonList(ToolCall.builder()
								.index(0)
								.function(FunctionCall.builder().arguments("{\"city\":\"北京\"}").build())
								.build()))
						.build()));
		// 收尾帧
		builder.append(response(choice(Delta.builder().content("").build(), "tool_calls")));

		Response<AiMessage> response = builder.build();

		assertNotNull(response, "build() 不得返回 null（官方 0.35.0 在此场景返回 null 导致报错）");
		AiMessage aiMessage = response.content();
		assertTrue(aiMessage.hasToolExecutionRequests());
		ToolExecutionRequest request = aiMessage.toolExecutionRequests().get(0);
		assertEquals("call_abc123", request.id());
		assertEquals("queryWeather", request.name());
		assertEquals("{\"city\":\"北京\"}", request.arguments());
		assertEquals(FinishReason.TOOL_EXECUTION, response.finishReason());
	}

	/** 多工具调用按 index 有序聚合：index 1 先到、index 0 后到，输出仍按 0,1 排序（官方 ConcurrentHashMap 无序） */
	@Test
	void aggregatesMultipleToolCallsInIndexOrder() {
		OpenAiStreamingResponseBuilder builder = new OpenAiStreamingResponseBuilder();

		builder.append(response(Delta.builder().content("")
				.toolCalls(Collections.singletonList(ToolCall.builder()
						.index(1).id("call_2")
						.function(FunctionCall.builder().name("queryWeather")
								.arguments("{\"city\":\"上海\"}").build())
						.build()))
				.build()));
		builder.append(response(Delta.builder().content("")
				.toolCalls(Collections.singletonList(ToolCall.builder()
						.index(0).id("call_1")
						.function(FunctionCall.builder().name("queryWeather")
								.arguments("{\"city\":\"北京\"}").build())
						.build()))
				.build()));

		Response<AiMessage> response = builder.build();

		List<ToolExecutionRequest> requests = response.content().toolExecutionRequests();
		assertEquals(2, requests.size());
		assertEquals("call_1", requests.get(0).id());
		assertEquals("{\"city\":\"北京\"}", requests.get(0).arguments());
		assertEquals("call_2", requests.get(1).id());
		assertEquals("{\"city\":\"上海\"}", requests.get(1).arguments());
	}

	/** 标准 OpenAI 分帧格式回归：纯 content 多帧 → 纯文本 AiMessage */
	@Test
	void buildsPlainContentFromSplitFrames() {
		OpenAiStreamingResponseBuilder builder = new OpenAiStreamingResponseBuilder();

		builder.append(response(Delta.builder().content("今天").build()));
		builder.append(response(Delta.builder().content("晴，26 度。").build()));
		builder.append(response(choice(Delta.builder().content("").build(), "stop")));
		builder.append(usageFrame());

		Response<AiMessage> response = builder.build();

		assertEquals("今天晴，26 度。", response.content().text());
		assertFalse(response.content().hasToolExecutionRequests());
		assertEquals(FinishReason.STOP, response.finishReason());
		TokenUsage tokenUsage = response.tokenUsage();
		assertNotNull(tokenUsage);
		assertEquals(495L, tokenUsage.inputTokenCount().longValue());
		assertEquals(240L, tokenUsage.outputTokenCount().longValue());
		assertEquals(735L, tokenUsage.totalTokenCount().longValue());
	}

	/** 只有 usage 帧的异常流：官方返回 null，本实现兜底返回空文本 AiMessage */
	@Test
	void buildsEmptyMessageForUsageOnlyStream() {
		OpenAiStreamingResponseBuilder builder = new OpenAiStreamingResponseBuilder();
		builder.append(usageFrame());

		Response<AiMessage> response = builder.build();

		assertNotNull(response, "纯 usage 帧也不得返回 null");
		assertEquals("", response.content().text());
	}

	// ---- 帧构造辅助 ----

	/** 单 choice 的流式帧 */
	private static ChatCompletionResponse response(Delta delta) {
		return response(choice(delta, null));
	}

	private static ChatCompletionResponse response(ChatCompletionChoice choice) {
		return ChatCompletionResponse.builder()
				.id("chatcmpl-test")
				.model("test-model")
				.choices(Collections.singletonList(choice))
				.build();
	}

	private static ChatCompletionChoice choice(Delta delta, String finishReason) {
		return ChatCompletionChoice.builder()
				.index(0)
				.delta(delta)
				.finishReason(finishReason)
				.build();
	}

	/** usage 帧：choices 为空，只带 token 用量（streamOptions.include_usage 开启时流末尾出现） */
	private static ChatCompletionResponse usageFrame() {
		return ChatCompletionResponse.builder()
				.id("chatcmpl-test")
				.model("test-model")
				.choices(Collections.emptyList())
				.usage(Usage.builder()
						.promptTokens(495).completionTokens(240).totalTokens(735)
						.build())
				.build();
	}
}
