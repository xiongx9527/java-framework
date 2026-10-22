package com.xiongx9527.fw.plus.graph.nebula.jdbc.mysql.replication;

import com.xiongx9527.fw.plus.common.exception.BizException;

public class BinlogStreamException extends BizException {

	/** 状态码 50009005（SERVER/DATABASE/005 MySQL binlog 流读取失败） */
	public static final int CODE = 50_009_005;

	private final BinlogStreamFailure failure;

	private BinlogStreamException(BinlogStreamFailure failure, String message) {
		super(CODE, message);
		this.failure = failure;
	}

	public static BinlogStreamException positionUnavailable(String message) {
		return new BinlogStreamException(BinlogStreamFailure.POSITION_UNAVAILABLE, message);
	}

	public static BinlogStreamException streamUnavailable(String message) {
		return new BinlogStreamException(BinlogStreamFailure.STREAM_UNAVAILABLE, message);
	}

	public static BinlogStreamException eventEncodingFailed(String message) {
		return new BinlogStreamException(BinlogStreamFailure.EVENT_ENCODING_FAILED, message);
	}

	public BinlogStreamFailure getFailure() {
		return failure;
	}
}
