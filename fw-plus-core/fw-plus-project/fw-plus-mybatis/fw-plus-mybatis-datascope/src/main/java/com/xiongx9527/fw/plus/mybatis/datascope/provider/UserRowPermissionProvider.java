package com.xiongx9527.fw.plus.mybatis.datascope.provider;

import com.xiongx9527.fw.plus.mybatis.datascope.consts.RowOperations;

import java.util.Set;

public interface UserRowPermissionProvider {
	boolean isRowPermissionEnabled();

	void loadTableRowPermissionConfig(TableRowPermissionConfigConsumer consumer);

	default Long getCurrentUserTenantId() {
		return null;
	}

	default Set<Integer> getCurrentUserRowPermission(String tableName, int operation, String requestPath) {
		return null;
	}

	default String getRequestPath() {
		return null;
	}

	default boolean isSupportedOperation(int operation) {
		return operation == RowOperations.SELECT || operation == RowOperations.UPDATE
				|| operation == RowOperations.DELETE;
	}
}
