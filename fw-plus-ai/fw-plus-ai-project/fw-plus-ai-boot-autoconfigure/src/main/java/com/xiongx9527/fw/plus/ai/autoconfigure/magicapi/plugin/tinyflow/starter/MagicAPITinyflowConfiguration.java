package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.starter;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.service.MagicApiTinyflowWorkflowRepository;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.provider.MagicApiTinyflowModelProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.provider.MagicApiTinyflowVectorStoreProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.web.MagicTinyflowController;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.MagicAPIBaseAutoConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.model.MagicDynamicModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.model.MagicDynamicVectorDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow.TinyflowAutoConfiguration;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowModelProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStoreProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowWorkflowRepository;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;
import dev.tinyflow.core.chain.runtime.ChainExecutor;

@Configuration
@AutoConfigureAfter(value = MagicAPIBaseAutoConfiguration.class,
		name = "com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.MagicAPIBaseAutoConfiguration")
@AutoConfigureBefore(TinyflowAutoConfiguration.class)
@ConditionalOnClass({MagicResourceService.class, ChainExecutor.class})
@ConditionalOnBean(MagicResourceService.class)
@ConditionalOnExpression("${magic-api.plugin.tinyflow.enabled:true} && ${fw.plus.ai.tinyflow.enabled:true}")
public class MagicAPITinyflowConfiguration implements MagicPluginConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public TinyflowWorkflowRepository magicApiTinyflowWorkflowRepository() {
		return new MagicApiTinyflowWorkflowRepository();
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(MagicDynamicModelDataSource.class)
	public TinyflowModelProvider magicApiTinyflowModelProvider(MagicDynamicModelDataSource dataSource) {
		return new MagicApiTinyflowModelProvider(dataSource);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(MagicDynamicVectorDataSource.class)
	public TinyflowVectorStoreProvider magicApiTinyflowVectorStoreProvider(MagicDynamicVectorDataSource dataSource) {
		return new MagicApiTinyflowVectorStoreProvider(dataSource);
	}

	@Override
	public Plugin plugin() {
		return new Plugin("tinyflow", "Tinyflow", "magic-tinyflow.1.0.0.iife.js");
	}

	@Override
	public MagicControllerRegister controllerRegister() {
		return (mapping, configuration) -> mapping.registerController(new MagicTinyflowController(configuration));
	}
}
