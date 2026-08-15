package com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.datascope;

import com.xiongx9527.fw.plus.mybatis.datascope.interceptor.RowPermissionInterceptor;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.List;

public final class SqlSessionFactoryDataScopeRegistrar implements BeanPostProcessor {
	private final RowPermissionInterceptor interceptor;

	public SqlSessionFactoryDataScopeRegistrar(RowPermissionInterceptor interceptor) {
		this.interceptor = interceptor;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof SqlSessionFactory) {
			register((SqlSessionFactory) bean);
		}
		return bean;
	}

	private void register(SqlSessionFactory factory) {
		Configuration configuration = factory.getConfiguration();
		synchronized (configuration) {
			List<Interceptor> interceptors = configuration.getInterceptors();
			for (Interceptor existing : interceptors) {
				if (existing instanceof RowPermissionInterceptor) {
					return;
				}
			}
			configuration.addInterceptor(interceptor);
		}
	}
}
