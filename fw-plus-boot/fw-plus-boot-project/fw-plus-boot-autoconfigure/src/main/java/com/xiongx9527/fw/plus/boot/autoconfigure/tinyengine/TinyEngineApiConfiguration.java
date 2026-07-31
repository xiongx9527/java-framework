package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.DefaultTinyEngineApiFacade;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.DefaultTinyEngineExternalCapability;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineApiFacade;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineExternalCapability;
import com.xiongx9527.fw.plus.tinyengine.service.AppService;
import com.xiongx9527.fw.plus.tinyengine.service.PageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TinyEngineApiConfiguration {

	@Bean
	@ConditionalOnBean({AppService.class, PageService.class})
	@ConditionalOnMissingBean
	public TinyEngineApiFacade tinyEngineApiFacade(AppService appService, PageService pageService) {
		return new DefaultTinyEngineApiFacade(appService, pageService);
	}

	@Bean
	@ConditionalOnMissingBean
	public TinyEngineExternalCapability tinyEngineExternalCapability() {
		return new DefaultTinyEngineExternalCapability();
	}
}
