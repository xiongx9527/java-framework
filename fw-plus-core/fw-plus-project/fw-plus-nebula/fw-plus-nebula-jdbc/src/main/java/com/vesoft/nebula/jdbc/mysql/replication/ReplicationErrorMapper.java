package com.vesoft.nebula.jdbc.mysql.replication;

public final class ReplicationErrorMapper {
	private ReplicationErrorMapper() {
	}

	public static ReplicationError replicationDisabled() {
		return new ReplicationError(1235, "HY000", "Replication subset is not enabled");
	}

	public static ReplicationError accessDenied() {
		return new ReplicationError(1045, "28000", "Access denied");
	}

	public static ReplicationError gtidUnsupported() {
		return new ReplicationError(1235, "HY000", "GTID replication is not supported");
	}

	public static ReplicationError replicationCommandUnsupported() {
		return new ReplicationError(1235, "HY000", "Replication command is not supported");
	}

	public static ReplicationError invalidPosition() {
		return new ReplicationError(1236, "HY000", "Could not parse requested binlog position");
	}

	public static ReplicationError malformedPacket() {
		return new ReplicationError(1235, "HY000", "Malformed replication command packet");
	}

	public static ReplicationError fromStreamException(BinlogStreamException exception) {
		if (exception.getFailure() == BinlogStreamFailure.POSITION_UNAVAILABLE) {
			return new ReplicationError(1236, "HY000", exception.getMessage());
		}
		if (exception.getFailure() == BinlogStreamFailure.EVENT_ENCODING_FAILED) {
			return new ReplicationError(1105, "HY000", "Could not encode replication event");
		}
		return new ReplicationError(1105, "HY000", exception.getMessage());
	}
}
