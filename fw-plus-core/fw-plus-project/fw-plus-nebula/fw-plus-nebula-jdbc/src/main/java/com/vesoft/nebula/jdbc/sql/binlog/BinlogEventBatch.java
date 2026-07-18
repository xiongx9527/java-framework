package com.vesoft.nebula.jdbc.sql.binlog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BinlogEventBatch {
	private final List<NebulaBinlogEvent> events;
	private final BinlogPosition nextPosition;

	public BinlogEventBatch(List<NebulaBinlogEvent> events, BinlogPosition nextPosition) {
		this.events = events == null ? Collections.<NebulaBinlogEvent>emptyList()
				: Collections.unmodifiableList(new ArrayList<NebulaBinlogEvent>(events));
		this.nextPosition = nextPosition;
	}

	public List<NebulaBinlogEvent> getEvents() {
		return events;
	}

	public BinlogPosition getNextPosition() {
		return nextPosition;
	}

	public boolean isEmpty() {
		return events.isEmpty();
	}
}
