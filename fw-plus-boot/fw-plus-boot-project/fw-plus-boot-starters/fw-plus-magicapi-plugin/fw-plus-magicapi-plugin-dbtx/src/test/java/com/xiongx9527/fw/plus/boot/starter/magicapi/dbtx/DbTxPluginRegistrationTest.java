package com.xiongx9527.fw.plus.boot.starter.magicapi.dbtx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DbTxPluginRegistrationTest {

	@Test
	void markerMustBeLoadableFromDbTxPlugin() {
		assertEquals("DbTxPluginMarker", DbTxPluginMarker.class.getSimpleName());
	}
}
