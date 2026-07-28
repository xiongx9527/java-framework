package com.xiongx9527.fw.plus.graph.nebula.jdbc.mysql.replication;

public class BinlogStreamEvent {
	private final String eventId;
	private final ReplicationStreamEventType type;
	private final ReplicationStreamPosition nextPosition;
	private final String payload;

	public BinlogStreamEvent(String eventId, ReplicationStreamEventType type, ReplicationStreamPosition nextPosition,
			String payload) {
		this.eventId = eventId;
		this.type = type;
		this.nextPosition = nextPosition;
		this.payload = payload;
	}

	public String getEventId() {
		return eventId;
	}

	public ReplicationStreamEventType getType() {
		return type;
	}

	public ReplicationStreamPosition getNextPosition() {
		return nextPosition;
	}

	public String getPayload() {
		return payload;
	}
}
