package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.binlog;

import java.sql.SQLException;

public interface BinlogEventReader {
	BinlogEventBatch readAfter(BinlogPosition position, int maxEvents, long waitMillis) throws SQLException;
}
