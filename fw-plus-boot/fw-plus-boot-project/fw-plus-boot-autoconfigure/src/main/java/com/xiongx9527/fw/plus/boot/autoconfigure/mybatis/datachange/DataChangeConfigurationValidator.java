package com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.datachange;

import com.xiongx9527.fw.plus.mybatis.datachange.config.DataChangeConfiguration;
import org.springframework.beans.factory.InitializingBean;

public final class DataChangeConfigurationValidator implements InitializingBean {
	private final DataChangeConfiguration configuration;

	public DataChangeConfigurationValidator(DataChangeConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public void afterPropertiesSet() {
		configuration.validate();
	}
}
