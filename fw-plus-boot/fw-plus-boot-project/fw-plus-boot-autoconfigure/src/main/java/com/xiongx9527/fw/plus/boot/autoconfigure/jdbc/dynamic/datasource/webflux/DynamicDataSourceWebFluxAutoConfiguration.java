package com.xiongx9527.fw.plus.boot.autoconfigure.jdbc.dynamic.datasource.webflux;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Hooks;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({WebFilter.class, Hooks.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class DynamicDataSourceWebFluxAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public DsWebFluxContextHook dsWebFluxContextHook() {
		return new DsWebFluxContextHook();
	}

	@Bean
	@ConditionalOnMissingBean
	public DsWebFluxRequestContextFilter dsWebFluxRequestContextFilter() {
		return new DsWebFluxRequestContextFilter();
	}

}
