package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.service.ManagedMagicDynamicDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;

import javax.sql.DataSource;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "fw.plus.magicapi", name = "enabled",
		havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = "org.ssssssss.magicapi.spring.boot.starter.MagicAPIAutoConfiguration")
@AutoConfigureBefore(name = "org.ssssssss.magicapi.spring.boot.starter.MagicAPIAutoConfiguration")
public class MagicAPIDynamicDataSourceAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(MagicDynamicDataSource.class)
	public MagicDynamicDataSource magicDynamicDataSource(
			@Autowired(required = false) DataSource dataSource) {
		ManagedMagicDynamicDataSource dynamicDataSource = new ManagedMagicDynamicDataSource();
		if (dataSource != null) {
			dynamicDataSource.put(dataSource);
		}
		return dynamicDataSource;
	}
}
