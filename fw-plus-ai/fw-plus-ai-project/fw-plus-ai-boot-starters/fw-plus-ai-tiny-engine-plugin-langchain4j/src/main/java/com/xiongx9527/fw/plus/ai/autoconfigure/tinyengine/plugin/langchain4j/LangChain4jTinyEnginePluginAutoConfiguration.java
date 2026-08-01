package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.plugin.langchain4j;

import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.TinyEngineAiProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.provider.TinyEngineAiProvider;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jAiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass({TinyEngineAiProvider.class, LangChain4jAiModel.class})
@ConditionalOnProperty(prefix = "fw.plus.tiny-engine.ai.plugin.langchain4j", name = "enabled",
		havingValue = "true", matchIfMissing = true)
@AutoConfigureAfter(name = {
		"com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.TinyEngineAiAutoConfiguration",
		"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.LangChain4jAdapterAutoConfiguration"
})
@EnableConfigurationProperties(TinyEngineAiProperties.class)
public class LangChain4jTinyEnginePluginAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(name = "langChain4jTinyEngineAiProvider")
	public TinyEngineAiProvider langChain4jTinyEngineAiProvider(ObjectProvider<AiModelRegistry> modelRegistryProvider,
			TinyEngineAiProperties properties) {
		return new LangChain4jTinyEngineAiProvider(modelRegistryProvider.getIfAvailable(), properties);
	}
}
