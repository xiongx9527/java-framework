package com.xiongx9527.fw.plus.ai.askdata.autoconfigure;

import com.xiongx9527.fw.plus.ai.askdata.AskDataProperties;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRegistry;
import com.xiongx9527.fw.plus.ai.askdata.model.LangChain4jAskDataModelRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.LangChain4jModelInfo;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(AskDataProperties.class)
public class AskDataAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public AskDataModelRegistry askDataModelRegistry(AskDataProperties properties,
			ObjectProvider<LangChain4jModelInfo> modelInfoProvider) {
		return new LangChain4jAskDataModelRegistry(properties, modelInfoProvider.getIfAvailable());
	}
}
