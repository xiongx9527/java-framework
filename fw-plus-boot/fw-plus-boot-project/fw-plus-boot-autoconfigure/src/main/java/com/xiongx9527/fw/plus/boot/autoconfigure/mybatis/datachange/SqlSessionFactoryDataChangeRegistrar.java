package com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.datachange;

import com.xiongx9527.fw.plus.mybatis.datachange.interceptor.DataChangeInterceptor;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.ArrayList;
import java.util.List;

public final class SqlSessionFactoryDataChangeRegistrar implements BeanPostProcessor {
	private final ObjectProvider<DataChangeInterceptor> interceptorProvider;

	public SqlSessionFactoryDataChangeRegistrar(ObjectProvider<DataChangeInterceptor> interceptorProvider) {
		this.interceptorProvider = interceptorProvider;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) {
		if (!(bean instanceof SqlSessionFactory)) {
			return bean;
		}
		DataChangeInterceptor interceptor = interceptorProvider.getIfAvailable();
		if (interceptor == null) {
			return bean;
		}
		register((SqlSessionFactory) bean, beanName, interceptor);
		return bean;
	}

	private void register(SqlSessionFactory factory, String beanName, DataChangeInterceptor interceptor) {
		Configuration configuration = factory.getConfiguration();
		synchronized (configuration) {
			List<Interceptor> interceptors = configuration.getInterceptors();
			int sameInstance = identityIndex(interceptors, interceptor);
			if (sameInstance >= 0) {
				if (sameInstance != interceptors.size() - 1) {
					throw wrongOrder(beanName, interceptors);
				}
				return;
			}
			for (Interceptor existing : interceptors) {
				if (existing instanceof DataChangeInterceptor) {
					throw new IllegalStateException("SqlSessionFactory '" + beanName
							+ "' already contains a different DataChangeInterceptor: " + describe(interceptors));
				}
			}
			configuration.addInterceptor(interceptor);
		}
	}

	private int identityIndex(List<Interceptor> interceptors, DataChangeInterceptor expected) {
		for (int index = 0; index < interceptors.size(); index++) {
			if (interceptors.get(index) == expected) {
				return index;
			}
		}
		return -1;
	}

	private IllegalStateException wrongOrder(String beanName, List<Interceptor> interceptors) {
		return new IllegalStateException("DataChangeInterceptor must be the last MyBatis interceptor in "
				+ "SqlSessionFactory '" + beanName + "': " + describe(interceptors));
	}

	private String describe(List<Interceptor> interceptors) {
		List<String> names = new ArrayList<String>();
		for (Interceptor interceptor : interceptors) {
			names.add(interceptor == null ? "null" : interceptor.getClass().getName());
		}
		return names.toString();
	}
}
