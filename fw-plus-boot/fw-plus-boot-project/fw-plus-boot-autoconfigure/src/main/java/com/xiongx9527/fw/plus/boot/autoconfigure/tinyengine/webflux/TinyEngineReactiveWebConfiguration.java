package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.TinyEngineProperties;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineApiFacade;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineExternalCapability;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineCredentialExtractor;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineResourceController;
import com.xiongx9527.fw.plus.tinyengine.dynamic.service.DynamicService;
import com.xiongx9527.fw.plus.tinyengine.service.ResourceService;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineUserSystemService;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineRequestUserScope;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass(name = {
		"reactor.core.publisher.Mono",
		"reactor.core.scheduler.Scheduler"
})
@ConditionalOnProperty(prefix = "fw.plus.tiny-engine.web", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TinyEngineReactiveWebConfiguration implements WebFluxConfigurer {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/tiny-engine/**").addResourceLocations("classpath:/tiny-engine/");
	}

	@Bean
	@ConditionalOnMissingBean
	public TinyEngineCredentialExtractor tinyEngineCredentialExtractor() {
		return new TinyEngineCredentialExtractor();
	}

	@Bean
	@ConditionalOnBean(ResourceService.class)
	@ConditionalOnMissingBean
	public TinyEngineResourceController tinyEngineResourceController(ResourceService resourceService) {
		return new TinyEngineResourceController(resourceService);
	}

	@Bean(name = "tinyEngineBlockingScheduler", destroyMethod = "dispose")
	@ConditionalOnMissingBean(name = "tinyEngineBlockingScheduler")
	public Scheduler tinyEngineBlockingScheduler(TinyEngineProperties properties) {
		TinyEngineProperties.Webflux.Scheduler scheduler = properties.getWebflux().getScheduler();
		return Schedulers.newBoundedElastic(scheduler.getThreadCount(), scheduler.getQueueCapacity(),
				"tiny-engine-blocking");
	}

	@Bean
	@ConditionalOnMissingBean
	public TinyEngineReactiveExecutor tinyEngineReactiveExecutor(
			@Qualifier("tinyEngineBlockingScheduler") Scheduler scheduler, TinyEngineProperties properties) {
		return new TinyEngineReactiveExecutor(scheduler, properties.getWebflux().getScheduler().getTimeout());
	}

	@Bean
	@ConditionalOnBean({TinyEngineApiFacade.class, TinyEngineReactiveExecutor.class})
	@ConditionalOnMissingBean
	public TinyEngineAppWebFluxController tinyEngineAppWebFluxController(TinyEngineApiFacade facade,
			TinyEngineReactiveExecutor executor) {
		return new TinyEngineAppWebFluxController(facade, executor);
	}

	@Bean
	@ConditionalOnBean({TinyEngineApiFacade.class, TinyEngineReactiveExecutor.class})
	@ConditionalOnMissingBean
	public TinyEnginePageWebFluxController tinyEnginePageWebFluxController(TinyEngineApiFacade facade,
			TinyEngineReactiveExecutor executor) {
		return new TinyEnginePageWebFluxController(facade, executor);
	}

	@Bean
	@ConditionalOnBean({TinyEngineApiFacade.class, TinyEngineReactiveExecutor.class})
	@ConditionalOnMissingBean
	public TinyEngineMaterialWebFluxController tinyEngineMaterialWebFluxController(TinyEngineApiFacade facade,
			TinyEngineReactiveExecutor executor) {
		return new TinyEngineMaterialWebFluxController(facade, executor);
	}

	@Bean
	@ConditionalOnBean({TinyEngineApiFacade.class, TinyEngineReactiveExecutor.class})
	@ConditionalOnMissingBean
	public TinyEngineModelWebFluxController tinyEngineModelWebFluxController(TinyEngineApiFacade facade,
			TinyEngineReactiveExecutor executor) {
		return new TinyEngineModelWebFluxController(facade, executor);
	}

	@Bean
	@ConditionalOnBean({TinyEngineApiFacade.class, TinyEngineReactiveExecutor.class})
	@ConditionalOnMissingBean
	public TinyEngineHistoryWebFluxController tinyEngineHistoryWebFluxController(TinyEngineApiFacade facade,
			TinyEngineReactiveExecutor executor) {
		return new TinyEngineHistoryWebFluxController(facade, executor);
	}

	@Bean
	@ConditionalOnBean({TinyEngineApiFacade.class, TinyEngineReactiveExecutor.class})
	@ConditionalOnMissingBean
	public TinyEngineI18nWebFluxController tinyEngineI18nWebFluxController(TinyEngineApiFacade facade,
			TinyEngineReactiveExecutor executor) {
		return new TinyEngineI18nWebFluxController(facade, executor);
	}

	@Bean
	@ConditionalOnBean({TinyEngineApiFacade.class, TinyEngineReactiveExecutor.class})
	@ConditionalOnMissingBean
	public TinyEngineTaskWebFluxController tinyEngineTaskWebFluxController(TinyEngineApiFacade facade,
			TinyEngineReactiveExecutor executor) {
		return new TinyEngineTaskWebFluxController(facade, executor);
	}

	@Bean
	@ConditionalOnBean({TinyEngineExternalCapability.class, TinyEngineReactiveExecutor.class})
	@ConditionalOnMissingBean
	public TinyEnginePlatformWebFluxController tinyEnginePlatformWebFluxController(
			TinyEngineExternalCapability externalCapability, TinyEngineReactiveExecutor executor) {
		return new TinyEnginePlatformWebFluxController(externalCapability, executor);
	}

	@Bean
	@ConditionalOnBean({TinyEngineExternalCapability.class, TinyEngineReactiveExecutor.class})
	@ConditionalOnMissingBean
	public TinyEngineCodegenWebFluxController tinyEngineCodegenWebFluxController(
			TinyEngineExternalCapability externalCapability, TinyEngineReactiveExecutor executor) {
		return new TinyEngineCodegenWebFluxController(externalCapability, executor);
	}

	@Bean
	@ConditionalOnBean({DynamicService.class, TinyEngineReactiveExecutor.class})
	@ConditionalOnMissingBean
	public TinyEngineDynamicModelWebFluxController tinyEngineDynamicModelWebFluxController(
			DynamicService dynamicService, TinyEngineReactiveExecutor executor) {
		return new TinyEngineDynamicModelWebFluxController(dynamicService, executor);
	}

	@Bean
	@ConditionalOnBean({TinyEngineUserSystemService.class, TinyEngineReactiveExecutor.class,
			TinyEngineRequestUserScope.class})
	@ConditionalOnMissingBean
	public TinyEngineUserWebFluxController tinyEngineUserWebFluxController(TinyEngineUserSystemService userSystemService,
			TinyEngineCredentialExtractor credentialExtractor, TinyEngineReactiveExecutor executor,
			TinyEngineRequestUserScope userScope) {
		return new TinyEngineUserWebFluxController(userSystemService, credentialExtractor, executor, userScope);
	}
}
