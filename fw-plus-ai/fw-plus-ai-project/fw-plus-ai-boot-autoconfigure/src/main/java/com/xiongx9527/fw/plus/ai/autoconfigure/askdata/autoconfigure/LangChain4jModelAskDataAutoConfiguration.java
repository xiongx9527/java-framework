package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.autoconfigure;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.LangChain4jAskDataModelSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.LangChain4jModelRegistryAskDataModelSource;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelRegistry;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@AutoConfigureBefore(AskDataAutoConfiguration.class)
@ConditionalOnClass(name = "com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelRegistry")
public class LangChain4jModelAskDataAutoConfiguration {

	@Bean
	@ConditionalOnBean(LangChain4jModelRegistry.class)
	@ConditionalOnMissingBean(LangChain4jAskDataModelSource.class)
	public LangChain4jAskDataModelSource langChain4jAskDataModelSource(LangChain4jModelRegistry modelRegistry) {
		return new LangChain4jModelRegistryAskDataModelSource(modelRegistry);
	}
}
