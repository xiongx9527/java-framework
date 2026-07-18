package com.vesoft.nebula.jdbc.mysql.replication;

import java.util.List;

public interface BinlogStreamCursor {
	List<BinlogStreamEvent> read(int maxEvents, long timeoutMillis) throws BinlogStreamException;

	void close();

	boolean isClosed();
}
