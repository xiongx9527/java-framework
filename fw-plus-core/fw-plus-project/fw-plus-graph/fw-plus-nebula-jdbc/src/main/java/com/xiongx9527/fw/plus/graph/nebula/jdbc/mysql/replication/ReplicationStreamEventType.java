package com.xiongx9527.fw.plus.graph.nebula.jdbc.mysql.replication;

public enum ReplicationStreamEventType {
	WRITE_ROWS,
	UPDATE_ROWS,
	DELETE_ROWS,
	QUERY,
	HEARTBEAT
}
