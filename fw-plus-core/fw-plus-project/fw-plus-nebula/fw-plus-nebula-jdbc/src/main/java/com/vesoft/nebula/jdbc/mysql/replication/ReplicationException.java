package com.vesoft.nebula.jdbc.mysql.replication;

public class ReplicationException extends Exception {
	private final ReplicationError error;

	public ReplicationException(ReplicationError error) {
		super(error.getMessage());
		this.error = error;
	}

	public ReplicationError getError() {
		return error;
	}
}
