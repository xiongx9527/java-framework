package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine;

import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag.TinyEngineRagRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag.TinyEngineRagSearchResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.service.TinyEngineAiChatService;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.service.TinyEngineRagService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TinyEngineAiWebAutoConfigurationTest {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(WebMvcAutoConfiguration.class, TinyEngineAiAutoConfiguration.class))
			.withBean(TinyEngineAiChatService.class, TestChatService::new)
			.withBean(TinyEngineRagService.class, TestRagService::new);

	@Test
	public void shouldRegisterControllerWhenWebIsEnabled() {
		this.contextRunner
				.withPropertyValues("fw.plus.tiny-engine.ai.web.enabled=true")
				.run(context -> assertEquals(1, context.getBeansWithAnnotation(RestController.class).size()));
	}

	@Test
	public void shouldNotRegisterControllerWhenWebIsDisabledByDefault() {
		this.contextRunner.run(context ->
				assertTrue(context.getBeansWithAnnotation(RestController.class).isEmpty()));
	}

	@Test
	public void shouldNotRegisterControllerWhenTinyEngineAiIsDisabled() {
		this.contextRunner
				.withPropertyValues("fw.plus.tiny-engine.ai.enabled=false", "fw.plus.tiny-engine.ai.web.enabled=true")
				.run(context -> assertTrue(context.getBeansWithAnnotation(RestController.class).isEmpty()));
	}

	@Test
	public void shouldRegisterConfiguredRoutesWithoutAppCenterApiRoutes() {
		this.contextRunner
				.withPropertyValues(
						"fw.plus.tiny-engine.ai.web.enabled=true",
						"fw.plus.tiny-engine.ai.web.base-path=/custom-ai",
						"fw.plus.tiny-engine.ai.web.chat-path=/custom-chat",
						"fw.plus.tiny-engine.ai.web.chat-completions-path=/custom-completions",
						"fw.plus.tiny-engine.ai.web.encrypt-key-path=/custom-encrypt-key",
						"fw.plus.tiny-engine.ai.web.search-path=/custom-search")
				.run(context -> {
					Set<String> patterns = mappedPatterns(context.getBean(RequestMappingHandlerMapping.class));
					assertTrue(patterns.contains("/custom-ai/custom-chat"));
					assertTrue(patterns.contains("/custom-ai/custom-completions"));
					assertTrue(patterns.contains("/custom-ai/custom-encrypt-key"));
					assertTrue(patterns.contains("/custom-ai/custom-search"));
					assertFalse(patterns.contains("/app-center/api/ai/chat"));
					assertFalse(patterns.contains("/app-center/api/chat/completions"));
					assertFalse(patterns.contains("/app-center/api/encrypt-key"));
					assertFalse(patterns.contains("/app-center/api/ai/search"));
				});
	}

	private Set<String> mappedPatterns(RequestMappingHandlerMapping handlerMapping) {
		Set<String> patterns = new HashSet<String>();
		handlerMapping.getHandlerMethods().keySet().forEach(mapping -> {
			if (mapping.getPatternsCondition() != null) {
				patterns.addAll(mapping.getPatternsCondition().getPatterns());
			} else {
				patterns.addAll(mapping.getPathPatternsCondition().getPatternValues());
			}
		});
		return patterns;
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
