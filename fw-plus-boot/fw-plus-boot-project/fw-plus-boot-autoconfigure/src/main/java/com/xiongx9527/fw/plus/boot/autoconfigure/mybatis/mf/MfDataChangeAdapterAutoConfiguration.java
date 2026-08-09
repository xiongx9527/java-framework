package com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.mf;

import com.xiongx9527.fw.plus.mf.datachange.MybatisFlexDataChangeAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(MybatisFlexDataChangeAdapter.class)
public class MfDataChangeAdapterAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(MybatisFlexDataChangeAdapter.class)
	public MybatisFlexDataChangeAdapter mybatisFlexDataChangeAdapter() {
		return new MybatisFlexDataChangeAdapter();
	}
}
