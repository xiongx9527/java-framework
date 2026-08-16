package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.autoconfigure;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataModelRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.LangChain4jAskDataModelSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.LangChain4jAskDataModelRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.LangChain4jModelRegistryAskDataModelSource;
import com.xiongx9527.fw.plus.ai.model.AiModelRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.util.Collections;
import java.util.stream.Collectors;

@AutoConfiguration
@AutoConfigureBefore(AskDataAutoConfiguration.class)
@ConditionalOnClass(name = {
		"com.xiongx9527.fw.plus.ai.model.AiModelRegistry",
		"com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jAiModel",
		"dev.langchain4j.model.chat.ChatModel",
		"dev.langchain4j.model.embedding.EmbeddingModel"
})
@ConditionalOnProperty(prefix = "fw.plus.ai.askdata", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LangChain4jModelAskDataAutoConfiguration {

	@Bean
	@ConditionalOnBean(AiModelRegistry.class)
	@ConditionalOnMissingBean(LangChain4jAskDataModelSource.class)
	public LangChain4jAskDataModelSource langChain4jAskDataModelSource(AiModelRegistry modelRegistry) {
		return new LangChain4jModelRegistryAskDataModelSource(modelRegistry);
	}

	@Bean
	@ConditionalOnBean(AiModelRegistry.class)
	@ConditionalOnMissingBean(AskDataModelRegistry.class)
	public AskDataModelRegistry langChain4jAskDataModelRegistry(AskDataProperties properties,
			ObjectProvider<LangChain4jAskDataModelSource> modelSourceProvider) {
		return new LangChain4jAskDataModelRegistry(properties, Collections.emptyMap(), Collections.emptyMap(),
				modelSourceProvider.orderedStream().collect(Collectors.toList()));
	}
}
