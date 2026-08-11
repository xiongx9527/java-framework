package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine;

import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag.TinyEngineRagRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag.TinyEngineRagSearchResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.service.TinyEngineAiChatService;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.service.TinyEngineRagService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

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
}
