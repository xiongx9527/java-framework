package com.xiongx9527.fw.plus.graph.nebula.jdbc.mysql.replication;

public class ReplicationSessionState {
	private final String connectionId;
	private String userName;
	private boolean authenticated;
	private long replicaServerId;
	private String replicaHostName;
	private String requestedFileName;
	private long requestedOffset;
	private int dumpFlags;
	private boolean streaming;
	private ReplicationStreamPosition lastSentPosition;
	private BinlogStreamCursor streamCursor;

	public ReplicationSessionState(String connectionId) {
		this.connectionId = connectionId;
	}

	public void markAuthenticated(String userName) {
		this.userName = userName;
		this.authenticated = true;
	}

	public void registerReplica(long replicaServerId, String replicaHostName) {
		this.replicaServerId = replicaServerId;
		this.replicaHostName = replicaHostName;
	}

	public void startStreaming(BinlogStreamCursor streamCursor, long replicaServerId, String requestedFileName,
			long requestedOffset, int dumpFlags) {
		this.streamCursor = streamCursor;
		this.replicaServerId = replicaServerId;
		this.requestedFileName = requestedFileName;
		this.requestedOffset = requestedOffset;
		this.dumpFlags = dumpFlags;
		this.streaming = true;
	}

	public void markSent(ReplicationStreamPosition lastSentPosition) {
		this.lastSentPosition = lastSentPosition;
	}

	public void closeStream() {
		if (streamCursor != null && !streamCursor.isClosed()) {
			streamCursor.close();
		}
		this.streaming = false;
	}

	public String getConnectionId() {
		return connectionId;
	}

	public String getUserName() {
		return userName;
	}

	public boolean isAuthenticated() {
		return authenticated;
	}

	public long getReplicaServerId() {
		return replicaServerId;
	}

	public String getReplicaHostName() {
		return replicaHostName;
	}

	public String getRequestedFileName() {
		return requestedFileName;
	}

	public long getRequestedOffset() {
		return requestedOffset;
	}

	public int getDumpFlags() {
		return dumpFlags;
	}

	public boolean isStreaming() {
		return streaming;
	}

	public ReplicationStreamPosition getLastSentPosition() {
		return lastSentPosition;
	}
}
