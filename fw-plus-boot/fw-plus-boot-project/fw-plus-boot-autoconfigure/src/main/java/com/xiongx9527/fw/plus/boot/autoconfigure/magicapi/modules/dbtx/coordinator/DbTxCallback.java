package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.coordinator;

@FunctionalInterface
public interface DbTxCallback {

	Object execute() throws Throwable;
}
