package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.binlog;

import java.sql.SQLException;

public interface BinlogEventStore {
	BinlogPosition append(NebulaBinlogEventDraft event) throws SQLException;

	BinlogEventBatch readAfter(BinlogPosition position, int maxEvents) throws SQLException;

	BinlogPosition currentPosition() throws SQLException;
}
