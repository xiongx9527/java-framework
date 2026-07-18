package com.vesoft.nebula.jdbc.mysql.replication;

public enum ReplicationStreamEventType {
	WRITE_ROWS,
	UPDATE_ROWS,
	DELETE_ROWS,
	QUERY,
	HEARTBEAT
}
