package com.xiongx9527.fw.plus.mybatis.datachange.interceptor;

import com.xiongx9527.fw.plus.mybatis.datachange.api.ConsistencyLevel;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeErrorCode;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeException;

import java.sql.Connection;
import java.sql.SQLException;

public final class JdbcTransactionStateResolver {
	public ConsistencyLevel resolve(Connection connection) {
		if (connection == null) {
			throw new DataChangeException(DataChangeErrorCode.CONFIGURATION,
					"DATA_CHANGE_CONNECTION_UNAVAILABLE");
		}
		try {
			return connection.getAutoCommit()
					? ConsistencyLevel.BEST_EFFORT : ConsistencyLevel.TRANSACTIONAL;
		} catch (SQLException exception) {
			throw new DataChangeException(DataChangeErrorCode.CONFIGURATION,
					"DATA_CHANGE_TRANSACTION_STATE_UNAVAILABLE");
		}
	}
}
