package com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.datachange;

import com.xiongx9527.fw.plus.mybatis.datachange.adapter.DataChangeFrameworkAdapter;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.DataChangeFrameworkAdapterRegistry;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.MybatisDataChangeFrameworkAdapter;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeErrorHandler;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeHandler;
import com.xiongx9527.fw.plus.mybatis.datachange.config.DataChangeConfiguration;
import com.xiongx9527.fw.plus.mybatis.datachange.config.TableMonitorRegistry;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.DataChangeDialect;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.DataChangeDialectRegistry;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.StandardDialectSet;
import com.xiongx9527.fw.plus.mybatis.datachange.diff.ChangeDiffEngine;
import com.xiongx9527.fw.plus.mybatis.datachange.dispatch.DataChangeDispatcher;
import com.xiongx9527.fw.plus.mybatis.datachange.interceptor.DataChangeEngine;
import com.xiongx9527.fw.plus.mybatis.datachange.interceptor.DataChangeInterceptor;
import com.xiongx9527.fw.plus.mybatis.datachange.snapshot.JdbcTableMetadataResolver;
import com.xiongx9527.fw.plus.mybatis.datachange.snapshot.SnapshotService;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({SqlSessionFactory.class, DataChangeInterceptor.class})
@AutoConfigureAfter(name = {
		"org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration",
		"com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration",
		"com.mybatisflex.spring.boot.MybatisFlexAutoConfiguration"
})
public class MybatisDataChangeAutoConfiguration {

	@Bean
	@ConfigurationProperties(prefix = "fw.plus.mybatis-datachange")
	@ConditionalOnMissingBean
	public DataChangeConfiguration mybatisDataChangeConfiguration() {
		return new DataChangeConfiguration();
	}

	@Bean
	public DataChangeConfigurationValidator dataChangeConfigurationValidator(
			DataChangeConfiguration configuration) {
		return new DataChangeConfigurationValidator(configuration);
	}

	@Bean
	@ConditionalOnMissingBean(MybatisDataChangeFrameworkAdapter.class)
	public MybatisDataChangeFrameworkAdapter mybatisDataChangeFrameworkAdapter() {
		return new MybatisDataChangeFrameworkAdapter();
	}

	@Bean
	@ConditionalOnMissingBean
	public DataChangeFrameworkAdapterRegistry dataChangeFrameworkAdapterRegistry(
			ObjectProvider<DataChangeFrameworkAdapter> adapters) {
		return new DataChangeFrameworkAdapterRegistry(adapters.orderedStream().collect(Collectors.toList()));
	}

	@Bean
	@ConditionalOnMissingBean
	public DataChangeDialectRegistry dataChangeDialectRegistry(ObjectProvider<DataChangeDialect> customDialects) {
		List<DataChangeDialect> dialects = new ArrayList<DataChangeDialect>(StandardDialectSet.create());
		dialects.addAll(customDialects.orderedStream().collect(Collectors.toList()));
		return new DataChangeDialectRegistry(dialects);
	}

	@Bean
	@ConditionalOnMissingBean
	public TableMonitorRegistry tableMonitorRegistry(DataChangeConfiguration configuration) {
		return new TableMonitorRegistry(configuration.getTables());
	}

	@Bean
	@ConditionalOnMissingBean
	public JdbcTableMetadataResolver jdbcTableMetadataResolver() {
		return new JdbcTableMetadataResolver();
	}

	@Bean
	@ConditionalOnMissingBean
	public SnapshotService dataChangeSnapshotService() {
		return new SnapshotService();
	}

	@Bean
	@ConditionalOnMissingBean
	public ChangeDiffEngine dataChangeDiffEngine() {
		return new ChangeDiffEngine();
	}

	@Bean
	@ConditionalOnMissingBean(DataChangeErrorHandler.class)
	public DataChangeErrorHandler dataChangeErrorHandler() {
		return new DefaultDataChangeErrorHandler();
	}

	@Bean
	@ConditionalOnMissingBean
	public DataChangeDispatcher dataChangeDispatcher(ObjectProvider<DataChangeHandler> handlers,
			DataChangeErrorHandler errorHandler) {
		return new DataChangeDispatcher(handlers.orderedStream().collect(Collectors.toList()), errorHandler);
	}

	@Bean
	@ConditionalOnMissingBean
	public DataChangeEngine dataChangeEngine(DataChangeConfiguration configuration,
			TableMonitorRegistry monitors, DataChangeFrameworkAdapterRegistry adapters,
			DataChangeDialectRegistry dialects, JdbcTableMetadataResolver metadataResolver,
			SnapshotService snapshots, ChangeDiffEngine diffEngine, DataChangeDispatcher dispatcher,
			DataChangeErrorHandler errorHandler) {
		return new DataChangeEngine(configuration, monitors, adapters, dialects, metadataResolver,
				snapshots, diffEngine, dispatcher, errorHandler);
	}

	@Bean
	@Order(Ordered.LOWEST_PRECEDENCE)
	@ConditionalOnMissingBean
	@ConditionalOnBean(DataChangeHandler.class)
	@ConditionalOnProperty(prefix = "fw.plus.mybatis-datachange", name = "enabled", havingValue = "true")
	public DataChangeInterceptor dataChangeInterceptor(DataChangeConfiguration configuration,
			DataChangeEngine engine) {
		return configuration.getTables().isEmpty() ? null : new DataChangeInterceptor(configuration, engine);
	}

	@Bean
	public static SqlSessionFactoryDataChangeRegistrar sqlSessionFactoryDataChangeRegistrar(
			ObjectProvider<DataChangeInterceptor> interceptorProvider) {
		return new SqlSessionFactoryDataChangeRegistrar(interceptorProvider);
	}
}
