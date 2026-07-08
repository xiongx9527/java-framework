package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.model.MagicDynamicRedisDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.module.RedisModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.service.RedisDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.service.RedisDataSourceMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.starter.MagicAPIRedisConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MagicAPIRedisConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(MagicAPIRedisConfiguration.class))
			.withBean(MagicAPIProperties.class, MagicAPIProperties::new);

	@Test
	void backsOffWhenPluginDisabled() {
		contextRunner
				.withPropertyValues("magic-api.plugin.redis.enabled=false")
				.run(context -> assertThat(context)
						.doesNotHaveBean(MagicDynamicRedisDataSource.class)
						.doesNotHaveBean(RedisModule.class));
	}

	@Test
	void createsInfrastructureWithoutDefaultRedisConnectionFactory() {
		contextRunner
				.withPropertyValues("magic-api.plugin.redis.enabled=true")
				.run(context -> assertThat(context)
						.hasSingleBean(MagicDynamicRedisDataSource.class)
						.hasSingleBean(RedisDataSourceInfoMagicResourceStorage.class)
						.hasSingleBean(RedisDataSourceMagicDynamicRegistry.class)
						.doesNotHaveBean(RedisModule.class));
	}

	@Test
	void createsRedisModuleWhenDefaultRedisConnectionFactoryExists() {
		contextRunner
				.withPropertyValues("magic-api.plugin.redis.enabled=true")
				.withBean(RedisConnectionFactory.class, () -> new LettuceConnectionFactory("127.0.0.1", 6379))
				.run(context -> assertThat(context)
						.hasSingleBean(MagicDynamicRedisDataSource.class)
						.hasSingleBean(RedisModule.class)
						.hasSingleBean(RedisDataSourceInfoMagicResourceStorage.class)
						.hasSingleBean(RedisDataSourceMagicDynamicRegistry.class));
	}

	@Test
	void backsOffForUserBeans() {
		contextRunner
				.withPropertyValues("magic-api.plugin.redis.enabled=true")
				.withUserConfiguration(UserRedisConfiguration.class)
				.run(context -> {
					assertThat(context).hasSingleBean(MagicDynamicRedisDataSource.class);
					assertThat(context).hasSingleBean(RedisModule.class);
					assertThat(context).hasSingleBean(RedisDataSourceInfoMagicResourceStorage.class);
					assertThat(context).hasSingleBean(RedisDataSourceMagicDynamicRegistry.class);
					assertThat(context.getBean(MagicDynamicRedisDataSource.class))
							.isSameAs(context.getBean("customDynamicRedisDataSource"));
				});
	}

	@Test
	void exposesPluginMetadata() {
		contextRunner
				.run(context -> {
					MagicPluginConfiguration configuration = context.getBean(MagicAPIRedisConfiguration.class);
					assertThat(configuration.plugin().getName()).isEqualTo("redis");
					assertThat(configuration.plugin().getGlobalName()).isEqualTo("MagicRedis");
					assertThat(configuration.plugin().getJavascriptFilename()).isEqualTo("magic-redis.1.0.0.iife.js");
				});
	}

	@Configuration(proxyBeanMethods = false)
	static class UserRedisConfiguration {

		@Bean
		MagicDynamicRedisDataSource customDynamicRedisDataSource() {
			return new MagicDynamicRedisDataSource();
		}

		@Bean
		RedisConnectionFactory customRedisConnectionFactory() {
			return new LettuceConnectionFactory("127.0.0.1", 6379);
		}

		@Bean
		RedisModule customRedisModule(MagicDynamicRedisDataSource dataSource, RedisConnectionFactory connectionFactory) {
			return new RedisModule(dataSource, connectionFactory);
		}

		@Bean
		RedisDataSourceInfoMagicResourceStorage customStorage() {
			return new RedisDataSourceInfoMagicResourceStorage();
		}

		@Bean
		RedisDataSourceMagicDynamicRegistry customRegistry() {
			return mock(RedisDataSourceMagicDynamicRegistry.class);
		}
	}
}
