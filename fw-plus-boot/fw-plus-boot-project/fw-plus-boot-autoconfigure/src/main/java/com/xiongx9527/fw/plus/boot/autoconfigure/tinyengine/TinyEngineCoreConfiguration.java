package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine;

import com.xiongx9527.fw.plus.tinyengine.support.auth.DefaultTinyEngineUserSystemService;
import com.xiongx9527.fw.plus.tinyengine.support.auth.SystemTinyEngineUserSystemService;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineAccountProvider;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineAuthenticationProvider;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEnginePermissionEvaluator;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineTenantProvider;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineUserSystemService;
import com.xiongx9527.fw.plus.tinyengine.support.context.SystemTinyEngineUserContext;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineRequestUserScope;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import com.xiongx9527.fw.plus.tinyengine.support.hook.NoopTinyEngineSaveHook;
import com.xiongx9527.fw.plus.tinyengine.support.hook.TinyEngineSaveHook;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(TinyEngineUserContext.class)
public class TinyEngineCoreConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "fw.plus.tiny-engine.security", name = "enabled", havingValue = "false",
			matchIfMissing = true)
	public TinyEngineUserContext tinyEngineUserContext() {
		return new SystemTinyEngineUserContext();
	}

	@Bean
	@ConditionalOnMissingBean(TinyEngineUserContext.class)
	@ConditionalOnProperty(prefix = "fw.plus.tiny-engine.security", name = "enabled", havingValue = "true")
	public TinyEngineRequestUserScope tinyEngineRequestUserScope() {
		return new TinyEngineRequestUserScope();
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "fw.plus.tiny-engine.security", name = "enabled", havingValue = "false",
			matchIfMissing = true)
	public TinyEngineUserSystemService systemTinyEngineUserSystemService(TinyEngineUserContext userContext) {
		return new SystemTinyEngineUserSystemService(userContext);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "fw.plus.tiny-engine.security", name = "enabled", havingValue = "true")
	public TinyEngineUserSystemService tinyEngineUserSystemService(
			TinyEngineAuthenticationProvider authenticationProvider,
			ObjectProvider<TinyEngineAccountProvider> accountProvider,
			ObjectProvider<TinyEngineTenantProvider> tenantProvider,
			TinyEnginePermissionEvaluator permissionEvaluator) {
		return new DefaultTinyEngineUserSystemService(authenticationProvider, accountProvider.getIfAvailable(),
				tenantProvider.getIfAvailable(), permissionEvaluator);
	}

	@Bean
	@ConditionalOnMissingBean
	public TinyEngineSaveHook tinyEngineSaveHook() {
		return new NoopTinyEngineSaveHook();
	}
}
