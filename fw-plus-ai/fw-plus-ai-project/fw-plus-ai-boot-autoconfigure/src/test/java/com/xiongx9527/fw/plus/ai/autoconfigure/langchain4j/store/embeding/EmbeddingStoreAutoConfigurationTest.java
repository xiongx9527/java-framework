package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding;

import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingDocument;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingMatch;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingQuery;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingSearchResult;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreAdapter;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreException;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreRegistry;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EmbeddingStoreAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(EmbeddingStoreAutoConfiguration.class);

	@Test
	public void shouldKeepNeutralInfrastructureWhenLangChain4jAdapterIsMissing() {
		this.contextRunner
				.withClassLoader(new FilteredClassLoader("com.xiongx9527.fw.plus.ai.langchain4j.adapter"))
				.run(context -> {
					assertTrue(context.getStartupFailure() == null);
					assertTrue(context.containsBean("aiEmbeddingStoreFactory"));
					assertTrue(context.containsBean("aiEmbeddingStoreRegistry"));
					assertTrue(context.getBeansOfType(AiEmbeddingStoreAdapter.class).isEmpty());
				});
	}

	@Test
	public void shouldFailConfiguredStoreWhenNoAdapterExists() {
		this.contextRunner
				.withPropertyValues(
						"fw-plus.ai.embedding-store.stores.knowledge.provider=test",
						"fw-plus.ai.embedding-store.stores.knowledge.type=vector")
				.run(context -> assertTrue(hasCause(context.getStartupFailure(),
						"No adapter supports store")));
	}

	@Test
	public void shouldAutoDiscoverTheOnlySupportingAdapter() {
		this.contextRunner
				.withPropertyValues(
						"fw-plus.ai.embedding-store.default-store=knowledge",
						"fw-plus.ai.embedding-store.stores.knowledge.provider=test",
						"fw-plus.ai.embedding-store.stores.knowledge.type=vector")
				.withBean(AiEmbeddingStoreAdapter.class, () -> new FakeStoreAdapter("langchain4j"))
				.run(context -> {
					assertTrue(context.getStartupFailure() == null);
					AiEmbeddingStoreRegistry registry = context.getBean(AiEmbeddingStoreRegistry.class);
					assertEquals("langchain4j", registry.getDefault().getFramework());
				});
	}

	@Test
	public void shouldRejectAmbiguousAdaptersWhenFrameworkIsMissing() {
		this.contextRunner
				.withPropertyValues(
						"fw-plus.ai.embedding-store.stores.knowledge.provider=test",
						"fw-plus.ai.embedding-store.stores.knowledge.type=vector")
				.withBean("langChain4jFakeAdapter", AiEmbeddingStoreAdapter.class,
						() -> new FakeStoreAdapter("langchain4j"))
				.withBean("springAiFakeAdapter", AiEmbeddingStoreAdapter.class,
						() -> new FakeStoreAdapter("spring-ai"))
				.run(context -> assertTrue(hasCause(context.getStartupFailure(),
						"Multiple adapters support store")));
	}

	@Test
	public void shouldSelectExplicitLangChain4jFramework() {
		this.contextRunner
				.withPropertyValues(
						"fw-plus.ai.embedding-store.default-store=knowledge",
						"fw-plus.ai.embedding-store.stores.knowledge.framework=langchain4j",
						"fw-plus.ai.embedding-store.stores.knowledge.provider=test",
						"fw-plus.ai.embedding-store.stores.knowledge.type=vector")
				.withBean("langChain4jFakeAdapter", AiEmbeddingStoreAdapter.class,
						() -> new FakeStoreAdapter("langchain4j"))
				.withBean("springAiFakeAdapter", AiEmbeddingStoreAdapter.class,
						() -> new FakeStoreAdapter("spring-ai"))
				.run(context -> {
					assertTrue(context.getStartupFailure() == null);
					assertEquals("langchain4j", context.getBean(AiEmbeddingStoreRegistry.class)
							.getDefault().getFramework());
				});
	}

	@Test
	public void shouldIgnoreLegacyLangChain4jEmbeddingStorePrefix() {
		this.contextRunner
				.withPropertyValues(
						"fw-plus.ai.langchain4j.embedding-store.default-store=legacy",
						"fw-plus.ai.langchain4j.embedding-store.stores.legacy.provider=test")
				.withBean(AiEmbeddingStoreAdapter.class, () -> new FakeStoreAdapter("langchain4j"))
				.run(context -> {
					assertTrue(context.getStartupFailure() == null);
					AiEmbeddingStoreRegistry registry = context.getBean(AiEmbeddingStoreRegistry.class);
					assertTrue(registry.keys().isEmpty());
					assertFalse(context.containsBean("embeddingStoreRegistry"));
				});
	}

	private boolean hasCause(Throwable throwable, String message) {
		Throwable current = throwable;
		while (current != null) {
			if (current instanceof AiEmbeddingStoreException && current.getMessage() != null
					&& current.getMessage().contains(message)) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}

	private static final class FakeStoreAdapter implements AiEmbeddingStoreAdapter {

		private final String framework;

		private FakeStoreAdapter(String framework) {
			this.framework = framework;
		}

		@Override
		public String getFramework() {
			return framework;
		}

		@Override
		public boolean supports(AiEmbeddingStoreDefinition definition) {
			return definition != null && "test".equals(definition.getProvider());
		}

		@Override
		public AiEmbeddingStore create(final AiEmbeddingStoreDefinition definition) {
			return new AiEmbeddingStore() {
				@Override public String getKey() { return definition.getKey(); }
				@Override public String getFramework() { return framework; }
				@Override public String getProvider() { return definition.getProvider(); }
				@Override public AiEmbeddingStoreType getType() { return definition.getType(); }
				@Override public String add(AiEmbeddingDocument document) { return null; }
				@Override public List<String> addAll(List<AiEmbeddingDocument> documents) { return Collections.emptyList(); }
				@Override public void remove(String id) { }
				@Override public void removeAll(List<String> ids) { }
				@Override public AiEmbeddingSearchResult search(AiEmbeddingQuery query) {
					return new AiEmbeddingSearchResult(Collections.<AiEmbeddingMatch>emptyList());
				}
			};
		}
	}
}
