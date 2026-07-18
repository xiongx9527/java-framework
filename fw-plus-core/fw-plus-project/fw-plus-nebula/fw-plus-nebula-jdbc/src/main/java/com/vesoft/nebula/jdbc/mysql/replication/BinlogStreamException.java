package com.vesoft.nebula.jdbc.mysql.replication;

public class BinlogStreamException extends Exception {
	private final BinlogStreamFailure failure;

	private BinlogStreamException(BinlogStreamFailure failure, String message) {
		super(message);
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
