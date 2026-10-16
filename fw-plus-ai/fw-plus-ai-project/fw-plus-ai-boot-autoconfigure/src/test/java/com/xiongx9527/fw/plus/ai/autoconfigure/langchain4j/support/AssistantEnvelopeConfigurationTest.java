package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support;

import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.Assistant;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.SimpleAssistant;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope.BusinessContract;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope.BusinessRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope.ChatEnvelope;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope.EnvelopeRuntime;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope.EnvelopeSessionMemory;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope.Langchain4jAssistantProperties;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 信封编排的 Spring 装配：开关未开启不注册也不安装（既有一字不变）；开启后注册 {@link EnvelopeRuntime}
 * 并把它装进容器里既有的 {@link SimpleAssistant}；模型/存储缺失时启动快速失败。
 *
 * <p>「是否已装上处理器」用可观测行为断言：装上以后既有无 mapper 流式入口会先做一次业务分类调用
 * （{@link ChatModel} 的请求记录即为证据），未装则一次都不调用。</p>
 */
class AssistantEnvelopeConfigurationTest {

	private static final String PREFIX = Langchain4jAssistantProperties.PREFIX;

	private static final String ORDER_SCHEMA = "{"
			+ "\"type\":\"object\","
			+ "\"properties\":{\"orderId\":{\"type\":\"string\"}},"
			+ "\"required\":[\"orderId\"]}";

	private final ScriptedChatModel model = new ScriptedChatModel();

	private final SimpleAssistant simple = new SimpleAssistant(new FakeAssistant());

