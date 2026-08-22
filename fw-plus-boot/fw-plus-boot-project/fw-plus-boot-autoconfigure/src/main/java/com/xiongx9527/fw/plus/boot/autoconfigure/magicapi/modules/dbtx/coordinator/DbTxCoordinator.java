package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.coordinator;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.model.DbTxDefinition;

public interface DbTxCoordinator {

	String getName();

	Object execute(DbTxDefinition definition, DbTxCallback callback) throws Throwable;
}
