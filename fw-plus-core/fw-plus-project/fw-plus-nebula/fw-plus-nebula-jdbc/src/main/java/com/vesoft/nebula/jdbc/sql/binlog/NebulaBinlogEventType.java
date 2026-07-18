package com.vesoft.nebula.jdbc.sql.binlog;

public enum NebulaBinlogEventType {
	WRITE_ROWS,
	UPDATE_ROWS,
	DELETE_ROWS,
	SCHEMA_CHANGE,
	HEARTBEAT
}
