package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineResourceController;
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
	@ConditionalOnBean(ResourceService.class)
	@ConditionalOnMissingBean
	public TinyEngineResourceController tinyEngineResourceController(ResourceService resourceService) {
		return new TinyEngineResourceController(resourceService);
	}
}
