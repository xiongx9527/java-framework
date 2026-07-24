package com.xiongx9527.fw.plus.ai.askdata.autoconfigure;

import com.xiongx9527.fw.plus.ai.askdata.AskDataProperties;
import com.xiongx9527.fw.plus.ai.askdata.AskDataService;
import com.xiongx9527.fw.plus.ai.askdata.DefaultAskDataService;
import com.xiongx9527.fw.plus.ai.askdata.conversation.AskDataContextCompressor;
import com.xiongx9527.fw.plus.ai.askdata.conversation.AskDataConversationStore;
import com.xiongx9527.fw.plus.ai.askdata.conversation.DefaultAskDataContextCompressor;
import com.xiongx9527.fw.plus.ai.askdata.conversation.InMemoryAskDataConversationStore;
import com.xiongx9527.fw.plus.ai.askdata.flow.AskDataAgentDispatcher;
import com.xiongx9527.fw.plus.ai.askdata.flow.AskDataFlowExecutor;
import com.xiongx9527.fw.plus.ai.askdata.flow.AskDataSubAgent;
import com.xiongx9527.fw.plus.ai.askdata.flow.DefaultAskDataFlowExecutor;
import com.xiongx9527.fw.plus.ai.askdata.flow.SequentialAskDataAgentDispatcher;
import com.xiongx9527.fw.plus.ai.askdata.metadata.AskDataMetadataProvider;
import com.xiongx9527.fw.plus.ai.askdata.metadata.EmptyAskDataMetadataProvider;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRegistry;
import com.xiongx9527.fw.plus.ai.askdata.model.LangChain4jAskDataModelRegistry;
import com.xiongx9527.fw.plus.ai.askdata.prompt.AskDataDialectRegistry;
import com.xiongx9527.fw.plus.ai.askdata.prompt.AskDataPromptTemplateProvider;
import com.xiongx9527.fw.plus.ai.askdata.prompt.DefaultAskDataPromptTemplateProvider;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlExecutor;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlValidator;
import com.xiongx9527.fw.plus.ai.askdata.sql.DefaultAskDataSqlValidator;
import com.xiongx9527.fw.plus.ai.askdata.sql.JdbcAskDataSqlExecutor;
import com.xiongx9527.fw.plus.ai.askdata.vector.AskDataVectorRetriever;
import com.xiongx9527.fw.plus.ai.askdata.vector.CompositeAskDataVectorRetriever;
import com.xiongx9527.fw.plus.ai.askdata.vector.ContentRetrieverAskDataVectorRetriever;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.LangChain4jModelInfo;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;
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
			ObjectProvider<LangChain4jModelInfo> modelInfoProvider) {
		return new LangChain4jAskDataModelRegistry(properties, modelInfoProvider.getIfAvailable());
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
    public AskDataSqlValidator askDataSqlValidator() {
        return new DefaultAskDataSqlValidator();
    }

    @Bean
    @ConditionalOnMissingBean
    public AskDataSqlExecutor askDataSqlExecutor(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        return new JdbcAskDataSqlExecutor(jdbcTemplateProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    public AskDataVectorRetriever askDataVectorRetriever(AskDataProperties properties,
            ObjectProvider<ContentRetriever> contentRetrieverProvider) {
        ContentRetriever contentRetriever = contentRetrieverProvider.getIfAvailable();
        if (contentRetriever == null) {
            return new CompositeAskDataVectorRetriever(Collections.<AskDataVectorRetriever>emptyList(),
                    properties.getVector().getDefaultTopK());
        }
        return new CompositeAskDataVectorRetriever(Collections.<AskDataVectorRetriever>singletonList(
                new ContentRetrieverAskDataVectorRetriever(contentRetriever)), properties.getVector().getDefaultTopK());
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
            AskDataContextCompressor contextCompressor, AskDataAgentDispatcher agentDispatcher) {
        return new DefaultAskDataFlowExecutor(properties, modelRegistry, metadataProvider, vectorRetriever,
                promptTemplateProvider, dialectRegistry, sqlValidator, sqlExecutor, conversationStore, contextCompressor,
                agentDispatcher);
    }

    @Bean
    @ConditionalOnMissingBean
    public AskDataService askDataService(AskDataFlowExecutor flowExecutor) {
        return new DefaultAskDataService(flowExecutor);
    }
}
