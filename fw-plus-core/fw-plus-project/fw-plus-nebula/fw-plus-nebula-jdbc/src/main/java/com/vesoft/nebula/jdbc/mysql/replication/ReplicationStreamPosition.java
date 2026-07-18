package com.vesoft.nebula.jdbc.mysql.replication;

public class ReplicationStreamPosition {
	private final String fileName;
	private final long offset;

	public ReplicationStreamPosition(String fileName, long offset) {
		this.fileName = fileName;
		this.offset = offset;
	}

	public String getFileName() {
		return fileName;
	}

	public long getOffset() {
		return offset;
	}

	public String getToken() {
		return fileName + ":" + offset;
	}
}
