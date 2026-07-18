package com.vesoft.nebula.jdbc.sql.binlog;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryBinlogEventStore implements BinlogEventStore, BinlogEventReader {
	private final String storeId;
	private final AtomicLong sequence = new AtomicLong();
	private final List<NebulaBinlogEvent> events = new ArrayList<NebulaBinlogEvent>();

	public InMemoryBinlogEventStore(String storeId) {
		if (isBlank(storeId)) {
			throw new IllegalArgumentException("Store id must not be blank.");
		}
		this.storeId = storeId;
	}

	@Override
	public synchronized BinlogPosition append(NebulaBinlogEventDraft draft) throws SQLException {
		if (draft == null) {
			throw new SQLException("Binlog event draft must not be null.");
		}
		BinlogPosition position = new BinlogPosition(sequence.incrementAndGet(), storeId);
		events.add(new NebulaBinlogEvent(position, draft));
		notifyAll();
		return position;
	}

	@Override
	public synchronized BinlogEventBatch readAfter(BinlogPosition position, int maxEvents) throws SQLException {
		BinlogPosition start = normalize(position);
		if (maxEvents <= 0) {
			return new BinlogEventBatch(new ArrayList<NebulaBinlogEvent>(), currentPosition());
		}
		List<NebulaBinlogEvent> selected = new ArrayList<NebulaBinlogEvent>();
		for (NebulaBinlogEvent event : events) {
			if (event.getPosition().getSequence() > start.getSequence()) {
				selected.add(event);
				if (selected.size() >= maxEvents) {
					break;
				}
			}
		}
		BinlogPosition nextPosition = selected.isEmpty()
				? currentPosition()
				: selected.get(selected.size() - 1).getPosition();
		return new BinlogEventBatch(selected, nextPosition);
	}

	@Override
	public synchronized BinlogEventBatch readAfter(BinlogPosition position, int maxEvents, long waitMillis)
			throws SQLException {
		long deadline = System.currentTimeMillis() + Math.max(0L, waitMillis);
		BinlogEventBatch batch = readAfter(position, maxEvents);
		while (batch.isEmpty() && waitMillis > 0L) {
			long remaining = deadline - System.currentTimeMillis();
			if (remaining <= 0L) {
				return batch;
			}
			try {
				wait(remaining);
			} catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
				throw new SQLException("Interrupted while waiting for binlog events.", exception);
			}
			batch = readAfter(position, maxEvents);
		}
		return batch;
	}

	@Override
	public synchronized BinlogPosition currentPosition() throws SQLException {
		return new BinlogPosition(sequence.get(), storeId);
	}

	private BinlogPosition normalize(BinlogPosition position) throws SQLException {
		if (position == null) {
			return new BinlogPosition(0L, storeId);
		}
		if (!storeId.equals(position.getStoreId())) {
			throw new SQLException("Cannot read binlog position from different store: " + position.getStoreId());
		}
		if (position.getSequence() < 0L) {
			throw new SQLException("Binlog position is expired: " + position);
		}
		if (position.getSequence() > sequence.get()) {
			throw new SQLException("Binlog position is unknown: " + position);
		}
		return position;
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}
}
