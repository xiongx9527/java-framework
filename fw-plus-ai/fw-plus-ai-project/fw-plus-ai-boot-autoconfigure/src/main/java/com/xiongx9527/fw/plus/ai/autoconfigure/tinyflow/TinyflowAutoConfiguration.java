package com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow;

import com.xiongx9527.fw.plus.ai.model.AiModelRegistry;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreRegistry;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowModelProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.node.TinyflowNodeProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.node.TinyflowNodeProviderRegistry;
import com.xiongx9527.fw.plus.ai.tinyflow.runtime.DefaultTinyflowService;
import com.xiongx9527.fw.plus.ai.tinyflow.runtime.InMemoryChainDefinitionRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.runtime.InMemoryTinyflowChainStateRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.runtime.TinyflowChainDefinitionRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.runtime.TinyflowChainStateRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.runtime.TinyflowService;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStoreProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.InMemoryTinyflowWorkflowRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowWorkflowRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowWorkflowValidator;
import dev.tinyflow.core.chain.repository.InMemoryNodeStateRepository;
import dev.tinyflow.core.chain.repository.NodeStateRepository;
import dev.tinyflow.core.chain.runtime.ChainExecutor;
import dev.tinyflow.core.llm.LlmProvider;
import dev.tinyflow.core.parser.ChainParser;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConditionalOnClass(ChainExecutor.class)
@ConditionalOnProperty(prefix = "fw.plus.ai.tinyflow", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(TinyflowProperties.class)
public class TinyflowAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public TinyflowWorkflowRepository tinyflowWorkflowRepository() {
		return new InMemoryTinyflowWorkflowRepository();
	}

	@Bean
	@ConditionalOnMissingBean
	public TinyflowChainStateRepository tinyflowChainStateRepository() {
		return new InMemoryTinyflowChainStateRepository();
	}

	@Bean
	@ConditionalOnMissingBean(TinyflowChainDefinitionRepository.class)
	public TinyflowChainDefinitionRepository tinyflowChainDefinitionRepository() {
		return new InMemoryChainDefinitionRepository();
	}

	@Bean
	@ConditionalOnMissingBean
	public TinyflowNodeProviderRegistry tinyflowNodeProviderRegistry(List<TinyflowNodeProvider> providers) {
		return new TinyflowNodeProviderRegistry(providers);
	}

	@Bean
	@ConditionalOnMissingBean
	public ChainParser tinyflowChainParser(TinyflowNodeProviderRegistry registry) {
		return ChainParser.builder().addParsers(registry.getParsers()).build();
	}

	@Bean
	@ConditionalOnMissingBean(NodeStateRepository.class)
	public NodeStateRepository tinyflowNodeStateRepository() {
		return new InMemoryNodeStateRepository();
	}

	@Bean
	@ConditionalOnMissingBean
	public ChainExecutor tinyflowChainExecutor(TinyflowChainDefinitionRepository definitionRepository,
			TinyflowChainStateRepository stateRepository, NodeStateRepository nodeStateRepository) {
		return new ChainExecutor(definitionRepository, stateRepository, nodeStateRepository);
	}

	@Bean
	@ConditionalOnMissingBean
	public TinyflowWorkflowValidator tinyflowWorkflowValidator(ChainParser chainParser) {
		return new TinyflowWorkflowValidator(chainParser);
	}

	@Bean
	@ConditionalOnMissingBean
	public TinyflowService tinyflowService(TinyflowWorkflowValidator validator,
			TinyflowWorkflowRepository workflowRepository, TinyflowChainDefinitionRepository definitionRepository,
			ChainExecutor chainExecutor, TinyflowProperties properties) {
		return new DefaultTinyflowService(validator, workflowRepository, definitionRepository, chainExecutor,
				properties.getExecutionTimeout());
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnClass(name = "com.xiongx9527.fw.plus.ai." + "lang" + "chain4j.adapter.model.LangChain4jModelRegistry")
	public TinyflowModelProvider tinyflowModelProvider(ObjectProvider<AiModelRegistry> modelRegistry,
			org.springframework.context.ApplicationContext applicationContext) {
		return RegistryTinyflowModelProvider.create(modelRegistry.getIfAvailable(), applicationContext);
	}

	@Bean
	@ConditionalOnMissingBean
	public TinyflowVectorStoreProvider tinyflowVectorStoreProvider(
			ObjectProvider<AiEmbeddingStoreRegistry> storeRegistry) {
		return new RegistryTinyflowVectorStoreProvider(storeRegistry.getIfAvailable());
	}

	@Bean
	@ConditionalOnMissingBean(LlmProvider.class)
	@org.springframework.boot.autoconfigure.condition.ConditionalOnBean(TinyflowModelProvider.class)
	@ConditionalOnClass(name = "dev.tinyflow." + "lang" + "chain4j.provider.Langchain4jLlm")
	public LlmProvider tinyflowLlmProvider(TinyflowModelProvider modelProvider) {
		return RegistryTinyflowLlmProvider.create(modelProvider);
	}

	@Bean
	@ConditionalOnMissingBean
	@org.springframework.boot.autoconfigure.condition.ConditionalOnBean(LlmProvider.class)
	public TinyflowLlmLifecycle tinyflowLlmLifecycle(LlmProvider provider) {
		return new TinyflowLlmLifecycle(provider);
	}
}
