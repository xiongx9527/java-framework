package com.xiongx9527.fw.plus.graph.nebula.jdbc.mysql.replication;

public class ReplicationEventPacket {
	private final ReplicationEventPacketType type;
	private final ReplicationStreamPosition nextPosition;
	private final String payload;

	private ReplicationEventPacket(ReplicationEventPacketType type, ReplicationStreamPosition nextPosition,
			String payload) {
		this.type = type;
		this.nextPosition = nextPosition;
		this.payload = payload;
	}

	public static ReplicationEventPacket formatDescription(ReplicationStreamPosition startPosition) {
		return new ReplicationEventPacket(ReplicationEventPacketType.FORMAT_DESCRIPTION, startPosition,
				"nebula-mysql-replication-subset-v1");
	}

	public static ReplicationEventPacket from(BinlogStreamEvent event) {
		return new ReplicationEventPacket(ReplicationEventPacketType.from(event.getType()), event.getNextPosition(),
				event.getPayload());
	}

	public ReplicationEventPacketType getType() {
		return type;
	}

	public ReplicationStreamPosition getNextPosition() {
		return nextPosition;
	}

	public String getPayload() {
		return payload;
	}
}
