package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineApiFacade;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineCredentialExtractor;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineResourceController;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineUserSystemService;
import com.xiongx9527.fw.plus.tinyengine.service.ResourceService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "fw.plus.tiny-engine.web", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TinyEngineServletWebConfiguration {

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

	@Bean
	@ConditionalOnBean(TinyEngineApiFacade.class)
	@ConditionalOnMissingBean
	public TinyEngineAppMvcController tinyEngineAppMvcController(TinyEngineApiFacade facade) {
		return new TinyEngineAppMvcController(facade);
	}

	@Bean
	@ConditionalOnBean(TinyEngineApiFacade.class)
	@ConditionalOnMissingBean
	public TinyEnginePageMvcController tinyEnginePageMvcController(TinyEngineApiFacade facade) {
		return new TinyEnginePageMvcController(facade);
	}

	@Bean
	@ConditionalOnBean(TinyEngineUserSystemService.class)
	@ConditionalOnMissingBean
	public TinyEngineUserMvcController tinyEngineUserMvcController(TinyEngineUserSystemService userSystemService,
			TinyEngineCredentialExtractor credentialExtractor) {
		return new TinyEngineUserMvcController(userSystemService, credentialExtractor);
	}
}
