package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineApiFacade;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineExternalCapability;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineCredentialExtractor;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineResourceController;
import com.xiongx9527.fw.plus.tinyengine.dynamic.service.DynamicService;
import com.xiongx9527.fw.plus.tinyengine.service.ResourceService;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineUserSystemService;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineRequestUserScope;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "fw.plus.tiny-engine.web", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TinyEngineServletWebConfiguration implements WebMvcConfigurer {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/tiny-engine/**")
				.addResourceLocations("classpath:/META-INF/resources/tiny-engine/");
	}

	@Bean
	@ConditionalOnMissingBean
	public TinyEngineCredentialExtractor tinyEngineCredentialExtractor() {
		return new TinyEngineCredentialExtractor();
	}

	@Bean
	@ConditionalOnBean(TinyEngineRequestUserScope.class)
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "fw.plus.tiny-engine.security", name = "enabled", havingValue = "true")
	public TinyEngineServletAuthenticationFilter tinyEngineServletAuthenticationFilter(
			TinyEngineCredentialExtractor credentialExtractor, TinyEngineUserSystemService userSystemService,
			TinyEngineRequestUserScope userScope) {
		return new TinyEngineServletAuthenticationFilter(credentialExtractor, userSystemService, userScope);
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
	@ConditionalOnBean(TinyEngineApiFacade.class)
	@ConditionalOnMissingBean
	public TinyEngineHistoryMvcController tinyEngineHistoryMvcController(TinyEngineApiFacade facade) {
		return new TinyEngineHistoryMvcController(facade);
	}

	@Bean
	@ConditionalOnBean(TinyEngineApiFacade.class)
	@ConditionalOnMissingBean
	public TinyEngineI18nMvcController tinyEngineI18nMvcController(TinyEngineApiFacade facade) {
		return new TinyEngineI18nMvcController(facade);
	}

	@Bean
	@ConditionalOnBean(TinyEngineApiFacade.class)
	@ConditionalOnMissingBean
	public TinyEngineTaskMvcController tinyEngineTaskMvcController(TinyEngineApiFacade facade) {
		return new TinyEngineTaskMvcController(facade);
	}

	@Bean
	@ConditionalOnBean(TinyEngineApiFacade.class)
	@ConditionalOnMissingBean
	public TinyEngineMaterialMvcController tinyEngineMaterialMvcController(TinyEngineApiFacade facade) {
		return new TinyEngineMaterialMvcController(facade);
	}

	@Bean
	@ConditionalOnBean(TinyEngineApiFacade.class)
	@ConditionalOnMissingBean
	public TinyEngineModelMvcController tinyEngineModelMvcController(TinyEngineApiFacade facade) {
		return new TinyEngineModelMvcController(facade);
	}

	@Bean
	@ConditionalOnBean(TinyEngineExternalCapability.class)
	@ConditionalOnMissingBean
	public TinyEnginePlatformMvcController tinyEnginePlatformMvcController(
			TinyEngineExternalCapability externalCapability) {
		return new TinyEnginePlatformMvcController(externalCapability);
	}

	@Bean
	@ConditionalOnBean(TinyEngineExternalCapability.class)
	@ConditionalOnMissingBean
	public TinyEngineCodegenMvcController tinyEngineCodegenMvcController(
			TinyEngineExternalCapability externalCapability) {
		return new TinyEngineCodegenMvcController(externalCapability);
	}

	@Bean
	@ConditionalOnBean(DynamicService.class)
	@ConditionalOnMissingBean
	public TinyEngineDynamicModelMvcController tinyEngineDynamicModelMvcController(DynamicService dynamicService) {
		return new TinyEngineDynamicModelMvcController(dynamicService);
	}

	@Bean
	@ConditionalOnBean(TinyEngineUserSystemService.class)
	@ConditionalOnMissingBean
	public TinyEngineUserMvcController tinyEngineUserMvcController(TinyEngineUserSystemService userSystemService,
			TinyEngineCredentialExtractor credentialExtractor) {
		return new TinyEngineUserMvcController(userSystemService, credentialExtractor);
	}
}
