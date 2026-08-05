package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.starter;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.service.MagicApiTinyflowWorkflowRepository;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.service.TinyflowMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.service.TinyflowResourceDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow.TinyflowAutoConfiguration;
import com.xiongx9527.fw.plus.ai.tinyflow.runtime.TinyflowService;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowWorkflowRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowWorkflowValidator;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.service.MagicResourceService;

@Configuration
@AutoConfigureBefore(TinyflowAutoConfiguration.class)
@ConditionalOnClass(MagicResourceService.class)
@ConditionalOnProperty(prefix = "magic-api.plugin.tinyflow", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MagicAPITinyflowConfiguration implements MagicPluginConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public TinyflowWorkflowRepository magicApiTinyflowWorkflowRepository() {
		return new MagicApiTinyflowWorkflowRepository();
	}

	@Bean
	@ConditionalOnMissingBean
	public TinyflowMagicResourceStorage tinyflowMagicResourceStorage(TinyflowWorkflowValidator validator) {
		return new TinyflowMagicResourceStorage(validator);
	}

	@Bean
	@ConditionalOnMissingBean
	public TinyflowResourceDynamicRegistry tinyflowResourceDynamicRegistry(TinyflowMagicResourceStorage storage,
			TinyflowService tinyflowService) {
		return new TinyflowResourceDynamicRegistry(storage, tinyflowService);
	}

	@Override
	public Plugin plugin() {
		return new Plugin("tinyflow", "Tinyflow", "magic-tinyflow.1.0.0.iife.js");
	}
}
