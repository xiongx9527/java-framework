package com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow;

import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowModelProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.runtime.InMemoryTinyflowChainStateRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.runtime.TinyflowChainStateRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.runtime.TinyflowService;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStoreProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowWorkflowRepository;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jModelRegistry;
import dev.tinyflow.core.chain.repository.ChainStateField;
import dev.tinyflow.core.chain.ChainState;
import dev.tinyflow.core.chain.runtime.ChainExecutor;
import dev.tinyflow.core.llm.LlmManager;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.EnumSet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class TinyflowAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(TinyflowAutoConfiguration.class));

	@Test
	public void shouldCreateTinyflowRuntimeBeansByDefault() {
		contextRunner.run(context -> {
			assertSingleBean(context, TinyflowProperties.class);
			assertSingleBean(context, TinyflowWorkflowRepository.class);
			assertSingleBean(context, TinyflowChainStateRepository.class);
			assertSingleBean(context, ChainExecutor.class);
			assertSingleBean(context, TinyflowService.class);
			assertSingleBean(context, TinyflowModelProvider.class);
			assertSingleBean(context, TinyflowVectorStoreProvider.class);
		});
	}

	@Test
	public void shouldDisableTinyflowWhenExplicitlyDisabled() {
		contextRunner.withPropertyValues("fw.plus.ai.tinyflow.enabled=false")
				.run(context -> assertFalse(context.containsBean("tinyflowService")));
	}

	@Test
	public void shouldBackOffEveryReplaceableRepository() {
		contextRunner.withBean(TinyflowChainStateRepository.class, StubChainStateRepository::new)
				.withBean(TinyflowWorkflowRepository.class, StubWorkflowRepository::new)
				.run(context -> {
					assertSame(context.getBean(StubChainStateRepository.class),
							context.getBean(TinyflowChainStateRepository.class));
					assertSame(context.getBean(StubWorkflowRepository.class),
							context.getBean(TinyflowWorkflowRepository.class));
				});
	}

	@Test
	public void shouldBackOffRuntimeServiceProviders() {
		contextRunner.withBean(TinyflowService.class, StubTinyflowService::new)
				.withBean(TinyflowModelProvider.class, StubTinyflowModelProvider::new)
				.withBean(TinyflowVectorStoreProvider.class, StubTinyflowVectorStoreProvider::new)
				.run(context -> {
					assertSame(context.getBean(StubTinyflowService.class), context.getBean(TinyflowService.class));
					assertSame(context.getBean(StubTinyflowModelProvider.class), context.getBean(TinyflowModelProvider.class));
					assertSame(context.getBean(StubTinyflowVectorStoreProvider.class),
							context.getBean(TinyflowVectorStoreProvider.class));
				});
	}

	@Test
	public void shouldNotLoadWhenTinyflowCoreIsMissing() {
		contextRunner.withClassLoader(new FilteredClassLoader(ChainExecutor.class))
				.run(context -> assertFalse(context.containsBean("tinyflowService")));
	}

	@Test
	public void shouldBackOffModelIntegrationWhenLangChain4jAdapterIsMissing() {
		contextRunner.withClassLoader(new FilteredClassLoader(LangChain4jModelRegistry.class))
				.run(context -> assertFalse(context.containsBean("tinyflowModelProvider")));
	}

	@Test
	public void shouldUnregisterTheSameLlmProviderAfterConsecutiveContextsClose() {
		int providerCount = LlmManager.getInstance().providers.size();

		contextRunner.run(context -> assertTrue(LlmManager.getInstance().providers.size() > providerCount));
		assertTrue(LlmManager.getInstance().providers.size() == providerCount);
		contextRunner.run(context -> assertTrue(LlmManager.getInstance().providers.size() > providerCount));
		assertTrue(LlmManager.getInstance().providers.size() == providerCount);
	}

	@Test
	public void shouldRegisterCustomLlmProviderInsteadOfTheDefaultProvider() {
		contextRunner.withBean(dev.tinyflow.core.llm.LlmProvider.class, StubLlmProvider::new)
				.run(context -> assertTrue(LlmManager.getInstance().providers.contains(context.getBean(StubLlmProvider.class))));
	}

	private static final class StubChainStateRepository extends InMemoryTinyflowChainStateRepository {
		@Override
		public boolean tryUpdate(ChainState state, EnumSet<ChainStateField> fields) {
			return super.tryUpdate(state, fields);
		}
	}

	private static final class StubWorkflowRepository implements TinyflowWorkflowRepository {
		@Override public com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowWorkflow findById(String id) { return null; }
		@Override public com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowWorkflow findByPath(String path) { return null; }
		@Override public void save(com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowWorkflow workflow) { }
		@Override public void delete(String id) { }
	}

	private static final class StubTinyflowService implements TinyflowService {
		@Override public void validate(com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowWorkflow workflow) { }
		@Override public void register(com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowWorkflow workflow) { }
		@Override public void remove(String workflowId) { }
		@Override public com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowExecutionResult execute(String workflowId, java.util.Map<String, Object> variables) { return null; }
		@Override public com.xiongx9527.fw.plus.ai.tinyflow.runtime.TinyflowAsyncResult executeAsync(String workflowId, java.util.Map<String, Object> variables) { return null; }
		@Override public com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowExecutionResult resume(String instanceId, java.util.Map<String, Object> variables) { return null; }
		@Override public com.xiongx9527.fw.plus.ai.tinyflow.runtime.TinyflowAsyncResult resumeAsync(String instanceId, java.util.Map<String, Object> variables) { return null; }
	}

	private static final class StubTinyflowModelProvider implements TinyflowModelProvider {
		@Override public java.util.List<com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowModelDescriptor> listChatModels() { return java.util.Collections.emptyList(); }
		@Override public java.util.List<com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowModelDescriptor> listEmbeddingModels() { return java.util.Collections.emptyList(); }
		@Override public dev.langchain4j.model.chat.ChatLanguageModel getChatModel(String key) { return null; }
		@Override public com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowEmbeddingModel getEmbeddingModel(String key) { return null; }
	}

	private static final class StubTinyflowVectorStoreProvider implements TinyflowVectorStoreProvider {
		@Override public java.util.List<com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStoreDescriptor> list() { return java.util.Collections.emptyList(); }
		@Override public com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStore get(String key) { return null; }
	}

	private static final class StubLlmProvider implements dev.tinyflow.core.llm.LlmProvider {
		@Override public dev.tinyflow.core.llm.Llm getChatModel(Object key) { return null; }
	}

	private void assertSingleBean(org.springframework.context.ApplicationContext context, Class<?> beanType) {
		assertTrue("Expected exactly one " + beanType.getName(), context.getBeansOfType(beanType).size() == 1);
	}
}
