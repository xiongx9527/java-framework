package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatResponse;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatStreamChunk;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.official.TinyEngineUpstreamException;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.official.TinyEngineOfficialAiController;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.official.TinyEngineOfficialAiWebFluxController;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.provider.TinyEngineAiCapability;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.provider.TinyEngineAiProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.provider.TinyEngineAiProviderRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag.TinyEngineRagRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag.TinyEngineRagSearchResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.service.TinyEngineAiChatService;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.service.TinyEngineRagService;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.TinyEngineAutoConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webflux.TinyEngineReactiveExecutor;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineRequestUserScope;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AiModelInvocationConfig;
import com.xiongx9527.fw.plus.tinyengine.service.AppAiModelConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TinyEngineAiWebFluxAutoConfigurationTest {

	private final WebApplicationContextRunner servletRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(WebMvcAutoConfiguration.class, TinyEngineAiAutoConfiguration.class))
			.withBean(TinyEngineAiChatService.class, TestChatService::new)
			.withBean(TinyEngineRagService.class, TestRagService::new)
			.withPropertyValues("fw.plus.tiny-engine.ai.web.enabled=true");

	private final ReactiveWebApplicationContextRunner reactiveRunner = new ReactiveWebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(WebFluxAutoConfiguration.class, TinyEngineAiAutoConfiguration.class))
			.withBean(TinyEngineAiChatService.class, TestChatService::new)
			.withBean(TinyEngineRagService.class, TestRagService::new)
			.withBean(TinyEngineReactiveExecutor.class, TinyEngineAiWebFluxAutoConfigurationTest::executor)
			.withPropertyValues("fw.plus.tiny-engine.ai.web.enabled=true");

	@Test
	void servletApplicationRegistersOnlyServletTinyEngineAiController() {
		servletRunner.run(context -> {
			assertThat(context).hasSingleBean(TinyEngineAiController.class);
			assertThat(context).doesNotHaveBean(TinyEngineAiWebFluxController.class);
			assertThat(context).doesNotHaveBean(TinyEngineOfficialAiController.class);
			assertThat(context).doesNotHaveBean(TinyEngineOfficialAiWebFluxController.class);
		});
	}

	@Test
	void servletApplicationRegistersControllerWhenOnlyChatIsEnabled() {
		new WebApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(WebMvcAutoConfiguration.class,
						TinyEngineAiAutoConfiguration.class))
				.withBean(TinyEngineAiChatService.class, TestChatService::new)
				.withPropertyValues("fw.plus.tiny-engine.ai.web.enabled=true")
				.run(context -> assertThat(context).hasSingleBean(TinyEngineAiController.class));
	}

	@Test
	void reactiveApplicationRegistersOnlyWebFluxTinyEngineAiController() {
		reactiveRunner.run(context -> {
			assertThat(context).hasSingleBean(TinyEngineAiWebFluxController.class);
			assertThat(context).doesNotHaveBean(TinyEngineAiController.class);
			assertThat(context).doesNotHaveBean(TinyEngineOfficialAiController.class);
			assertThat(context).doesNotHaveBean(TinyEngineOfficialAiWebFluxController.class);
		});
	}

	@Test
	void aiAutoConfigurationRunsAfterTinyEngineBootAutoConfiguration() {
		AutoConfigureAfter order = TinyEngineAiAutoConfiguration.class.getAnnotation(AutoConfigureAfter.class);
		assertThat(order).isNotNull();
		assertThat(order.value()).contains(TinyEngineAutoConfiguration.class);
	}

	@Test
	void chatRequestAcceptsOpenAiCompatibleSnakeCaseGenerationParameters() throws Exception {
		TinyEngineChatRequest request = new ObjectMapper().readValue(
				"{\"appId\":2,\"serviceId\":10,\"modelName\":\"test-model\","
						+ "\"max_tokens\":128,\"top_p\":0.8,\"presence_penalty\":0.3,"
						+ "\"frequency_penalty\":0.1,\"response_format\":{\"type\":\"json_object\"},"
						+ "\"enable_thinking\":true,\"parallel_tool_calls\":false}",
				TinyEngineChatRequest.class);

		assertThat(request.getMaxTokens()).isEqualTo(Integer.valueOf(128));
		assertThat(request.getTopP()).isEqualTo(Double.valueOf(0.8D));
		assertThat(request.getPresencePenalty()).isEqualTo(Double.valueOf(0.3D));
		assertThat(request.getFrequencyPenalty()).isEqualTo(Double.valueOf(0.1D));
		assertThat(request.getResponseFormat()).isEqualTo("json_object");
		assertThat(request.getEnableThinking()).isTrue();
		assertThat(request.getParallelToolCalls()).isFalse();
	}

	@Test
	void defaultChatServiceResolvesServerSideCredentialsAndIgnoresLegacyClientCredentials() throws Exception {
		AppAiModelConfigService configService = mock(AppAiModelConfigService.class);
		TinyEngineAiProvider provider = mock(TinyEngineAiProvider.class);
		when(provider.getKey()).thenReturn("test");
		when(provider.supports(TinyEngineAiCapability.CHAT)).thenReturn(true);
		when(provider.chat(org.mockito.ArgumentMatchers.any(TinyEngineChatRequest.class)))
				.thenReturn(new TinyEngineChatResponse("ok"));
		AiModelInvocationConfig resolved = invocation("https://example.invalid/v1", "test-key");
		when(configService.resolveInvocation(2, 10L, "test-model")).thenReturn(resolved);
		assertThat(new ObjectMapper().writeValueAsString(resolved)).isEqualTo("{}");
		TinyEngineAiProperties properties = new TinyEngineAiProperties();
		StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
		beanFactory.addBean("configService", configService);
		TinyEngineAiChatService chatService = new TinyEngineAiAutoConfiguration().tinyEngineAiChatService(
				new TinyEngineAiProviderRegistry(Collections.singletonList(provider), "test"), properties,
				beanFactory.getBeanProvider(AppAiModelConfigService.class));
		TinyEngineChatRequest request = new ObjectMapper().readValue(
				"{\"appId\":2,\"serviceId\":10,\"modelName\":\"test-model\",\"model\":\"attacker-model\",\"baseUrl\":\"https://attacker.invalid\",\"apiKey\":\"attacker-key\",\"modelSettings\":{\"temperature\":1}}",
				TinyEngineChatRequest.class);
		assertThat(new ObjectMapper().writeValueAsString(request)).doesNotContain("baseUrl", "apiKey", "modelSettings");

		chatService.chat(request);

		verify(configService).resolveInvocation(2, 10L, "test-model");
		verify(provider).chat(argThat(it -> "test-model".equals(it.getModel())
				&& "https://example.invalid/v1".equals(it.getBaseUrl())
				&& "test-key".equals(it.getApiKey())
				&& Double.valueOf(0.2D).equals(it.getModelSettings().get("temperature"))));
	}

	@Test
	void webFluxControllerConvertsUpstreamAuthorizationFailureToModelInvocationError() {
		TinyEngineAiWebFluxController controller = new TinyEngineAiWebFluxController(
				new FailingChatService(), new TestRagService(), executor());
		TinyEngineChatRequest request = new TinyEngineChatRequest();
		request.setAppId(Integer.valueOf(2));
		request.setServiceId(Long.valueOf(10));
		request.setModelName("test-model");

		org.springframework.web.server.ResponseStatusException exception = assertThrows(
				org.springframework.web.server.ResponseStatusException.class, () -> controller.chat(request).block());
		assertEquals(org.springframework.http.HttpStatus.BAD_GATEWAY, exception.getStatus());
		assertEquals("TinyEngine AI model invocation failed", exception.getReason());
	}

	@Test
	void webFluxControllerReturnsOpenAiCompatibleSseFramesOverHttp() {
		final TinyEngineChatRequest[] seenRequest = new TinyEngineChatRequest[1];
		final TinyEngineChatRequest[] seenChatRequest = new TinyEngineChatRequest[1];
		TinyEngineAiChatService chatService = new TinyEngineAiChatService() {
			@Override
			public TinyEngineChatResponse chat(TinyEngineChatRequest request) {
				seenChatRequest[0] = request;
				return new TinyEngineChatResponse("chat");
			}

			@Override
			public List<TinyEngineChatStreamChunk> stream(TinyEngineChatRequest request) {
				seenRequest[0] = request;
				return java.util.Arrays.asList(new TinyEngineChatStreamChunk("stream", false),
						new TinyEngineChatStreamChunk(null, true));
			}
		};
		WebTestClient client = WebTestClient.bindToController(
				new TinyEngineAiWebFluxController(chatService, new TestRagService(), executor())).build();

		client.post().uri("/app-center/api/chat/completions")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.TEXT_EVENT_STREAM)
				.bodyValue("{\"appId\":2,\"serviceId\":10,\"modelName\":\"test-model\",\"stream\":true,"
						+ "\"baseUrl\":\"https://attacker.invalid/v1\",\"apiKey\":\"attacker-key\"}")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
				.expectBody(String.class)
				.value(body -> {
					assertThat(body).contains("data: {\"choices\":[{\"index\":0,\"delta\":{\"content\":\"stream\"}");
					assertThat(body).contains("\"finish_reason\":\"stop\"");
					assertThat(body).contains("data: [DONE]\n\n");
				});
		assertThat(seenRequest[0]).isNotNull();
		assertThat(seenRequest[0].getBaseUrl()).isNull();
		assertThat(seenRequest[0].getApiKey()).isNull();

		client.post().uri("/app-center/api/ai/chat")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"appId\":2,\"serviceId\":10,\"modelName\":\"test-model\","
						+ "\"model\":\"attacker-model\",\"baseUrl\":\"https://attacker.invalid/v1\","
						+ "\"apiKey\":\"attacker-key\"}")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.expectBody()
				.jsonPath("$.object").isEqualTo("chat.completion")
				.jsonPath("$.model").isEqualTo("test-model")
				.jsonPath("$.choices[0].message.role").isEqualTo("assistant")
				.jsonPath("$.choices[0].message.content").isEqualTo("chat")
				.jsonPath("$.choices[0].finish_reason").isEqualTo("stop");
		assertThat(seenChatRequest[0]).isNotNull();
		assertThat(seenChatRequest[0].getModelName()).isEqualTo("test-model");
		assertThat(seenChatRequest[0].getBaseUrl()).isNull();
		assertThat(seenChatRequest[0].getApiKey()).isNull();

		client.post().uri("/tiny-engine/ai/chat/completions")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"appId\":2,\"serviceId\":10,\"modelName\":\"test-model\"}")
				.exchange()
				.expectStatus().isNotFound();
	}

	@Test
	void webFluxControllerRunsFormalChatOnTinyEngineBlockingScheduler() {
		Scheduler scheduler = Schedulers.newSingle("tiny-engine-ai-blocking-test");
		try {
			AtomicReference<String> invocationThread = new AtomicReference<String>();
			TinyEngineAiChatService chatService = new TinyEngineAiChatService() {
				@Override
				public TinyEngineChatResponse chat(TinyEngineChatRequest request) {
					invocationThread.set(Thread.currentThread().getName());
					return new TinyEngineChatResponse("chat");
				}

				@Override
				public List<TinyEngineChatStreamChunk> stream(TinyEngineChatRequest request) {
					throw new UnsupportedOperationException();
				}
			};
			TinyEngineReactiveExecutor executor = new TinyEngineReactiveExecutor(scheduler, Duration.ofSeconds(5),
					new TinyEngineRequestUserScope());
			TinyEngineAiWebFluxController controller = new TinyEngineAiWebFluxController(
					chatService, new TestRagService(), executor);
			TinyEngineChatRequest request = new TinyEngineChatRequest();
			request.setAppId(Integer.valueOf(2));
			request.setServiceId(Long.valueOf(10));
			request.setModelName("test-model");

			controller.chatCompletions(request).block();

			assertThat(invocationThread.get()).startsWith("tiny-engine-ai-blocking-test");
		} finally {
			scheduler.dispose();
		}
	}

	private static TinyEngineReactiveExecutor executor() {
		return new TinyEngineReactiveExecutor(Schedulers.immediate(), Duration.ofSeconds(5),
				new TinyEngineRequestUserScope());
	}

	private AiModelInvocationConfig invocation(String baseUrl, String apiKey) {
		AiModelInvocationConfig value = new AiModelInvocationConfig();
		value.setBaseUrl(baseUrl);
		value.setApiKey(apiKey);
		value.setModelName("test-model");
		Map<String, Object> settings = new java.util.LinkedHashMap<String, Object>();
		settings.put("temperature", Double.valueOf(0.2D));
		value.setSettings(settings);
		return value;
	}

	private static final class TestChatService implements TinyEngineAiChatService {
		@Override public com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatResponse chat(
				com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatRequest request) { return null; }
		@Override public java.util.List<com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatStreamChunk> stream(
				com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatRequest request) { return null; }
	}

	private static final class TestRagService implements TinyEngineRagService {
		@Override public boolean isAvailable() { return true; }
		@Override public String getUnavailableReason() { return null; }
		@Override public TinyEngineRagSearchResult search(TinyEngineRagRequest request) { return null; }
	}

	private static final class FailingChatService implements TinyEngineAiChatService {
		@Override public TinyEngineChatResponse chat(TinyEngineChatRequest request) {
			throw new TinyEngineUpstreamException(401, "INVALID_API_KEY", "Bearer server-api-key");
		}
		@Override public List<TinyEngineChatStreamChunk> stream(TinyEngineChatRequest request) {
			return Collections.emptyList();
		}
	}
}
