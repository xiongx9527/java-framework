package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.seata.coordinator;

@FunctionalInterface
public interface SeataTransactionFactory {

	SeataTransaction getCurrentOrCreate();
}
