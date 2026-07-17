package com.vesoft.nebula.jdbc.mysql.query;

import com.vesoft.nebula.jdbc.mysql.protocol.MysqlErrPacket;

import java.sql.SQLException;

public final class MysqlErrorMapper {
	private MysqlErrorMapper() {
	}

	public static MysqlGatewayException accessDenied() {
		return new MysqlGatewayException("Access denied", "28000", 1045);
	}

	public static MysqlGatewayException syntax(String message) {
		return new MysqlGatewayException(message == null ? "You have an error in your SQL syntax" : message, "42000", 1064);
	}

	public static MysqlGatewayException readOnly(String message) {
		return new MysqlGatewayException(message == null ? "The server is running in read-only mode" : message, "HY000", 1290);
	}

	public static MysqlGatewayException unsupported(String message) {
		return new MysqlGatewayException(message == null ? "Command is not supported" : message, "HY000", 1235);
	}

	public static MysqlGatewayException tableNotFound(String tableName) {
		return new MysqlGatewayException("Table '" + tableName + "' doesn't exist", "42S02", 1146);
	}

	public static MysqlGatewayException execution(SQLException exception) {
		if (exception instanceof MysqlGatewayException) {
			return (MysqlGatewayException) exception;
		}
		return new MysqlGatewayException("Nebula execution failed", "HY000", 1105);
	}

	public static MysqlErrPacket toErrPacket(SQLException exception) {
		return MysqlErrPacket.of(exception.getErrorCode(), exception.getSQLState(), exception.getMessage());
	}
}
