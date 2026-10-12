package com.xiongx9527.fw.plus.ai.sample.chat.ai;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
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
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ChatContextLogger} 的单元测试：六类消息的日志路径与 AiServices 参数校验。
 *
 * <p>核心价值在于 onRequest/onResponse 的 instanceof 强转：任何一类消息漏判都会 ClassCastException，
 * 用构造好的消息清单一次性锁死。截断与换行归一化（{@code abbreviate}）虽是私有实现，
 * 但通过挂 {@link ListAppender} 捕获日志事件可以直接断言其效果——工具参数是 pretty-print 的
 * JSON（含换行），漏掉归一化会让一条 TOOL_CALL 撑成多行。</p>
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

	/**
	 * 工具参数走 {@code abbreviate} 归一化：压成单行 + 超长截断。
	 *
	 * <p>两条打印路径都要覆盖——onRequest 里随上下文带上的工具调用，与
	 * onResponse 里本轮模型返回的工具调用，历史上只有后者被修过。</p>
	 */
	@Test
	void toolArgumentsAreFlattenedToSingleLine() {
		ListAppender<ILoggingEvent> appender = new ListAppender<ILoggingEvent>();
		appender.start();
		ch.qos.logback.classic.Logger target =
				(ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ChatContextLogger.class);
		target.addAppender(appender);
		try {
			String prettyArgs = "{\n  \"city\" : \"北京\"\n}";
			AiMessage toolCall = AiMessage.from(ToolExecutionRequest.builder()
					.name("queryWeather").arguments(prettyArgs).build());

			logger.onRequest(new ChatModelRequestContext(ChatModelRequest.builder()
					.model("gemma4")
					.messages(Collections.<ChatMessage>singletonList(toolCall))
					.build(), Collections.emptyMap()));
			logger.onResponse(new ChatModelResponseContext(
					new ChatModelResponse("id", "gemma4", null, null, toolCall),
					emptyRequest(), Collections.emptyMap()));

			// onRequest 还会打「模型请求上下文」与「TOOLS」两行，这里只取 TOOL_CALL
			List<ILoggingEvent> toolCallLogs = appender.list.stream()
					.filter(e -> e.getFormattedMessage().contains("TOOL_CALL"))
					.collect(Collectors.toList());
			assertEquals(2, toolCallLogs.size(), "上下文与响应两条 TOOL_CALL 路径都应产出日志");
			for (ILoggingEvent event : toolCallLogs) {
				String formatted = event.getFormattedMessage();
				assertFalse(formatted.contains("\n"), "日志不应含换行: " + formatted);
				assertTrue(formatted.contains("\"city\" : \"北京\""), "参数内容应保留: " + formatted);
			}
		} finally {
			target.detachAppender(appender);
			appender.stop();
		}
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
	 *
	 * <p>注意：这里裸建代理只为验证参数注解校验；业务侧应经 {@code AssistantBuilder}
	 * 取桥接实现，裸代理不识别多模态（详见 {@link Assistant} javadoc）。</p>
	 */
	@Test
	void assistantMethodsPassParameterValidation() {
		Assistant assistant = AiServices.builder(Assistant.class)
				.chatLanguageModel(messages -> dev.langchain4j.model.output.Response.from(AiMessage.from("pong")))
				.build();

		String reply = assistant.chat("s-1", "",
				Collections.singletonList(dev.langchain4j.data.message.TextContent.from("ping")));

		assertEquals("pong", reply);
	}

	private static ChatModelRequest emptyRequest() {
		return ChatModelRequest.builder()
				.model("gemma4")
				.messages(Collections.<ChatMessage>emptyList())
				.build();
	}
}
