package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.starter;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.service.MagicApiTinyflowWorkflowRepository;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.MagicAPIBaseAutoConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow.TinyflowAutoConfiguration;
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

	@Override
	public Plugin plugin() {
		return new Plugin("tinyflow", "Tinyflow", "magic-tinyflow.1.0.0.iife.js");
	}
}
