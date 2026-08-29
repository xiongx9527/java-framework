package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter;

import com.xiongx9527.fw.plus.ai.langchain4j.adapter.model.LangChain4jModelProviderAdapter;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.store.LangChain4jEmbeddingStoreProviderAdapter;
import com.xiongx9527.fw.plus.ai.model.AiModelAdapter;
import com.xiongx9527.fw.plus.ai.model.AiModelFactory;
import com.xiongx9527.fw.plus.ai.model.AiModelRegistry;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreAdapter;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreFactory;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.support.SpringFactoriesLoader;

import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreType;

import java.util.LinkedHashSet;
import java.util.Set;

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
			assertEquals(23, context.getBeansOfType(
					LangChain4jEmbeddingStoreProviderAdapter.class).size());
		});
	}

	@Test
	public void shouldContributeSingleAdapterBeansWhenLoadedFromSpringFactoriesMetadata() throws Exception {
		ClassLoader classLoader = getClass().getClassLoader();
		Set<String> autoConfigurationNames = new LinkedHashSet<String>(
				SpringFactoriesLoader.loadFactoryNames(EnableAutoConfiguration.class, classLoader));
		assertTrue(autoConfigurationNames.contains(LangChain4jAdapterAutoConfiguration.class.getName()));

		Class<?> autoConfiguration = Class.forName(LangChain4jAdapterAutoConfiguration.class.getName(), true, classLoader);
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(autoConfiguration))
				.run(context -> {
					assertTrue(context.containsBean("openAiChatModelProviderAdapter"));
					assertEquals(1, context.getBeansOfType(AiModelAdapter.class).size());
					assertEquals(1, context.getBeansOfType(AiEmbeddingStoreAdapter.class).size());
				});
	}

	@Test
	public void shouldStartWhenOptionalEmbeddingStoreProviderSdksAreMissing() {
		this.contextRunner
				.withClassLoader(new FilteredClassLoader(
						"dev.langchain4j.store.embedding.redis",
						"dev.langchain4j.store.embedding.milvus",
						"dev.langchain4j.store.embedding.elasticsearch",
						"dev.langchain4j.store.embedding.qdrant",
						"dev.langchain4j.store.embedding.azure.search",
						"dev.langchain4j.store.embedding.weaviate",
						"dev.langchain4j.rag.content.retriever.azure.search",
						"com.azure.core.credential",
						"com.azure.search.documents.indexes",
						"io.milvus",
						"io.qdrant",
						"io.weaviate"))
				.run(context -> {
					assertTrue(context.getStartupFailure() == null);
					assertTrue(context.containsBean("redisStackEmbeddingStoreProviderAdapter"));
					assertTrue(context.containsBean("milvusEmbeddingStoreProviderAdapter"));
					assertTrue(context.containsBean("elasticsearchEmbeddingStoreProviderAdapter"));
					assertTrue(context.containsBean("qdrantEmbeddingStoreProviderAdapter"));
					assertTrue(context.containsBean("weaviateEmbeddingStoreProviderAdapter"));
					assertTrue(context.containsBean("azureAiSearchEmbeddingStoreProviderAdapter"));
					assertFalse(provider(context, "redisStackEmbeddingStoreProviderAdapter")
							.supports(definition("redis")));
					assertFalse(provider(context, "milvusEmbeddingStoreProviderAdapter")
							.supports(definition("milvus")));
					assertTrue(context.containsBean("inMemoryEmbeddingStoreProviderAdapter"));
					assertEquals(23, context.getBeansOfType(
							LangChain4jEmbeddingStoreProviderAdapter.class).size());
					assertEquals(1, context.getBeansOfType(AiEmbeddingStoreAdapter.class).size());
				});
	}

	private LangChain4jEmbeddingStoreProviderAdapter provider(
			org.springframework.context.ApplicationContext context, String name) {
		return context.getBean(name, LangChain4jEmbeddingStoreProviderAdapter.class);
	}

	private AiEmbeddingStoreDefinition definition(String provider) {
		AiEmbeddingStoreDefinition definition = new AiEmbeddingStoreDefinition();
		definition.setKey("knowledge");
		definition.setFramework("langchain4j");
		definition.setProvider(provider);
		definition.setType(AiEmbeddingStoreType.VECTOR);
		definition.setEnabled(true);
		return definition;
	}
}
