package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.seata.coordinator;

@FunctionalInterface
public interface SeataTransactionFactory {

	SeataTransaction getCurrentOrCreate();
}
