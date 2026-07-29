package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter;

import com.xiongx9527.fw.plus.ai.langchain4j.adapter.EmbeddingStoreProviderAdapter;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jModelProviderAdapter;
import com.xiongx9527.fw.plus.ai.model.AiModelAdapter;
import com.xiongx9527.fw.plus.ai.model.AiModelFactory;
import com.xiongx9527.fw.plus.ai.model.AiModelRegistry;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreAdapter;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreFactory;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LangChain4jAdapterAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(LangChain4jAdapterAutoConfiguration.class));

	@Test
	public void shouldBackOffWhenUnifiedLangChain4jAdapterIsMissing() {
		this.contextRunner
				.withClassLoader(new FilteredClassLoader("com.xiongx9527.fw.plus.ai.langchain4j.adapter"))
				.run(context -> {
					assertTrue(context.getStartupFailure() == null);
					assertTrue(context.getBeansOfType(AiModelAdapter.class).isEmpty());
					assertTrue(context.getBeansOfType(AiEmbeddingStoreAdapter.class).isEmpty());
				});
	}

	@Test
	public void shouldContributeOnlyLangChain4jAdapters() {
		this.contextRunner.run(context -> {
			assertTrue(context.getStartupFailure() == null);
			assertEquals(1, context.getBeansOfType(AiModelAdapter.class).size());
			assertEquals("langchain4j", context.getBean(AiModelAdapter.class).getFramework());
			assertEquals(1, context.getBeansOfType(AiEmbeddingStoreAdapter.class).size());
			assertEquals("langchain4j", context.getBean(AiEmbeddingStoreAdapter.class).getFramework());
			assertFalse(context.containsBeanDefinition("aiModelFactory"));
			assertFalse(context.containsBeanDefinition("aiModelRegistry"));
			assertFalse(context.containsBeanDefinition("aiEmbeddingStoreFactory"));
			assertFalse(context.containsBeanDefinition("aiEmbeddingStoreRegistry"));
			assertTrue(context.getBeansOfType(AiModelFactory.class).isEmpty());
			assertTrue(context.getBeansOfType(AiModelRegistry.class).isEmpty());
			assertTrue(context.getBeansOfType(AiEmbeddingStoreFactory.class).isEmpty());
			assertTrue(context.getBeansOfType(AiEmbeddingStoreRegistry.class).isEmpty());
		});
	}

	@Test
	public void shouldContributeLangChain4jProviderAdapters() {
		this.contextRunner.run(context -> {
			assertTrue(context.getStartupFailure() == null);
			assertTrue(context.containsBean("openAiChatModelProviderAdapter"));
			assertTrue(context.containsBean("inMemoryEmbeddingStoreProviderAdapter"));
			assertFalse(context.getBeansOfType(LangChain4jModelProviderAdapter.class).isEmpty());
			assertFalse(context.getBeansOfType(EmbeddingStoreProviderAdapter.class).isEmpty());
		});
	}
}
