package com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.datachange;

import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeEvent;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeHandler;
import com.xiongx9527.fw.plus.mybatis.datachange.interceptor.DataChangeInterceptor;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;

class SqlSessionFactoryDataChangeRegistrarTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(MybatisDataChangeAutoConfiguration.class))
			.withPropertyValues("fw.plus.mybatis-datachange.enabled=true",
					"fw.plus.mybatis-datachange.tables[0].name=sys_user");

	@Test
	void registersFactoryCreatedAfterContextInitialization() {
		contextRunner.withUserConfiguration(HandlerConfiguration.class).run(context -> {
			SqlSessionFactory factory = new DefaultSqlSessionFactory(new Configuration());
			SqlSessionFactoryDataChangeRegistrar registrar = context.getBean(SqlSessionFactoryDataChangeRegistrar.class);
			registrar.postProcessAfterInitialization(factory, "lateFactory");
			registrar.postProcessAfterInitialization(factory, "lateFactory");
			assertThat(factory.getConfiguration().getInterceptors())
					.containsExactly(context.getBean(DataChangeInterceptor.class));
		});
	}

	@Test
	void doesNotModifyFactoryWhenInterceptorIsNotConfigured() {
		contextRunner.withUserConfiguration(FactoryConfiguration.class).run(context -> assertThat(
				context.getBean(SqlSessionFactory.class).getConfiguration().getInterceptors()).isEmpty());
	}

	@Test
	void failsStartupWhenDataChangeAlreadyExistsButIsNotLast() {
		contextRunner.withUserConfiguration(HandlerConfiguration.class, WrongOrderFactoryConfiguration.class)
				.run(context -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class);
					Throwable rootCause = context.getStartupFailure();
					while (rootCause.getCause() != null) {
						rootCause = rootCause.getCause();
					}
					assertThat(rootCause).hasMessageContaining(
							"DataChangeInterceptor must be the last MyBatis interceptor")
							.hasMessageContaining("wrongOrderFactory");
				});
	}

	@org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
	static class HandlerConfiguration {
		@Bean DataChangeHandler handler() {
			return new DataChangeHandler() {
				@Override public boolean supports(DataChangeEvent event) { return true; }
				@Override public void handle(DataChangeEvent event) { }
				@Override public int getOrder() { return 0; }
			};
		}
	}

	@org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
	static class FactoryConfiguration {
		@Bean SqlSessionFactory sqlSessionFactory() {
			return new DefaultSqlSessionFactory(new Configuration());
		}
	}

	@org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
	static class WrongOrderFactoryConfiguration {
		@Bean SqlSessionFactory wrongOrderFactory(DataChangeInterceptor dataChangeInterceptor) {
			Configuration configuration = new Configuration();
			configuration.addInterceptor(dataChangeInterceptor);
			configuration.addInterceptor((Interceptor) invocation -> invocation.proceed());
			return new DefaultSqlSessionFactory(configuration);
		}
	}
}
