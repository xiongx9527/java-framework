package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.autoconfigure;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataService;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.DefaultAskDataService;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.AskDataContextCompressor;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.AskDataConversationStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.DefaultAskDataContextCompressor;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.InMemoryAskDataConversationStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow.AskDataAgentDispatcher;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow.AskDataAgent;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow.AskDataFlowExecutor;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow.AskDataSubAgent;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow.DefaultAskDataFlowExecutor;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow.SequentialAskDataAgentDispatcher;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataMetadataProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.EmptyAskDataMetadataProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataModelRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.LangChain4jAskDataModelRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.prompt.AskDataDialectRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.prompt.AskDataPromptTemplateProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.prompt.DefaultAskDataPromptTemplateProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql.AskDataSqlExecutor;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql.AskDataSqlValidator;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql.DefaultAskDataSqlValidator;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql.JdbcAskDataSqlExecutor;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector.AskDataVectorRetriever;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector.CompositeAskDataVectorRetriever;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector.ContentRetrieverAskDataVectorRetriever;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelRegistry;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.stream.Collectors;

@AutoConfiguration
@ConditionalOnProperty(prefix = "fw.plus.ai.askdata", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties
public class AskDataAutoConfiguration {

	@Bean
	@ConfigurationProperties(prefix = "fw.plus.ai.askdata")
	@ConditionalOnMissingBean
	public AskDataProperties askDataProperties() {
		return new AskDataProperties();
	}

	@Bean
	@ConditionalOnMissingBean
	public AskDataModelRegistry askDataModelRegistry(AskDataProperties properties,
			ObjectProvider<LangChain4jModelRegistry> modelRegistryProvider) {
		return new LangChain4jAskDataModelRegistry(properties, modelRegistryProvider.getIfAvailable());
	}

    @Bean
    @ConditionalOnMissingBean
    public AskDataMetadataProvider askDataMetadataProvider() {
        return new EmptyAskDataMetadataProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public AskDataDialectRegistry askDataDialectRegistry() {
        return new AskDataDialectRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public AskDataPromptTemplateProvider askDataPromptTemplateProvider(AskDataDialectRegistry dialectRegistry) {
        return new DefaultAskDataPromptTemplateProvider(dialectRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public AskDataSqlValidator askDataSqlValidator(AskDataProperties properties) {
        return new DefaultAskDataSqlValidator(properties.getSql().isEnforceLimit());
    }

    @Bean
    @ConditionalOnMissingBean
    public AskDataSqlExecutor askDataSqlExecutor(AskDataProperties properties,
            ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        Integer defaultMaxRows = properties.getSql().getDefaultMaxRows();
        return new JdbcAskDataSqlExecutor(jdbcTemplateProvider,
                defaultMaxRows == null ? 100 : defaultMaxRows.intValue());
    }

    @Bean
    @ConditionalOnMissingBean
    public AskDataVectorRetriever askDataVectorRetriever(AskDataProperties properties,
            ObjectProvider<ContentRetriever> contentRetrieverProvider) {
        List<AskDataVectorRetriever> retrievers = contentRetrieverProvider.orderedStream()
                .map(ContentRetrieverAskDataVectorRetriever::new)
                .collect(Collectors.<AskDataVectorRetriever>toList());
        return new CompositeAskDataVectorRetriever(retrievers, properties.getVector().getDefaultTopK());
    }

    @Bean
    @ConditionalOnMissingBean
    public AskDataAgentDispatcher askDataAgentDispatcher(ObjectProvider<AskDataSubAgent> subAgentProvider) {
        return new SequentialAskDataAgentDispatcher(subAgentProvider.orderedStream().collect(Collectors.toList()));
    }

    @Bean
    @ConditionalOnMissingBean
    public AskDataConversationStore askDataConversationStore() {
        return new InMemoryAskDataConversationStore();
    }

    @Bean
    @ConditionalOnMissingBean
    public AskDataContextCompressor askDataContextCompressor(AskDataProperties properties) {
        return new DefaultAskDataContextCompressor(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public AskDataFlowExecutor askDataFlowExecutor(AskDataProperties properties, AskDataModelRegistry modelRegistry,
            AskDataMetadataProvider metadataProvider, AskDataVectorRetriever vectorRetriever,
            AskDataPromptTemplateProvider promptTemplateProvider, AskDataDialectRegistry dialectRegistry,
            AskDataSqlValidator sqlValidator, AskDataSqlExecutor sqlExecutor, AskDataConversationStore conversationStore,
            AskDataContextCompressor contextCompressor, AskDataAgentDispatcher agentDispatcher,
            ObjectProvider<AskDataAgent> agentProvider) {
        List<AskDataAgent> mainAgents = agentProvider.orderedStream()
                .filter(agent -> !(agent instanceof AskDataSubAgent))
                .collect(Collectors.toList());
        return new DefaultAskDataFlowExecutor(properties, modelRegistry, metadataProvider, vectorRetriever,
                promptTemplateProvider, dialectRegistry, sqlValidator, sqlExecutor, conversationStore, contextCompressor,
                agentDispatcher, mainAgents);
    }

    @Bean
    @ConditionalOnMissingBean
    public AskDataService askDataService(AskDataFlowExecutor flowExecutor) {
        return new DefaultAskDataService(flowExecutor);
    }
}
