package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine;

import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineAuthenticationProvider;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineCredential;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineLoginRequest;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineLoginResult;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEnginePermissionEvaluator;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEnginePrincipal;
import com.xiongx9527.fw.plus.tinyengine.support.auth.SystemTinyEngineUserSystemService;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineUserSystemService;
import com.xiongx9527.fw.plus.tinyengine.support.context.SystemTinyEngineUserContext;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineRequestUserScope;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class TinyEngineSecurityAutoConfigurationTest {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(TinyEngineAutoConfiguration.class))
			.withPropertyValues("fw.plus.tiny-engine.enabled=true");

	@Test
	void securityEnabledRequiresAuthenticationAndPermissionProviders() {
		contextRunner.withPropertyValues("fw.plus.tiny-engine.security.enabled=true")
				.run(context -> assertThat(context).hasFailed());
	}

	@Test
	void securityEnabledCreatesProviderBackedUserSystem() {
		contextRunner.withUserConfiguration(SecurityProviderConfiguration.class)
				.withPropertyValues("fw.plus.tiny-engine.security.enabled=true")
				.run(context -> {
					assertThat(context).hasSingleBean(TinyEngineUserSystemService.class);
					assertThat(context).hasSingleBean(TinyEngineRequestUserScope.class);
					assertThat(context).hasSingleBean(TinyEngineUserContext.class);
					assertThat(context).doesNotHaveBean(SystemTinyEngineUserContext.class);
				});
	}

	@Test
	void securityDisabledKeepsSystemUserFallback() {
		contextRunner.run(context -> {
			assertThat(context).hasSingleBean(SystemTinyEngineUserContext.class);
			assertThat(context).hasSingleBean(TinyEngineUserContext.class);
			assertThat(context).hasSingleBean(TinyEngineUserSystemService.class);
			assertThat(context.getBean(TinyEngineUserSystemService.class))
					.isInstanceOf(SystemTinyEngineUserSystemService.class);
		});
	}

	@Configuration
	static class SecurityProviderConfiguration {

		@Bean
		TinyEngineAuthenticationProvider tinyEngineAuthenticationProvider() {
			return new TinyEngineAuthenticationProvider() {
				@Override
				public TinyEngineLoginResult login(TinyEngineLoginRequest request) {
					return new TinyEngineLoginResult("token", "Bearer", Long.valueOf(3600L));
				}

				@Override
				public TinyEnginePrincipal resolve(TinyEngineCredential credential) {
					return new TinyEnginePrincipal("u1", "User One", credential.getTenantId(), "r1",
							Integer.valueOf(1));
				}

				@Override
				public void logout(TinyEngineCredential credential) {
				}
			};
		}

		@Bean
		TinyEnginePermissionEvaluator tinyEnginePermissionEvaluator() {
			return (principal, permission) -> {
			};
		}
	}
}
