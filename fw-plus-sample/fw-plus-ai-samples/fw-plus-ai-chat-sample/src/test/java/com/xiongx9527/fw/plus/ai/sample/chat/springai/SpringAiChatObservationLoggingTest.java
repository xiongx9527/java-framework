package com.xiongx9527.fw.plus.ai.sample.chat.springai;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.observation.ChatModelCompletionObservationHandler;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.chat.observation.ChatModelPromptContentObservationHandler;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spring AI 原生模型层监听验证：{@code spring.ai.chat.observations.log-prompt /
 * log-completion} 开关（application.yaml）打开后——
 *
 * <ul>
 *   <li>官方 {@link ChatModelPromptContentObservationHandler} /
 *       {@link ChatModelCompletionObservationHandler} 被自动配置注册；</li>
 *   <li>handler 接进了 {@link ObservationRegistry}（模型调用触发 observation 时
 *       打印发给模型的 prompt 与模型回复，Ollama / DashScope 两条链路共用此机制）。</li>
 * </ul>
 *
 * <p>测试通过 {@link ObservationRegistry} 手动触发一次模型层 observation 来验证，
 * 不依赖真实模型端点；与 LangChain4j 链路的 ChatContextLogger 测试思路等价。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"spring.ai.dashscope.api-key=", "spring.ai.dashscope.enabled=false"})
class SpringAiChatObservationLoggingTest {

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private ObservationRegistry observationRegistry;

	@Test
	void officialLogHandlersRegisteredByConfigSwitch() {
		// 开关生效的直接证据：@ConditionalOnProperty（spring.ai.chat.observations.log-prompt /
		// log-completion）条件满足时自动配置才会创建这两个 handler bean
		assertNotNull(applicationContext.getBean(ChatModelPromptContentObservationHandler.class, false),
				"log-prompt 开关应注册 ChatModelPromptContentObservationHandler");
		assertNotNull(applicationContext.getBean(ChatModelCompletionObservationHandler.class, false),
				"log-completion 开关应注册 ChatModelCompletionObservationHandler");
	}

	@Test
	void promptLoggedWhenModelObservationStops() {
		Logger handlerLogger = (Logger) LoggerFactory.getLogger(ChatModelPromptContentObservationHandler.class);
		ListAppender<ILoggingEvent> appender = capture(handlerLogger);
		try {
			ChatModelObservationContext context = ChatModelObservationContext.builder()
					.prompt(new Prompt(Collections.singletonList(new UserMessage("观测测试消息"))))
					.provider("test")
					.build();

			// 与模型真实调用同一条路径：observation start -> stop 触发已注册的 handler
			Observation.start("test.chat.prompt", () -> context, observationRegistry).stop();

			assertTrue(appender.list.stream()
							.anyMatch(event -> event.getFormattedMessage().contains("观测测试消息")),
					"模型层 observation 结束应打印发给模型的 prompt（log-prompt 开关生效）");
		} finally {
			detach(handlerLogger, appender);
		}
	}

	@Test
	void completionLoggedWhenModelObservationStops() {
		Logger handlerLogger = (Logger) LoggerFactory.getLogger(ChatModelCompletionObservationHandler.class);
		ListAppender<ILoggingEvent> appender = capture(handlerLogger);
		try {
			ChatModelObservationContext context = ChatModelObservationContext.builder()
					.prompt(new Prompt(Collections.singletonList(new UserMessage("任意问题"))))
					.provider("test")
					.build();
			context.setResponse(new ChatResponse(
					Collections.singletonList(new Generation(new AssistantMessage("模型回复文本")))));

			Observation.start("test.chat.completion", () -> context, observationRegistry).stop();

			assertTrue(appender.list.stream()
							.anyMatch(event -> event.getFormattedMessage().contains("模型回复文本")),
					"模型层 observation 结束应打印模型回复（log-completion 开关生效）");
		} finally {
			detach(handlerLogger, appender);
		}
	}

	/** 挂上日志捕获器（INFO 级别由 handler 自身决定，不改 logger 级别）。 */
	private ListAppender<ILoggingEvent> capture(Logger logger) {
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
		return appender;
	}

	private void detach(Logger logger, ListAppender<ILoggingEvent> appender) {
		logger.detachAppender(appender);
		appender.stop();
	}
}
