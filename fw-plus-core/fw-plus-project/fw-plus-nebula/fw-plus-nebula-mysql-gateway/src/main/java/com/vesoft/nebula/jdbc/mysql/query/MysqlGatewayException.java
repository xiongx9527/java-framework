package com.vesoft.nebula.jdbc.mysql.query;

import java.sql.SQLException;

public class MysqlGatewayException extends SQLException {
	public MysqlGatewayException(String message, String sqlState, int vendorCode) {
		super(message, sqlState, vendorCode);
	}
}
