package com.xiongx9527.fw.plus.graph.nebula.jdbc.mysql.replication;

public enum ReplicationEventPacketType {
	FORMAT_DESCRIPTION,
	WRITE_ROWS,
	UPDATE_ROWS,
	DELETE_ROWS,
	QUERY,
	HEARTBEAT;

	public static ReplicationEventPacketType from(ReplicationStreamEventType eventType) {
		if (eventType == ReplicationStreamEventType.WRITE_ROWS) {
			return WRITE_ROWS;
		}
		if (eventType == ReplicationStreamEventType.UPDATE_ROWS) {
			return UPDATE_ROWS;
		}
		if (eventType == ReplicationStreamEventType.DELETE_ROWS) {
			return DELETE_ROWS;
		}
		if (eventType == ReplicationStreamEventType.QUERY) {
			return QUERY;
		}
		return HEARTBEAT;
	}
}
