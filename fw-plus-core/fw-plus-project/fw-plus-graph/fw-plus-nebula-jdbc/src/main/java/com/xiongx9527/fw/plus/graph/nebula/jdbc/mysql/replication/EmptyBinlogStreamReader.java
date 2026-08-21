package com.xiongx9527.fw.plus.graph.nebula.jdbc.mysql.replication;

import java.util.Collections;
import java.util.List;

public class EmptyBinlogStreamReader implements BinlogStreamReader {
	@Override
	public BinlogStreamCursor open(BinlogReadRequest request) {
		return new EmptyCursor();
	}

	private static class EmptyCursor implements BinlogStreamCursor {
		private boolean closed;

		@Override
		public List<BinlogStreamEvent> read(int maxEvents, long timeoutMillis) {
			return Collections.emptyList();
		}

		@Override
		public void close() {
			this.closed = true;
		}

		@Override
		public boolean isClosed() {
			return closed;
		}
	}
}
