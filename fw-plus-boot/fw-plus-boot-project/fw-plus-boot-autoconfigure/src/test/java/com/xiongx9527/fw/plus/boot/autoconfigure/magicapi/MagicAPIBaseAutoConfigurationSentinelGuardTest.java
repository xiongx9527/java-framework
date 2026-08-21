package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.key.MagicApiRequestKeyResolver;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.sentinel.MagicApiSentinelGuardRequestInterceptor;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.sentinel.MagicApiSentinelResourceResolver;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.sentinel.MagicApiSentinelRuleManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.ssssssss.magicapi.core.interceptor.ResultProvider;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MagicAPIBaseAutoConfigurationSentinelGuardTest {

	@Test
	void sentinelGuardInfrastructureBeanMethodsAreRegistered() throws Exception {
		assertBeanMethod("magicApiRequestKeyResolver");
		assertBeanMethod("magicApiSentinelResourceResolver");
		assertBeanMethod("magicApiSentinelRuleManager");
		assertBeanMethod("magicApiSentinelGuardRequestInterceptor",
				ResultProvider.class,
				MagicApiRequestKeyResolver.class,
				MagicApiSentinelResourceResolver.class,
				MagicApiSentinelRuleManager.class);
	}

	@Test
	void createsSentinelGuardInfrastructure() {
		MagicAPIWebMvcAutoConfiguration configuration = configuration();
		MagicApiRequestKeyResolver keyResolver = configuration.magicApiRequestKeyResolver();
		MagicApiSentinelResourceResolver resourceResolver = configuration.magicApiSentinelResourceResolver();
		MagicApiSentinelRuleManager ruleManager = configuration.magicApiSentinelRuleManager();
		MagicApiSentinelGuardRequestInterceptor interceptor = configuration.magicApiSentinelGuardRequestInterceptor(
				mock(ResultProvider.class), keyResolver, resourceResolver, ruleManager);

		assertThat(keyResolver).isNotNull();
		assertThat(resourceResolver).isNotNull();
		assertThat(ruleManager).isNotNull();
		assertThat(interceptor).isNotNull();
	}

	private void assertBeanMethod(String name, Class<?>... parameterTypes) throws Exception {
		Method method = MagicAPIWebMvcAutoConfiguration.class.getMethod(name, parameterTypes);

		assertThat(method.getAnnotation(Bean.class)).isNotNull();
	}

	@SuppressWarnings("unchecked")
	private MagicAPIWebMvcAutoConfiguration configuration() {
		return new MagicAPIWebMvcAutoConfiguration(
				mock(ObjectProvider.class),
				mock(ApplicationContext.class));
	}
}
