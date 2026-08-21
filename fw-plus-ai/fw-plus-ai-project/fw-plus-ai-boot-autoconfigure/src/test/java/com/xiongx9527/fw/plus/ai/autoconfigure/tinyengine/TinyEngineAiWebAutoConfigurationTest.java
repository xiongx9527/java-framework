package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine;

import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag.TinyEngineRagRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag.TinyEngineRagSearchResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.official.TinyEngineOfficialAiController;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.official.TinyEngineOfficialAiWebFluxController;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.service.TinyEngineAiChatService;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.service.TinyEngineRagService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.HashSet;
import java.util.Map;
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
				.run(context -> {
					assertFalse(TinyEngineOfficialAiController.class.isAnnotationPresent(RestController.class));
					assertFalse(TinyEngineOfficialAiWebFluxController.class.isAnnotationPresent(RestController.class));
					assertEquals(1, context.getBeansWithAnnotation(RestController.class).size());
					assertTrue(context.getBeansOfType(TinyEngineOfficialAiController.class).isEmpty());
					RequestMappingHandlerMapping mapping = context.getBean(RequestMappingHandlerMapping.class);
					Set<String> patterns = mappedPatterns(mapping);
					assertTrue(patterns.contains("/app-center/api/ai/chat"));
					assertTrue(patterns.contains("/app-center/api/chat/completions"));
					assertTrue(patterns.contains("/app-center/api/ai/search"));
					assertFalse(patterns.contains("/tiny-engine/ai/chat"));
					assertFalse(patterns.contains("/tiny-engine/ai/chat/completions"));
					assertFalse(patterns.contains("/tiny-engine/ai/encrypt-key"));
					assertFalse(patterns.contains("/tiny-engine/ai/search"));
					assertEquals(TinyEngineAiController.class,
							mappedHandlerType(mapping, "/app-center/api/chat/completions"));
				});
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
	public void shouldPrefixAppCenterRoutesWithTinyEngineWebBasePath() {
		this.contextRunner
				.withPropertyValues(
						"fw.plus.tiny-engine.ai.web.enabled=true",
						"fw.plus.tiny-engine.web.base-path=/tiny-engine")
				.run(context -> {
					Set<String> patterns = mappedPatterns(context.getBean(RequestMappingHandlerMapping.class));
					assertTrue(patterns.contains("/tiny-engine/app-center/api/ai/chat"));
					assertTrue(patterns.contains("/tiny-engine/app-center/api/chat/completions"));
					assertFalse(patterns.contains("/app-center/api/chat/completions"));
				});
	}

	@Test
	public void shouldKeepAppCenterRoutesAndIgnoreLegacyCompatibilityPathOverrides() {
		this.contextRunner
				.withPropertyValues(
						"fw.plus.tiny-engine.ai.web.enabled=true",
						"fw.plus.tiny-engine.ai.web.base-path=/custom-ai",
						"fw.plus.tiny-engine.ai.web.chat-path=/custom-chat",
						"fw.plus.tiny-engine.ai.web.chat-completions-path=/custom-completions",
						"fw.plus.tiny-engine.ai.web.encrypt-key-path=/custom-encrypt-key",
						"fw.plus.tiny-engine.ai.web.search-path=/custom-search",
						"fw.plus.tiny-engine.ai.official.enabled=false")
				.run(context -> {
					Set<String> patterns = mappedPatterns(context.getBean(RequestMappingHandlerMapping.class));
					assertFalse(patterns.contains("/custom-ai/custom-chat"));
					assertFalse(patterns.contains("/custom-ai/custom-completions"));
					assertFalse(patterns.contains("/custom-ai/custom-encrypt-key"));
					assertFalse(patterns.contains("/custom-ai/custom-search"));
					assertTrue(patterns.contains("/app-center/api/ai/chat"));
					assertTrue(patterns.contains("/app-center/api/chat/completions"));
					assertTrue(patterns.contains("/app-center/api/encrypt-key"));
					assertTrue(patterns.contains("/app-center/api/ai/search"));
					assertEquals(TinyEngineAiController.class,
							mappedHandlerType(context.getBean(RequestMappingHandlerMapping.class),
									"/app-center/api/chat/completions"));
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

	private Class<?> mappedHandlerType(RequestMappingHandlerMapping handlerMapping, String path) {
		for (Map.Entry<org.springframework.web.servlet.mvc.method.RequestMappingInfo, HandlerMethod> entry
				: handlerMapping.getHandlerMethods().entrySet()) {
			Set<String> patterns = entry.getKey().getPatternsCondition() == null
					? entry.getKey().getPathPatternsCondition().getPatternValues()
					: entry.getKey().getPatternsCondition().getPatterns();
			if (patterns.contains(path)) {
				return entry.getValue().getBeanType();
			}
		}
		return null;
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
