package com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow.dify;

import com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow.TinyflowKnowledgeLifecycle;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeAccessKeyRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeManagerAdapter;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeProviderRegistry;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRepository;
import com.xiongx9527.fw.plus.dify.dataset.IKnowledgeService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

@Configuration
@ConditionalOnClass(IKnowledgeService.class)
@ConditionalOnBean({TinyflowKnowledgeProviderRegistry.class, TinyflowKnowledgeRepository.class,
		TinyflowKnowledgeAccessKeyRepository.class})
@ConditionalOnProperty(prefix = TinyflowDifyExternalProperties.PREFIX, name = "enabled", havingValue = "true")
@EnableConfigurationProperties(TinyflowDifyExternalProperties.class)
public class TinyflowDifyExternalAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public TinyflowKnowledgeAccessKeyService tinyflowKnowledgeAccessKeyService(
			TinyflowKnowledgeAccessKeyRepository repository) {
		return new TinyflowKnowledgeAccessKeyService(repository);
	}

	@Bean
	@ConditionalOnMissingBean(TinyflowDifyExternalKnowledgeService.class)
	@Conditional(TinyflowLocalProviderCondition.class)
	public TinyflowDifyExternalKnowledgeService tinyflowDifyExternalKnowledgeService(
			TinyflowKnowledgeProviderRegistry providers, TinyflowKnowledgeRepository knowledgeRepository,
			TinyflowKnowledgeAccessKeyService accessKeyService, TinyflowDifyExternalProperties properties) {
		return new TinyflowDifyExternalKnowledgeService(providers, knowledgeRepository, accessKeyService, properties);
	}

	@Bean
	@ConditionalOnMissingBean
	public TinyflowKnowledgeManagerAdapter tinyflowKnowledgeManagerAdapter(
			TinyflowKnowledgeProviderRegistry providers) {
		return new TinyflowKnowledgeManagerAdapter(providers);
	}

	@Bean
	@ConditionalOnMissingBean
	public TinyflowKnowledgeLifecycle tinyflowKnowledgeLifecycle(TinyflowKnowledgeManagerAdapter adapter) {
		return new TinyflowKnowledgeLifecycle(adapter);
	}

	@Bean
	@ConditionalOnBean(TinyflowDifyExternalKnowledgeService.class)
	@ConditionalOnMissingBean
	public TinyflowDifyExternalController tinyflowDifyExternalController(
			TinyflowDifyExternalKnowledgeService service) {
		return new TinyflowDifyExternalController(service);
	}
}

final class TinyflowLocalProviderCondition implements Condition {

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		if (context.getBeanFactory() == null) {
			return false;
		}
		String[] names = context.getBeanFactory().getBeanNamesForType(
				TinyflowKnowledgeProviderRegistry.class, false, false);
		for (String name : names) {
			TinyflowKnowledgeProviderRegistry registry =
					context.getBeanFactory().getBean(name, TinyflowKnowledgeProviderRegistry.class);
			if (registry.get("fw-plus-local") != null) {
				return true;
			}
		}
		return false;
	}
}
