package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.seata.coordinator;

import org.apache.seata.core.model.GlobalStatus;

public interface SeataTransaction {

	boolean isLauncher();

	void begin(int timeoutMs, String name) throws Exception;

	void commit() throws Exception;

	void rollback() throws Exception;

	String getXid();

	GlobalStatus getGlobalStatus();
}
