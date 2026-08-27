package com.xiongx9527.fw.plus.boot.starter.magicapi;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.DbTxModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.coordinator.LocalDbTxCoordinator;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class MagicApiDbTxBuiltinClasspathTest {

	@Test
	void mainMagicApiStarterMustExposeBuiltInLocalDbTx() {
		assertNotNull(DbTxModule.class);
		assertNotNull(LocalDbTxCoordinator.class);
		assertNotNull(DataSourceTransactionManager.class);
	}
}
