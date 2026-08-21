package com.xiongx9527.fw.plus.graph.nebula.jdbc.mysql.replication;

import com.xiongx9527.fw.plus.common.exception.BizException;

public class ReplicationException extends BizException {
	private final ReplicationError error;

	public ReplicationException(ReplicationError error) {
		super(error.getMessage());
		this.error = error;
	}

	public ReplicationError getError() {
		return error;
	}
}
