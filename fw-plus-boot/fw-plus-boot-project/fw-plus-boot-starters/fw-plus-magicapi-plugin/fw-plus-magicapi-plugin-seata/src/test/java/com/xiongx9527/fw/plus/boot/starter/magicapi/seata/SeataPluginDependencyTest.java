package com.xiongx9527.fw.plus.boot.starter.magicapi.seata;

import com.xiongx9527.fw.plus.boot.starter.magicapi.dbtx.DbTxPluginMarker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SeataPluginDependencyTest {

	@Test
	void seataPluginMustExposeBothMarkers() {
		assertNotNull(SeataPluginMarker.class);
		assertNotNull(DbTxPluginMarker.class);
		assertNotNull(io.seata.tm.api.GlobalTransactionContext.class);
	}
}
