package com.vesoft.nebula.jdbc.sql.binlog;

import java.sql.SQLException;

public interface BinlogEventReader {
	BinlogEventBatch readAfter(BinlogPosition position, int maxEvents, long waitMillis) throws SQLException;
}
