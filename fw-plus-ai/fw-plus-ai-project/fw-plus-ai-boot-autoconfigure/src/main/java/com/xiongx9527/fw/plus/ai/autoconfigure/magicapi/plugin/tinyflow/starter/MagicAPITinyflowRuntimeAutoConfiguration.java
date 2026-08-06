package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.starter;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.service.TinyflowMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.service.TinyflowResourceDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.module.TinyflowModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow.TinyflowAutoConfiguration;
import com.xiongx9527.fw.plus.ai.tinyflow.node.TinyflowNodeProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.node.TinyflowNodeProviderRegistry;
import com.xiongx9527.fw.plus.ai.tinyflow.runtime.TinyflowService;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowWorkflowRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowWorkflowValidator;
import dev.tinyflow.core.chain.runtime.ChainExecutor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.service.MagicResourceService;

import java.util.List;

@Configuration
@AutoConfigureAfter(value = TinyflowAutoConfiguration.class,
		name = "com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.MagicAPIBaseAutoConfiguration")
@ConditionalOnClass({MagicResourceService.class, ChainExecutor.class})
@ConditionalOnBean({MagicResourceService.class, TinyflowWorkflowValidator.class, TinyflowService.class})
@ConditionalOnExpression("${magic-api.plugin.tinyflow.enabled:true} && ${fw.plus.ai.tinyflow.enabled:true}")
public class MagicAPITinyflowRuntimeAutoConfiguration {

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

	@Bean
	@ConditionalOnMissingBean
	public TinyflowNodeProviderRegistry tinyflowNodeProviderRegistry(List<TinyflowNodeProvider> providers) {
		return new TinyflowNodeProviderRegistry(providers);
	}

	@Bean
	@ConditionalOnMissingBean
	public TinyflowModule tinyflowModule(TinyflowWorkflowRepository repository, TinyflowService tinyflowService) {
		return new TinyflowModule(repository, tinyflowService);
	}
}
