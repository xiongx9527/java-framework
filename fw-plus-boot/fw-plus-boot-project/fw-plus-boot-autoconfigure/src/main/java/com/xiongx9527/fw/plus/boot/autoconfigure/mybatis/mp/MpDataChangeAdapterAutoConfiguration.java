package com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.mp;

import com.xiongx9527.fw.plus.mp.datachange.MybatisPlusDataChangeAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(MybatisPlusDataChangeAdapter.class)
public class MpDataChangeAdapterAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(MybatisPlusDataChangeAdapter.class)
	public MybatisPlusDataChangeAdapter mybatisPlusDataChangeAdapter() {
		return new MybatisPlusDataChangeAdapter();
	}
}
