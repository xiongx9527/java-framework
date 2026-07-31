package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.TinyEngineProperties;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineCredentialExtractor;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineResourceController;
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
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass(name = {
		"reactor.core.publisher.Mono",
		"reactor.core.scheduler.Scheduler"
})
@ConditionalOnProperty(prefix = "fw.plus.tiny-engine.web", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TinyEngineReactiveWebConfiguration {

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
	@ConditionalOnBean({TinyEngineUserSystemService.class, TinyEngineReactiveExecutor.class,
			TinyEngineRequestUserScope.class})
	@ConditionalOnMissingBean
	public TinyEngineUserWebFluxController tinyEngineUserWebFluxController(TinyEngineUserSystemService userSystemService,
			TinyEngineCredentialExtractor credentialExtractor, TinyEngineReactiveExecutor executor,
			TinyEngineRequestUserScope userScope) {
		return new TinyEngineUserWebFluxController(userSystemService, credentialExtractor, executor, userScope);
	}
}
