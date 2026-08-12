package com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.mp;

import com.xiongx9527.fw.plus.mp.datascope.MybatisPlusDataScopeAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(MybatisPlusDataScopeAdapter.class)
@ConditionalOnProperty(prefix = "fw.plus.mp-datascope", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MpDataScopeAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(MybatisPlusDataScopeAdapter.class)
	public MybatisPlusDataScopeAdapter mybatisPlusDataScopeAdapter() {
		return new MybatisPlusDataScopeAdapter();
	}
}
