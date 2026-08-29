package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.tinyflow;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.model.MagicDynamicModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.starter.MagicAPITinyflowConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow.TinyflowAutoConfiguration;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowModelProvider;
import dev.tinyflow.core.chain.runtime.ChainExecutor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.service.MagicResourceService;

@Configuration
@AutoConfigureAfter(MagicAPITinyflowConfiguration.class)
@AutoConfigureBefore(TinyflowAutoConfiguration.class)
@ConditionalOnClass(value = {MagicDynamicModelDataSource.class, MagicResourceService.class, ChainExecutor.class},
		name = "com.xiongx9527.fw.plus.ai.langchain4j.adapter.model.LangChain4jModelRegistry")
@ConditionalOnBean({MagicResourceService.class, MagicDynamicModelDataSource.class})
@ConditionalOnExpression("${magic-api.plugin.tinyflow.enabled:true} && ${fw.plus.ai.tinyflow.enabled:true}")
public class MagicApiTinyflowLangChain4jAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(TinyflowModelProvider.class)
	public TinyflowModelProvider magicApiTinyflowModelProvider(MagicDynamicModelDataSource dataSource) {
		return new MagicApiTinyflowModelProvider(dataSource);
	}
}
