package com.xiongx9527.fw.plus.graph.nebula.jdbc.mysql.replication;

import com.xiongx9527.fw.plus.common.exception.BizException;

public class ReplicationException extends BizException {

	/** 状态码 50009006（SERVER/DATABASE/006 MySQL binlog 复制错误） */
	public static final int CODE = 50_009_006;

	private final ReplicationError error;

	public ReplicationException(ReplicationError error) {
		super(CODE, error.getMessage());
		this.error = error;
	}

	public ReplicationError getError() {
		return error;
	}
}
