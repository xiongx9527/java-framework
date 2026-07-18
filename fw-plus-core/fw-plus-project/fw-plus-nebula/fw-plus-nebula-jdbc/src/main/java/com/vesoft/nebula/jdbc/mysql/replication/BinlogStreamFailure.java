package com.vesoft.nebula.jdbc.mysql.replication;

public enum BinlogStreamFailure {
	POSITION_UNAVAILABLE,
	STREAM_UNAVAILABLE,
	EVENT_ENCODING_FAILED
}
