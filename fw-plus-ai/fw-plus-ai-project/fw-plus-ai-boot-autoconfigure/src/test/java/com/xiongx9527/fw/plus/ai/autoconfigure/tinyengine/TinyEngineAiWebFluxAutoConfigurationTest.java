package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatResponse;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatStreamChunk;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.official.TinyEngineUpstreamException;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.provider.TinyEngineAiCapability;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.provider.TinyEngineAiProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.provider.TinyEngineAiProviderRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag.TinyEngineRagRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag.TinyEngineRagSearchResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.service.TinyEngineAiChatService;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.service.TinyEngineRagService;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AiModelInvocationConfig;
import com.xiongx9527.fw.plus.tinyengine.service.AppAiModelConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import java.util.Collections;
import java.util.List;

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
			.withPropertyValues("fw.plus.tiny-engine.ai.web.enabled=true");

	@Test
	void servletApplicationRegistersOnlyServletTinyEngineAiController() {
		servletRunner.run(context -> {
			assertThat(context).hasSingleBean(TinyEngineAiController.class);
			assertThat(context).doesNotHaveBean(TinyEngineAiWebFluxController.class);
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
		});
	}

	@Test
	void defaultChatServiceResolvesServerSideCredentialsAndIgnoresLegacyClientCredentials() throws Exception {
		AppAiModelConfigService configService = mock(AppAiModelConfigService.class);
		TinyEngineAiProvider provider = mock(TinyEngineAiProvider.class);
		when(provider.getKey()).thenReturn("test");
		when(provider.supports(TinyEngineAiCapability.CHAT)).thenReturn(true);
		when(provider.chat(org.mockito.ArgumentMatchers.any(TinyEngineChatRequest.class)))
				.thenReturn(new TinyEngineChatResponse("ok"));
		when(configService.resolveInvocation(2, 10L, "test-model"))
				.thenReturn(invocation("https://example.invalid/v1", "test-key"));
		TinyEngineAiProperties properties = new TinyEngineAiProperties();
		StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
		beanFactory.addBean("configService", configService);
		TinyEngineAiChatService chatService = new TinyEngineAiAutoConfiguration().tinyEngineAiChatService(
				new TinyEngineAiProviderRegistry(Collections.singletonList(provider), "test"), properties,
				beanFactory.getBeanProvider(AppAiModelConfigService.class));
		TinyEngineChatRequest request = new ObjectMapper().readValue(
				"{\"appId\":2,\"serviceId\":10,\"modelName\":\"test-model\",\"baseUrl\":\"https://attacker.invalid\",\"apiKey\":\"attacker-key\"}",
				TinyEngineChatRequest.class);
		assertThat(new ObjectMapper().writeValueAsString(request)).doesNotContain("baseUrl", "apiKey");

		chatService.chat(request);

		verify(configService).resolveInvocation(2, 10L, "test-model");
		verify(provider).chat(argThat(it -> "test-model".equals(it.getModel())
				&& "https://example.invalid/v1".equals(it.getBaseUrl())
				&& "test-key".equals(it.getApiKey())));
	}

	@Test
	void webFluxControllerConvertsUpstreamAuthorizationFailureToModelInvocationError() {
		TinyEngineAiWebFluxController controller = new TinyEngineAiWebFluxController(
				new FailingChatService(), new TestRagService());
		TinyEngineChatRequest request = new TinyEngineChatRequest();
		request.setAppId(Integer.valueOf(2));
		request.setServiceId(Long.valueOf(10));
		request.setModelName("test-model");

		org.springframework.web.server.ResponseStatusException exception = assertThrows(
				org.springframework.web.server.ResponseStatusException.class, () -> controller.chat(request).block());
		assertEquals(org.springframework.http.HttpStatus.BAD_GATEWAY, exception.getStatus());
		assertEquals("TinyEngine AI model invocation failed", exception.getReason());
	}

	private AiModelInvocationConfig invocation(String baseUrl, String apiKey) {
		AiModelInvocationConfig value = new AiModelInvocationConfig();
		value.setBaseUrl(baseUrl);
		value.setApiKey(apiKey);
		value.setModelName("test-model");
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
