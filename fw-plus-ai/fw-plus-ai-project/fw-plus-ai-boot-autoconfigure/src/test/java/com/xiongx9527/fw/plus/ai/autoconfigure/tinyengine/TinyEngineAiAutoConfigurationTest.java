package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine;

import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.service.TinyEngineAiChatService;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.service.TinyEngineKnowledgeBaseService;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.service.TinyEngineRagService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TinyEngineAiAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(TinyEngineAiAutoConfiguration.class));

	@Test
	public void shouldNotCreateTinyEngineAiBeansWhenDisabled() {
		this.contextRunner
				.withPropertyValues("fw.plus.tiny-engine.ai.enabled=false")
				.run(context -> {
					assertTrue(context.getBeansOfType(TinyEngineAiChatService.class).isEmpty());
					assertTrue(context.getBeansOfType(TinyEngineRagService.class).isEmpty());
					assertTrue(context.getBeansOfType(TinyEngineKnowledgeBaseService.class).isEmpty());
				});
	}

	@Test
	public void shouldCreateChatServiceWithoutVectorStore() {
		this.contextRunner
				.withPropertyValues("fw.plus.tiny-engine.ai.chat.enabled=true")
				.run(context -> {
					assertTrue(context.getStartupFailure() == null);
					assertTrue(context.getBeansOfType(TinyEngineAiChatService.class).size() == 1);
					assertTrue(context.getBeansOfType(TinyEngineRagService.class).isEmpty());
				});
	}

	@Test
	public void shouldFailFastWhenRagDependenciesAreMissing() {
		this.contextRunner
				.withPropertyValues("fw.plus.tiny-engine.ai.rag.enabled=true")
				.run(context -> assertTrue(hasCause(context.getStartupFailure(),
						"requires both AiModelRegistry and AiEmbeddingStoreRegistry")));
	}

	private boolean hasCause(Throwable throwable, String message) {
		Throwable current = throwable;
		while (current != null) {
			if (current.getMessage() != null && current.getMessage().contains(message)) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}
}
