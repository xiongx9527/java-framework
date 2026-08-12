package com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.datascope;

import com.xiongx9527.fw.plus.mybatis.datascope.adapter.DataScopeFrameworkAdapter;
import com.xiongx9527.fw.plus.mybatis.datascope.adapter.DataScopeFrameworkAdapterRegistry;
import com.xiongx9527.fw.plus.mybatis.datascope.interceptor.RowPermissionInterceptor;
import com.xiongx9527.fw.plus.mybatis.datascope.provider.UserRowPermissionProvider;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.List;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({SqlSessionFactory.class, RowPermissionInterceptor.class})
@ConditionalOnBean(UserRowPermissionProvider.class)
@ConditionalOnExpression("${fw.plus.mybatis-datascope.enabled:${fw.data-permission.enabled:true}}")
public class MybatisDataScopeAutoConfiguration {

	@Bean
	@ConfigurationProperties(prefix = "fw.plus.mybatis-datascope")
	@ConditionalOnMissingBean
	public DataScopeProperties dataScopeProperties(Environment environment) {
		DataScopeProperties properties = new DataScopeProperties();
		String enabled = environment.getProperty("fw.plus.mybatis-datascope.enabled");
		if (enabled == null) {
			enabled = environment.getProperty("fw.data-permission.enabled");
		}
		if (enabled != null) {
			properties.setEnabled(Boolean.parseBoolean(enabled));
		}

		String scopeWhenMissingConfig = environment.getProperty(
				"fw.plus.mybatis-datascope.scope-when-missing-config");
		if (scopeWhenMissingConfig == null) {
			scopeWhenMissingConfig = environment.getProperty(
					"fw.data-permission.scopeWhenMissingConfig");
		}
		if (scopeWhenMissingConfig != null) {
			properties.setScopeWhenMissingConfig(Integer.valueOf(scopeWhenMissingConfig));
		}
		return properties;
	}

	@Bean
	@ConditionalOnMissingBean
	public DataScopeFrameworkAdapterRegistry dataScopeFrameworkAdapterRegistry(
			List<DataScopeFrameworkAdapter> adapters) {
		return new DataScopeFrameworkAdapterRegistry(adapters);
	}

	@Bean
	@ConditionalOnMissingBean
	public RowPermissionInterceptor rowPermissionInterceptor(
			UserRowPermissionProvider provider,
			DataScopeProperties properties,
			DataScopeFrameworkAdapterRegistry adapterRegistry) {
		return new RowPermissionInterceptor(provider, properties.getScopeWhenMissingConfig(), adapterRegistry);
	}

	@Bean
	@ConditionalOnMissingBean
	public SqlSessionFactoryDataScopeRegistrar sqlSessionFactoryDataScopeRegistrar(
			RowPermissionInterceptor interceptor) {
		return new SqlSessionFactoryDataScopeRegistrar(interceptor);
	}
}
