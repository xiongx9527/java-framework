package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.seata.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.seata.coordinator.DefaultSeataTransactionFactory;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.seata.coordinator.SeataDbTxCoordinator;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.seata.coordinator.SeataTransactionFactory;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.seata.datasource.SeataDataSourceAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "io.seata.tm.api.GlobalTransactionContext")
@ConditionalOnProperty(prefix = "magic-api.dbtx", name = "seata-enabled", havingValue = "true")
public class MagicAPISeataConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public SeataTransactionFactory seataTransactionFactory() {
		return new DefaultSeataTransactionFactory();
	}

	@Bean
	@ConditionalOnMissingBean
	public SeataDbTxCoordinator seataDbTxCoordinator(MagicDynamicDataSource dynamicDataSource,
			SeataTransactionFactory transactionFactory) {
		return new SeataDbTxCoordinator(dynamicDataSource, transactionFactory);
	}

	@Bean
	@ConditionalOnMissingBean
	public SeataDataSourceAdapter seataDataSourceAdapter() {
		return new SeataDataSourceAdapter();
	}
}
