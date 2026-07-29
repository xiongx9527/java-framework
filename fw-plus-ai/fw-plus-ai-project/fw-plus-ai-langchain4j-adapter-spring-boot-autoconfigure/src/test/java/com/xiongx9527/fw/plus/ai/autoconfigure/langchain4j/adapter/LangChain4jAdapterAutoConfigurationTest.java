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
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
					assertFalse(context.containsBean("redisStackEmbeddingStoreProviderAdapter"));
					assertFalse(context.containsBean("milvusEmbeddingStoreProviderAdapter"));
					assertFalse(context.containsBean("elasticsearchEmbeddingStoreProviderAdapter"));
					assertFalse(context.containsBean("qdrantEmbeddingStoreProviderAdapter"));
					assertFalse(context.containsBean("weaviateEmbeddingStoreProviderAdapter"));
					assertFalse(context.containsBean("azureAiSearchEmbeddingStoreProviderAdapter"));
					assertTrue(context.containsBean("inMemoryEmbeddingStoreProviderAdapter"));
					assertEquals(1, context.getBeansOfType(AiEmbeddingStoreAdapter.class).size());
				});
	}

	@Test
	public void shouldDeclareClassConditionsOnDirectLinkedEmbeddingStoreProviderBeans() throws Exception {
		assertClassCondition("redisStackEmbeddingStoreProviderAdapter",
				"dev.langchain4j.store.embedding.redis.RedisEmbeddingStore");
		assertClassCondition("milvusEmbeddingStoreProviderAdapter",
				"dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore",
				"io.milvus.common.clientenum.ConsistencyLevelEnum",
				"io.milvus.param.IndexType",
				"io.milvus.param.MetricType");
		assertClassCondition("elasticsearchEmbeddingStoreProviderAdapter",
				"dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore");
		assertClassCondition("qdrantEmbeddingStoreProviderAdapter",
				"dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore");
		assertClassCondition("weaviateEmbeddingStoreProviderAdapter",
				"dev.langchain4j.store.embedding.weaviate.WeaviateEmbeddingStore");
		assertClassCondition("azureAiSearchEmbeddingStoreProviderAdapter",
				"dev.langchain4j.store.embedding.azure.search.AzureAiSearchEmbeddingStore",
				"dev.langchain4j.rag.content.retriever.azure.search.AzureAiSearchFilterMapper",
				"com.azure.core.credential.TokenCredential",
				"com.azure.search.documents.indexes.models.SearchIndex");
	}

	private void assertClassCondition(String beanMethodName, String... expectedClassNames) throws Exception {
		Method method = beanMethod(beanMethodName);
		ConditionalOnClass conditional = method.getAnnotation(ConditionalOnClass.class);
		assertNotNull(conditional, beanMethodName + " should declare @ConditionalOnClass");
		Set<String> actualClassNames = new LinkedHashSet<String>(Arrays.asList(conditional.name()));
		assertEquals(new LinkedHashSet<String>(Arrays.asList(expectedClassNames)), actualClassNames);
	}

	private Method beanMethod(String methodName) {
		for (Method method : LangChain4jEmbeddingStoreAdapterAutoConfiguration.class.getDeclaredMethods()) {
			if (methodName.equals(method.getName())) {
				return method;
			}
		}
		throw new IllegalArgumentException("Cannot find bean method: " + methodName);
	}
}
