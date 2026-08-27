package com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.datascope;

import com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.mf.MfDataScopeAdapterAutoConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.mp.MpDataScopeAutoConfiguration;
import com.xiongx9527.fw.plus.mf.datascope.MybatisFlexDataScopeAdapter;
import com.xiongx9527.fw.plus.mp.datascope.MybatisPlusDataScopeAdapter;
import com.xiongx9527.fw.plus.mybatis.datascope.interceptor.RowPermissionInterceptor;
import com.xiongx9527.fw.plus.mybatis.datascope.provider.TableRowPermissionConfigConsumer;
import com.xiongx9527.fw.plus.mybatis.datascope.provider.UserRowPermissionProvider;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;

class MybatisDataScopeAutoConfigurationTest {
	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					ConfigurationPropertiesAutoConfiguration.class,
					MybatisDataScopeAutoConfiguration.class,
					MpDataScopeAutoConfiguration.class,
					MfDataScopeAdapterAutoConfiguration.class));

	@Test
	void registersOneSharedInterceptorIntoEveryFactoryAndIsIdempotent() {
		configuredRunner().run(context -> {
			assertThat(context).hasSingleBean(RowPermissionInterceptor.class);
			assertThat(context).hasSingleBean(MybatisPlusDataScopeAdapter.class);
			assertThat(context).hasSingleBean(MybatisFlexDataScopeAdapter.class);
			RowPermissionInterceptor interceptor = context.getBean(RowPermissionInterceptor.class);
			SqlSessionFactory first = context.getBean("firstFactory", SqlSessionFactory.class);
			SqlSessionFactory second = context.getBean("secondFactory", SqlSessionFactory.class);
			assertThat(first.getConfiguration().getInterceptors()).containsExactly(interceptor);
			assertThat(second.getConfiguration().getInterceptors()).containsExactly(interceptor);

			SqlSessionFactoryDataScopeRegistrar registrar =
					context.getBean(SqlSessionFactoryDataScopeRegistrar.class);
			registrar.postProcessAfterInitialization(first, "firstFactory");
			registrar.postProcessAfterInitialization(second, "secondFactory");
			assertThat(first.getConfiguration().getInterceptors()).containsExactly(interceptor);
			assertThat(second.getConfiguration().getInterceptors()).containsExactly(interceptor);
		});
	}

	@Test
	void doesNotCreateInterceptorWithoutProviderOrWhenDisabled() {
		contextRunner.run(context -> assertThat(context).doesNotHaveBean(RowPermissionInterceptor.class));
		configuredRunner().withPropertyValues("fw.plus.mybatis-datascope.enabled=false")
				.run(context -> assertThat(context).doesNotHaveBean(RowPermissionInterceptor.class));
		configuredRunner().withPropertyValues("fw.data-permission.enabled=false")
				.run(context -> assertThat(context).doesNotHaveBean(RowPermissionInterceptor.class));
	}

	@Test
	void legacyScopeDefaultIsUsedWhenNewPropertyIsAbsent() {
		configuredRunner().withPropertyValues("fw.data-permission.scopeWhenMissingConfig=7")
				.run(context -> assertThat(context.getBean(DataScopeProperties.class)
						.getScopeWhenMissingConfig()).isEqualTo(7));
		configuredRunner().withPropertyValues("fw.data-permission.scopeWhenMissingConfig=7",
				"fw.plus.mybatis-datascope.scope-when-missing-config=9")
				.run(context -> assertThat(context.getBean(DataScopeProperties.class)
						.getScopeWhenMissingConfig()).isEqualTo(9));
	}

	@Test
	void explicitEnabledPropertyOverridesLegacyAlias() {
		configuredRunner().withPropertyValues("fw.data-permission.enabled=false",
				"fw.plus.mybatis-datascope.enabled=true")
				.run(context -> assertThat(context).hasSingleBean(RowPermissionInterceptor.class));
		configuredRunner().withPropertyValues("fw.data-permission.enabled=true",
				"fw.plus.mybatis-datascope.enabled=false")
				.run(context -> assertThat(context).doesNotHaveBean(RowPermissionInterceptor.class));
	}

	private ApplicationContextRunner configuredRunner() {
		return contextRunner.withUserConfiguration(ProviderConfiguration.class);
	}

	@org.springframework.context.annotation.Configuration
	static class ProviderConfiguration {
		@Bean
		UserRowPermissionProvider provider() {
			return new UserRowPermissionProvider() {
				@Override
				public boolean isRowPermissionEnabled() {
					return true;
				}

				@Override
				public void loadTableRowPermissionConfig(TableRowPermissionConfigConsumer consumer) {
					// no table configuration is required for the bean wiring test
				}
			};
		}

		@Bean
		SqlSessionFactory firstFactory() {
			return new DefaultSqlSessionFactory(new Configuration());
		}

		@Bean
		SqlSessionFactory secondFactory() {
			return new DefaultSqlSessionFactory(new Configuration());
		}
	}
}
