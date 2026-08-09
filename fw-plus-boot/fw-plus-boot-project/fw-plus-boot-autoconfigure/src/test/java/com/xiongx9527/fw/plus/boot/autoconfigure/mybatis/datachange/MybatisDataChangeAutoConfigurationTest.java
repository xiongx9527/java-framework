package com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.datachange;

import com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.mf.MfDataChangeAdapterAutoConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.mp.MpDataChangeAdapterAutoConfiguration;
import com.xiongx9527.fw.plus.mf.datachange.MybatisFlexDataChangeAdapter;
import com.xiongx9527.fw.plus.mp.datachange.MybatisPlusDataChangeAdapter;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.DataChangeFrameworkAdapterRegistry;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.MybatisDataChangeFrameworkAdapter;
import com.xiongx9527.fw.plus.mybatis.datachange.api.ConsistencyLevel;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeErrorCode;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeErrorHandler;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeEvent;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeException;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeFailure;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeHandler;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeOperation;
import com.xiongx9527.fw.plus.mybatis.datachange.api.FieldChange;
import com.xiongx9527.fw.plus.mybatis.datachange.api.RowChange;
import com.xiongx9527.fw.plus.mybatis.datachange.api.RowIdentity;
import com.xiongx9527.fw.plus.mybatis.datachange.api.StatementChange;
import com.xiongx9527.fw.plus.mybatis.datachange.api.TableChange;
import com.xiongx9527.fw.plus.mybatis.datachange.api.TableIdentifier;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.AnalyzedStatement;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.BoundSqlContext;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.DataChangeDialect;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.DataChangeDialectRegistry;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.DatabaseMetadata;
import com.xiongx9527.fw.plus.mybatis.datachange.interceptor.DataChangeExecutionContext;
import com.xiongx9527.fw.plus.mybatis.datachange.interceptor.DataChangeExecutionContextHolder;
import com.xiongx9527.fw.plus.mybatis.datachange.interceptor.DataChangeInterceptor;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Statement;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class MybatisDataChangeAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					ConfigurationPropertiesAutoConfiguration.class,
					MybatisDataChangeAutoConfiguration.class,
					MpDataChangeAdapterAutoConfiguration.class,
					MfDataChangeAdapterAutoConfiguration.class));

	@Test
	void registersOneInterceptorIntoEveryFactoryExactlyOnce() {
		configuredRunner().withUserConfiguration(TwoFactoriesConfiguration.class).run(context -> {
			assertThat(context).hasSingleBean(DataChangeInterceptor.class);
			DataChangeInterceptor interceptor = context.getBean(DataChangeInterceptor.class);
			SqlSessionFactory first = context.getBean("firstFactory", SqlSessionFactory.class);
			SqlSessionFactory second = context.getBean("secondFactory", SqlSessionFactory.class);
			assertThat(first.getConfiguration().getInterceptors()).containsExactly(interceptor);
			assertThat(second.getConfiguration().getInterceptors()).containsExactly(interceptor);

			SqlSessionFactoryDataChangeRegistrar registrar = context.getBean(SqlSessionFactoryDataChangeRegistrar.class);
			registrar.postProcessAfterInitialization(first, "firstFactory");
			registrar.postProcessAfterInitialization(second, "secondFactory");
			assertThat(first.getConfiguration().getInterceptors()).containsExactly(interceptor);
			assertThat(second.getConfiguration().getInterceptors()).containsExactly(interceptor);
		});
	}

	@Test
	void disabledEmptyTablesAndMissingHandlerDoNotRegisterInterceptor() {
		contextRunner.withUserConfiguration(HandlerConfiguration.class)
				.withPropertyValues("fw.plus.mybatis-datachange.enabled=false",
						"fw.plus.mybatis-datachange.tables[0].name=sys_user")
				.run(context -> assertThat(context).doesNotHaveBean(DataChangeInterceptor.class));
		contextRunner.withUserConfiguration(HandlerConfiguration.class)
				.withPropertyValues("fw.plus.mybatis-datachange.enabled=true")
				.run(context -> assertThat(context).doesNotHaveBean(DataChangeInterceptor.class));
		contextRunner.withPropertyValues("fw.plus.mybatis-datachange.enabled=true",
					"fw.plus.mybatis-datachange.tables[0].name=sys_user")
				.run(context -> assertThat(context).doesNotHaveBean(DataChangeInterceptor.class));
	}

	@Test
	void legacyPrefixDoesNotEnableInterceptor() {
		contextRunner.withUserConfiguration(HandlerConfiguration.class)
				.withPropertyValues("fw.plus.mp-datachange.enabled=true",
						"fw.plus.mp-datachange.tables[0].name=sys_user")
				.run(context -> assertThat(context).doesNotHaveBean(DataChangeInterceptor.class));
	}

	@Test
	void startsWithNativeMybatisOnly() {
		configuredRunner()
				.withClassLoader(new FilteredClassLoader("com.baomidou.mybatisplus", "com.mybatisflex"))
				.run(context -> {
					assertThat(context).hasSingleBean(MybatisDataChangeFrameworkAdapter.class);
					assertThat(context).hasSingleBean(DataChangeFrameworkAdapterRegistry.class);
					assertThat(context).hasSingleBean(DataChangeInterceptor.class);
				});
	}

	@Test
	void plusAndFlexAdaptersAreConditionalIndependentlyByConcreteType() {
		configuredRunner().run(context -> {
			assertThat(context).hasSingleBean(MybatisPlusDataChangeAdapter.class);
			assertThat(context).hasSingleBean(MybatisFlexDataChangeAdapter.class);
		});

		configuredRunner().withUserConfiguration(CustomPlusAdapterConfiguration.class).run(context -> {
			assertThat(context).hasSingleBean(MybatisPlusDataChangeAdapter.class);
			assertThat(context).hasSingleBean(MybatisFlexDataChangeAdapter.class);
		});
	}

	@Test
	void selectsDialectFromEveryActualConnectionWithoutCrossProductCache() throws Exception {
		contextRunner.run(context -> {
			DataChangeDialectRegistry registry = context.getBean(DataChangeDialectRegistry.class);
			assertThat(registry.select(connection("MySQL", 8, 0)).getClass().getSimpleName())
					.isEqualTo("MySqlDataChangeDialect");
			assertThat(registry.select(connection("PostgreSQL", 15, 4)).getClass().getSimpleName())
					.isEqualTo("PostgreSqlDataChangeDialect");
		});
	}

	@Test
	void dataChangePrepareObservesFinalBoundSqlFromInnerPlugins() throws Exception {
		configuredRunner().withUserConfiguration(RecordingDialectConfiguration.class).run(context -> {
			try {
				Configuration configuration = new Configuration();
				MappedStatement mappedStatement = new MappedStatement.Builder(configuration, "user.rename",
						new StaticSqlSource(configuration, "UPDATE sys_user SET name = 'after' WHERE id = 1"),
						SqlCommandType.UPDATE).build();
				BoundSql boundSql = mappedStatement.getBoundSql(null);
				StatementHandler target = mock(StatementHandler.class);
				Connection connection = connection("H2", 2, 2);
				when(connection.getAutoCommit()).thenReturn(true);
				when(target.getBoundSql()).thenReturn(boundSql);
				when(target.prepare(connection, null)).thenReturn(mock(Statement.class));

				StatementHandler chain = (StatementHandler) new SqlAppendingInterceptor(" AND tenant_id = 7").plugin(target);
				chain = (StatementHandler) new SqlAppendingInterceptor(" AND deleted = 0").plugin(chain);
				chain = (StatementHandler) context.getBean(DataChangeInterceptor.class).plugin(chain);
				DataChangeExecutionContext executionContext = new DataChangeExecutionContext(mappedStatement, null,
						new MybatisDataChangeFrameworkAdapter());
				try (DataChangeExecutionContextHolder.Scope ignored = DataChangeExecutionContextHolder.push(executionContext)) {
					chain.prepare(connection, null);
				}

				assertThat(context.getBean(RecordingDialect.class).getSql())
						.isEqualTo("UPDATE sys_user SET name = 'after' WHERE id = 1 AND deleted = 0 AND tenant_id = 7");
			} catch (Exception exception) {
				throw new AssertionError(exception);
			}
		});
	}

	@Test
	void defaultErrorHandlerDoesNotLogSensitiveEventValues(CapturedOutput output) throws Exception {
		contextRunner.run(context -> {
			try {
				DataChangeErrorHandler errorHandler = context.getBean(DataChangeErrorHandler.class);
				Map<String, Object> identity = new LinkedHashMap<String, Object>();
				identity.put("credential", "token-secret");
				RowChange row = new RowChange(DataChangeOperation.UPDATE, new RowIdentity(identity),
						Collections.singletonList(new FieldChange("password", "password",
								"before-secret", "after-secret")));
				TableChange table = new TableChange(new TableIdentifier(null, "audit", "sys_user"),
						Collections.singletonList(row));
				DataChangeEvent event = new DataChangeEvent(ConsistencyLevel.BEST_EFFORT,
						Collections.singletonList(new StatementChange("user.rename",
								Collections.singletonList(table))));
				errorHandler.handle(new DataChangeFailure(event,
						new DataChangeException(DataChangeErrorCode.HANDLER_FAILED, "handler failed")));
			} catch (Exception exception) {
				throw new AssertionError(exception);
			}
		});

		assertThat(output).contains("eventId=", "statementId=user.rename", "audit.sys_user",
				"BEST_EFFORT", "HANDLER_FAILED", "DataChangeException", "handler failed")
				.doesNotContain("token-secret", "before-secret", "after-secret", "password=", "credential=");
	}

	private ApplicationContextRunner configuredRunner() {
		return contextRunner.withUserConfiguration(HandlerConfiguration.class)
				.withPropertyValues("fw.plus.mybatis-datachange.enabled=true",
						"fw.plus.mybatis-datachange.tables[0].name=sys_user");
	}

	private Connection connection(String productName, int major, int minor) {
		try {
			Connection connection = mock(Connection.class);
			DatabaseMetaData metadata = mock(DatabaseMetaData.class);
			when(connection.getMetaData()).thenReturn(metadata);
			when(metadata.getDatabaseProductName()).thenReturn(productName);
			when(metadata.getDatabaseMajorVersion()).thenReturn(major);
			when(metadata.getDatabaseMinorVersion()).thenReturn(minor);
			return connection;
		} catch (Exception exception) {
			throw new IllegalStateException(exception);
		}
	}

	@org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
	static class HandlerConfiguration {
		@Bean
		DataChangeHandler handler() {
			return new DataChangeHandler() {
				@Override public boolean supports(DataChangeEvent event) { return true; }
				@Override public void handle(DataChangeEvent event) { }
				@Override public int getOrder() { return 0; }
			};
		}
	}

	@org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
	static class TwoFactoriesConfiguration {
		@Bean SqlSessionFactory firstFactory() { return new DefaultSqlSessionFactory(new Configuration()); }
		@Bean SqlSessionFactory secondFactory() { return new DefaultSqlSessionFactory(new Configuration()); }
	}

	@org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
	static class CustomPlusAdapterConfiguration {
		@Bean MybatisPlusDataChangeAdapter mybatisPlusDataChangeAdapter() {
			return new MybatisPlusDataChangeAdapter();
		}
	}

	@org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
	static class RecordingDialectConfiguration {
		@Bean RecordingDialect recordingDialect() { return new RecordingDialect(); }
	}

	@Intercepts(@Signature(type = StatementHandler.class, method = "prepare",
			args = {Connection.class, Integer.class}))
	static class SqlAppendingInterceptor implements Interceptor {
		private final String suffix;

		SqlAppendingInterceptor(String suffix) { this.suffix = suffix; }

		@Override
		public Object intercept(Invocation invocation) throws Throwable {
			BoundSql boundSql = ((StatementHandler) invocation.getTarget()).getBoundSql();
			SystemMetaObject.forObject(boundSql).setValue("sql", boundSql.getSql() + suffix);
			return invocation.proceed();
		}
	}

	static class RecordingDialect implements DataChangeDialect {
		private String sql;

		@Override public boolean supports(DatabaseMetadata metadata) {
			return metadata != null && "h2".equals(metadata.getProductName());
		}
		@Override public TableIdentifier normalize(TableIdentifier table) { return table; }
		@Override public List<AnalyzedStatement> analyze(BoundSqlContext context) {
			this.sql = context.getSql();
			return Collections.emptyList();
		}
		@Override public int getOrder() { return 1000; }
		String getSql() { return sql; }
	}
}
