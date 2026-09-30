package com.xiongx9527.fw.plus.ai.sample.chat.ai;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.Assistant;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.logging.ChatContextLogger;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelRequest;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponse;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ChatContextLogger} 的单元测试：六类消息的日志路径与 AiServices 参数校验。
 *
 * <p>核心价值在于 onRequest/onResponse 的 instanceof 强转：任何一类消息漏判都会 ClassCastException，
 * 用构造好的消息清单一次性锁死。内部截断实现（{@code abbreviate}）是私有的，不在此断言。</p>
 */
class ChatContextLoggerTest {

	private final ChatContextLogger logger = new ChatContextLogger();

	@Test
	void onRequestLogsEveryMessageTypeWithoutClassCast() {
		List<ChatMessage> messages = Arrays.asList(
				SystemMessage.from("你是 fw-plus 内置智能助手"),
				UserMessage.from("杭州天气怎么样"),
				UserMessage.from("杭州天气怎么样"),
				AiMessage.from(ToolExecutionRequest.builder()
						.name("queryWeather").arguments("{\"city\":\"杭州\"}").build()),
				AiMessage.from("上一轮回复"),
				ToolExecutionResultMessage.from("id-1", "queryWeather", "{\"temperatureC\":29}")
		);
		ChatModelRequest request = ChatModelRequest.builder()
				.model("gemma4")
				.messages(messages)
				.toolSpecifications(Collections.emptyList())
				.build();

		logger.onRequest(new ChatModelRequestContext(request, Collections.emptyMap()));
	}

	@Test
	void onResponseLogsToolCallAndPlainText() {
		ChatModelRequest request = emptyRequest();

		ChatModelResponse toolCall = new ChatModelResponse("id", "gemma4", null, null,
				AiMessage.from(ToolExecutionRequest.builder()
						.name("queryWeather").arguments("{}").build()));
		logger.onResponse(new ChatModelResponseContext(toolCall, request, Collections.emptyMap()));

		ChatModelResponse plain = new ChatModelResponse("id", "gemma4", null, null, AiMessage.from("回复内容"));
		logger.onResponse(new ChatModelResponseContext(plain, request, Collections.emptyMap()));
	}

	@Test
	void onErrorDoesNotThrow() {
		ChatModelErrorContext context = new ChatModelErrorContext(new RuntimeException("boom"),
				emptyRequest(), null, Collections.emptyMap());

		logger.onError(context);
	}

	/**
	 * 真实走一遍 AiServices 代理：{@code validateParameters} 要求每个参数都有注解，
	 * 裸参数只在调用时才抛 IllegalConfigurationException——mock Assistant 的测试发现不了。
	 */
	@Test
	void assistantMethodsPassParameterValidation() {
		Assistant assistant = AiServices.builder(Assistant.class)
				.chatLanguageModel(messages -> dev.langchain4j.model.output.Response.from(AiMessage.from("pong")))
				.build();

		String reply = assistant.chat("s-1", "", "ping");

		assertEquals("pong", reply);
	}

	private static ChatModelRequest emptyRequest() {
		return ChatModelRequest.builder()
				.model("gemma4")
				.messages(Collections.<ChatMessage>emptyList())
				.build();
	}
}