	private ApplicationContextRunner runner() {
		return new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(LangChain4jSupportAutoConfiguration.class))
				.withBean(ChatModel.class, () -> model)
				.withBean(SimpleAssistant.class, () -> simple)
				.withBean(BusinessContract.class, AssistantEnvelopeConfigurationTest::order);
	}

	@Test
	void switchOffRegistersNothingAndInstallsNothing() {
		runner().run(context -> {
			assertThat(context).doesNotHaveBean(EnvelopeRuntime.class);

			// 未安装处理器：既有无 mapper 入口走原路径，不产生任何分类调用
			context.getBean(SimpleAssistant.class).streamToSse("s-1", "讲个故事");
			assertThat(model.requests).isEmpty();
		});
	}

	@Test
	void switchOnRegistersRuntimeAndInstallsItIntoExistingAssistant() {
		runner().withPropertyValues(PREFIX + ".envelope.enabled=true").run(context -> {
			assertThat(context).hasSingleBean(EnvelopeRuntime.class);

			context.getBean(SimpleAssistant.class).streamToSse("s-1", "讲个故事");
			assertThat(model.requests).as("分类调用发生 → 处理器已装进既有 SimpleAssistant").hasSize(1);
		});
	}

	@Test
	void switchOnCollectsContractBeansIntoRegistry() {
		model.replies.addAll(Arrays.asList("{\"business\":\"order\"}", "{\"orderId\":\"A1\"}"));

		runner().withPropertyValues(PREFIX + ".envelope.enabled=true").run(context -> {
			context.getBean(SimpleAssistant.class).streamToSse("s-1", "帮我查订单 A1");

			// 分类 1 次 + 结构化生成 1 次：契约 Bean 被收进注册表（否则不会命中 order）
			assertThat(model.requests).hasSize(2);
			List<ChatMessage> messages = context.getBean(ChatMemoryStore.class).getMessages("s-1");
			assertThat(messages).hasSize(2);
			assertThat(((AiMessage) messages.get(1)).text()).isEqualTo(EnvelopeSessionMemory.summarize(
					ChatEnvelope.structured("order", JSONObject.parseObject("{\"orderId\":\"A1\"}"))));
		});
	}

	@Test
	void switchOnWithoutAssistantBeanIsHarmless() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(LangChain4jSupportAutoConfiguration.class))
				.withBean(ChatModel.class, () -> model)
				.withPropertyValues(PREFIX + ".envelope.enabled=true")
				.run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context).hasSingleBean(EnvelopeRuntime.class);
				});
	}

	@Test
	void switchOnWithoutChatModelFailsFast() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(LangChain4jSupportAutoConfiguration.class))
				.withPropertyValues(PREFIX + ".envelope.enabled=true")
				.run(context -> assertThat(context).getFailure()
						.hasRootCauseInstanceOf(IllegalStateException.class)
						.hasStackTraceContaining("ChatModel"));
	}

	@Test
	void switchOnWithoutMemoryStoreFailsFast() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(
						LangChain4jSupportAutoConfiguration.AssistantEnvelopeConfiguration.class))
				.withBean(ChatModel.class, () -> model)
				.withPropertyValues(PREFIX + ".envelope.enabled=true")
				.run(context -> assertThat(context).getFailure()
						.hasRootCauseInstanceOf(IllegalStateException.class)
						.hasStackTraceContaining("ChatMemoryStore"));
	}

	@Test
	void customRuntimeBeanWins() {
		EnvelopeRuntime custom = new EnvelopeRuntime(model, new InMemoryChatMemoryStore(),
				new BusinessRegistry(), new Langchain4jAssistantProperties());

		runner().withPropertyValues(PREFIX + ".envelope.enabled=true")
				.withBean(EnvelopeRuntime.class, () -> custom)
				.run(context -> {
					assertThat(context).hasSingleBean(EnvelopeRuntime.class);
					assertThat(context.getBean(EnvelopeRuntime.class)).isSameAs(custom);
				});
	}

	private static BusinessContract order() {
		return BusinessContract.builder("order").schema(ORDER_SCHEMA).structured(true).build();
	}

	/** 按脚本回放的对话模型替身；脚本用尽后返回空串（便于用请求数识别多余调用） */
	private static final class ScriptedChatModel implements ChatModel {

		private final List<List<ChatMessage>> requests = new ArrayList<>();
		private final Deque<String> replies = new ArrayDeque<>();

		@Override
		public ChatResponse doChat(ChatRequest request) {
			requests.add(new ArrayList<>(request.messages()));
			return ChatResponse.builder()
					.aiMessage(AiMessage.from(replies.isEmpty() ? "" : replies.poll()))
					.build();
		}
	}

	/** 既有对话通道替身：逐字回调一次后完成 */
	private static final class FakeAssistant implements Assistant {

		@Override
		public String chat(String memoryId, Object context, List<Content> contents) {
			return "回复";
		}

		@Override
		public TokenStream stream(String memoryId, Object context, List<Content> contents) {
			return new TokenStream() {

				private final List<Consumer<String>> nextHandlers = new ArrayList<>();
				private Consumer<ChatResponse> completeHandler;

				@Override
				public TokenStream onPartialResponse(Consumer<String> handler) {
					nextHandlers.add(handler);
					return this;
				}

				@Override
				public TokenStream onPartialResponseWithContext(
						java.util.function.BiConsumer<dev.langchain4j.model.chat.response.PartialResponse,
								dev.langchain4j.model.chat.response.PartialResponseContext> handler) {
					return onPartialResponse(text -> handler.accept(
							new dev.langchain4j.model.chat.response.PartialResponse(text), null));
				}

				@Override
				public TokenStream onPartialThinkingWithContext(
						java.util.function.BiConsumer<dev.langchain4j.model.chat.response.PartialThinking,
								dev.langchain4j.model.chat.response.PartialThinkingContext> handler) {
					return this;
				}

				@Override
				public TokenStream beforeToolExecution(
						Consumer<dev.langchain4j.service.tool.BeforeToolExecution> handler) {
					return this;
				}

				@Override
				public TokenStream onRetrieved(Consumer<List<dev.langchain4j.rag.content.Content>> handler) {
					return this;
				}

				@Override
				public TokenStream onToolExecuted(Consumer<dev.langchain4j.service.tool.ToolExecution> handler) {
					return this;
				}

				@Override
				public TokenStream onCompleteResponse(Consumer<ChatResponse> handler) {
					completeHandler = handler;
					return this;
				}

				@Override
				public TokenStream onError(Consumer<Throwable> handler) {
					return this;
				}

				@Override
				public TokenStream ignoreErrors() {
					return this;
				}

				@Override
				public void start() {
					for (Consumer<String> handler : nextHandlers) {
						handler.accept("你好");
					}
					if (completeHandler != null) {
						completeHandler.accept(ChatResponse.builder()
								.aiMessage(AiMessage.from("你好"))
								.build());
					}
				}
			};
		}
	}
}
