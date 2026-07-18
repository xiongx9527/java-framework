package com.vesoft.nebula.jdbc.mysql.replication;

public class BinlogReadRequest {
	private final String connectionId;
	private final String userName;
	private final long replicaServerId;
	private final ReplicationStreamPosition startPosition;
	private final int dumpFlags;
	private final boolean continuous;
	private final int batchSize;
	private final long timeoutMillis;

	public BinlogReadRequest(String connectionId, String userName, long replicaServerId,
			ReplicationStreamPosition startPosition, int dumpFlags, boolean continuous, int batchSize,
			long timeoutMillis) {
		this.connectionId = connectionId;
		this.userName = userName;
		this.replicaServerId = replicaServerId;
		this.startPosition = startPosition;
		this.dumpFlags = dumpFlags;
		this.continuous = continuous;
		this.batchSize = batchSize;
		this.timeoutMillis = timeoutMillis;
	}

	public String getConnectionId() {
		return connectionId;
	}

	public String getUserName() {
		return userName;
	}

	public long getReplicaServerId() {
		return replicaServerId;
	}

	public ReplicationStreamPosition getStartPosition() {
		return startPosition;
	}

	public int getDumpFlags() {
		return dumpFlags;
	}

	public boolean isContinuous() {
		return continuous;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public long getTimeoutMillis() {
		return timeoutMillis;
	}
}
