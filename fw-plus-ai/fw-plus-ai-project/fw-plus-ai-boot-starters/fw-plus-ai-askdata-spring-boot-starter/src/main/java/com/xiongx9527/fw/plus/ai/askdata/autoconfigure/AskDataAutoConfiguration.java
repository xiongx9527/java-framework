package com.xiongx9527.fw.plus.ai.askdata.autoconfigure;

import com.xiongx9527.fw.plus.ai.askdata.AskDataProperties;
import com.xiongx9527.fw.plus.ai.askdata.metadata.AskDataMetadataProvider;
import com.xiongx9527.fw.plus.ai.askdata.metadata.EmptyAskDataMetadataProvider;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRegistry;
import com.xiongx9527.fw.plus.ai.askdata.model.LangChain4jAskDataModelRegistry;
import com.xiongx9527.fw.plus.ai.askdata.prompt.AskDataDialectRegistry;
import com.xiongx9527.fw.plus.ai.askdata.prompt.AskDataPromptTemplateProvider;
import com.xiongx9527.fw.plus.ai.askdata.prompt.DefaultAskDataPromptTemplateProvider;
import com.xiongx9527.fw.plus.ai.askdata.vector.AskDataVectorRetriever;
import com.xiongx9527.fw.plus.ai.askdata.vector.CompositeAskDataVectorRetriever;
import com.xiongx9527.fw.plus.ai.askdata.vector.ContentRetrieverAskDataVectorRetriever;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.LangChain4jModelInfo;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.Collections;

@AutoConfiguration
@EnableConfigurationProperties(AskDataProperties.class)
public class AskDataAutoConfiguration {

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
}
