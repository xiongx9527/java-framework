package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine;

import com.xiongx9527.fw.plus.ai.autoconfigure.model.AiModelAutoConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.store.embedding.AiEmbeddingStoreAutoConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.service.TinyEngineAiChatService;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.service.TinyEngineKnowledgeBaseService;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.service.TinyEngineRagService;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag.TinyEngineRagRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.provider.TinyEngineAiProvider;
import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelRegistry;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.Set;

public class TinyEngineAiAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(TinyEngineAiAutoConfiguration.class));

	private final ApplicationContextRunner neutralRegistryContextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(AiModelAutoConfiguration.class,
					AiEmbeddingStoreAutoConfiguration.class, TinyEngineAiAutoConfiguration.class));

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
				.withBean(TinyEngineAiProvider.class, TestBusinessProvider::new)
				.withPropertyValues("fw.plus.tiny-engine.ai.chat.enabled=true")
				.run(context -> {
					assertTrue(context.getStartupFailure() == null);
					assertTrue(context.getBeansOfType(TinyEngineAiChatService.class).size() == 1);
					assertTrue(context.getBeansOfType(TinyEngineRagService.class).isEmpty());
				});
	}

	@Test
	public void shouldFailFastWhenChatProviderIsNotSuppliedByBusiness() {
		this.contextRunner
				.withPropertyValues("fw.plus.tiny-engine.ai.chat.enabled=true")
				.run(context -> assertTrue(hasCause(context.getStartupFailure(),
						"TinyEngine AI chat provider is unavailable")));
	}

	@Test
	public void shouldFailFastWhenConfiguredChatProviderIsMissing() {
		this.contextRunner
				.withPropertyValues(
						"fw.plus.tiny-engine.ai.chat.enabled=true",
						"fw.plus.tiny-engine.ai.provider=missing")
				.run(context -> assertTrue(hasCause(context.getStartupFailure(),
						"TinyEngine AI chat provider is unavailable")));
	}

	@Test
	public void shouldRegisterUnavailableChatServiceWhenExplicitlyDegraded() {
		this.contextRunner
				.withPropertyValues(
						"fw.plus.tiny-engine.ai.chat.enabled=true",
						"fw.plus.tiny-engine.ai.provider=missing",
						"fw.plus.tiny-engine.ai.missing-dependency-policy=degrade")
				.run(context -> {
					assertTrue(context.getStartupFailure() == null);
					TinyEngineAiChatService chatService = context.getBean(TinyEngineAiChatService.class);
					assertTrue(!chatService.isAvailable());
					assertTrue(chatService.getUnavailableReason().contains("chat provider"));
				});
	}

	@Test
	public void shouldFailFastWhenRagDependenciesAreMissing() {
		this.contextRunner
				.withPropertyValues("fw.plus.tiny-engine.ai.rag.enabled=true")
				.run(context -> assertTrue(hasCause(context.getStartupFailure(),
						"requires both AiModelRegistry and AiEmbeddingStoreRegistry")));
	}

	@Test
	public void shouldFailFastWhenNeutralRegistriesContainNoConfiguredRagDependencies() {
		this.neutralRegistryContextRunner
				.withPropertyValues("fw.plus.tiny-engine.ai.rag.enabled=true")
				.run(context -> assertTrue(hasCause(context.getStartupFailure(),
						"requires an available embedding model and embedding store")));
	}

	@Test
	public void shouldFailFastWhenRegistryDefaultKeysResolveToNullDependencies() {
		this.contextRunner
				.withBean(AiModelRegistry.class, DefaultModelRegistry::new)
				.withBean(AiEmbeddingStoreRegistry.class, DefaultStoreRegistry::new)
				.withPropertyValues("fw.plus.tiny-engine.ai.rag.enabled=true")
				.run(context -> assertTrue(hasCause(context.getStartupFailure(),
						"requires an available embedding model and embedding store")));
	}

	@Test
	public void shouldRejectUnknownMissingDependencyPolicy() {
		this.contextRunner
				.withPropertyValues(
						"fw.plus.tiny-engine.ai.rag.enabled=true",
						"fw.plus.tiny-engine.ai.missing-dependency-policy=ignore")
				.run(context -> assertTrue(hasCause(context.getStartupFailure(),
						"Unsupported TinyEngine AI missing dependency policy")));
	}

	@Test
	public void shouldRegisterUnavailableRagServiceWhenExplicitlyDegraded() {
		this.contextRunner
				.withPropertyValues(
						"fw.plus.tiny-engine.ai.rag.enabled=true",
						"fw.plus.tiny-engine.ai.missing-dependency-policy=degrade")
				.run(context -> {
					assertTrue(context.getStartupFailure() == null);
					TinyEngineRagService ragService = context.getBean(TinyEngineRagService.class);
					assertTrue(!ragService.isAvailable());
					assertTrue(ragService.getUnavailableReason().contains("available embedding model"));
					assertThrows(IllegalStateException.class, () -> ragService.search(new TinyEngineRagRequest()));
				});
	}

	@Test
	public void shouldRegisterUnavailableRagServiceWhenDegradePolicyContainsWhitespace() {
		TinyEngineAiProperties properties = new TinyEngineAiProperties();
		properties.setMissingDependencyPolicy(" degrade ");
		StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();

		TinyEngineRagService ragService = new TinyEngineAiAutoConfiguration().tinyEngineRagService(properties,
				beanFactory.getBeanProvider(AiModelRegistry.class),
				beanFactory.getBeanProvider(AiEmbeddingStoreRegistry.class));

		assertTrue(!ragService.isAvailable());
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

	private static final class DefaultModelRegistry implements AiModelRegistry {
		@Override public AiModel get(String key) { return null; }
		@Override public AiModel getDefault() { return null; }
		@Override public String getDefaultKey() { return "default-model"; }
		@Override public boolean contains(String key) { return "default-model".equals(key); }
		@Override public Set<String> keys() { return Collections.singleton("default-model"); }
	}

	private static final class DefaultStoreRegistry implements AiEmbeddingStoreRegistry {
		@Override public AiEmbeddingStore get(String key) { return null; }
		@Override public AiEmbeddingStore getDefault() { return null; }
		@Override public String getDefaultKey() { return "default-store"; }
		@Override public boolean contains(String key) { return "default-store".equals(key); }
		@Override public Set<String> keys() { return Collections.singleton("default-store"); }
	}

	private static final class TestBusinessProvider implements TinyEngineAiProvider {
		@Override public String getKey() { return "business"; }
		@Override public boolean supports(com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.provider.TinyEngineAiCapability capability) { return true; }
		@Override public com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatResponse chat(
				com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatRequest request) { return null; }
		@Override public java.util.List<com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatStreamChunk> stream(
				com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatRequest request) { return null; }
	}
}
