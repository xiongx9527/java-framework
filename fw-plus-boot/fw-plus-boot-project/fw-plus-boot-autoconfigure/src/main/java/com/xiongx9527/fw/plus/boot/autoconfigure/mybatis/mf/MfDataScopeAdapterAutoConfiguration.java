package com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.mf;

import com.xiongx9527.fw.plus.mf.datascope.MybatisFlexDataScopeAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(MybatisFlexDataScopeAdapter.class)
public class MfDataScopeAdapterAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(MybatisFlexDataScopeAdapter.class)
	public MybatisFlexDataScopeAdapter mybatisFlexDataScopeAdapter() {
		return new MybatisFlexDataScopeAdapter();
	}
}
