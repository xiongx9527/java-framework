package com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.mp;

import com.xiongx9527.fw.plus.common.encrypt.DefaultEncryptor;
import com.xiongx9527.fw.plus.mybatis.encrypt.interceptor.FieldDecryptInterceptor;
import com.xiongx9527.fw.plus.mybatis.encrypt.interceptor.FieldEncryptInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.MybatisAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MpAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(MpAutoConfiguration.class))
			.withUserConfiguration(DataSourceConfiguration.class)
			.withClassLoader(new FilteredClassLoader(
					"com.github.yulichang.autoconfigure.conditional.JoinSqlInjectorCondition"));

	@Test
	void rootAutoConfigurationDoesNotRequireJoinConditionClass() {
		contextRunner.run(context -> assertThat(context).hasSingleBean(MpAutoConfiguration.class));
	}

	@Test
	void rootAutoConfigurationBacksOffJoinWhenJoinDependencyIsMissing() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(MpAutoConfiguration.class))
				.withUserConfiguration(DataSourceConfiguration.class)
				.withClassLoader(new FilteredClassLoader(
						"com.github.yulichang.autoconfigure.MybatisPlusJoinProperties"))
				.run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context).hasSingleBean(MpAutoConfiguration.class);
				});
	}

	@Test
	void encryptAutoConfigurationBacksOffWhenDisabled() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(MpEncryptAutoConfiguration.class))
				.withPropertyValues("fw.plus.mp-encrypt.enabled=false")
				.run(context -> assertThat(context)
						.doesNotHaveBean(DefaultEncryptor.class)
						.doesNotHaveBean(FieldEncryptInterceptor.class)
						.doesNotHaveBean(FieldDecryptInterceptor.class));
	}

	@Test
	void aggregateRegistersEncryptionEvenWhenOtherMpEnhancementsAreDisabled() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(MybatisAutoConfiguration.class))
				.withUserConfiguration(DataSourceConfiguration.class)
				.withPropertyValues("fw.plus.mp.enabled=false")
				.run(context -> assertThat(context)
						.hasSingleBean(FieldEncryptInterceptor.class)
						.hasSingleBean(FieldDecryptInterceptor.class));
	}

	@Configuration(proxyBeanMethods = false)
	static class DataSourceConfiguration {

		@Bean
		DataSource dataSource() {
			return mock(DataSource.class);
		}
	}
}
