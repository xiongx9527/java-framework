package com.vesoft.nebula.jdbc.mysql.replication;

import com.vesoft.nebula.jdbc.sql.binlog.BinlogEventBatch;
import com.vesoft.nebula.jdbc.sql.binlog.BinlogEventReader;
import com.vesoft.nebula.jdbc.sql.binlog.BinlogPosition;
import com.vesoft.nebula.jdbc.sql.binlog.NebulaBinlogEvent;
import com.vesoft.nebula.jdbc.sql.binlog.NebulaBinlogEventType;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BinlogEventReaderStreamReader implements BinlogStreamReader {
	private static final long FIRST_EVENT_OFFSET = 4L;

	private final BinlogEventReader reader;
	private final String compatibleFileName;

	public BinlogEventReaderStreamReader(BinlogEventReader reader, String compatibleFileName) {
		if (reader == null) {
			throw new IllegalArgumentException("Binlog event reader must not be null.");
		}
		if (isBlank(compatibleFileName)) {
			throw new IllegalArgumentException("Compatible binlog file name must not be blank.");
		}
		this.reader = reader;
		this.compatibleFileName = compatibleFileName;
	}

	@Override
	public BinlogStreamCursor open(BinlogReadRequest request) throws BinlogStreamException {
		if (request == null || request.getStartPosition() == null) {
			throw BinlogStreamException.positionUnavailable("Requested binlog position is not available");
		}
		if (!compatibleFileName.equals(request.getStartPosition().getFileName())) {
			throw BinlogStreamException.positionUnavailable("Requested binlog position is no longer available");
		}
		return new ReaderCursor(reader, toBinlogPosition(request.getStartPosition()), compatibleFileName);
	}

	private BinlogPosition toBinlogPosition(ReplicationStreamPosition position) {
		long sequence = position.getOffset() - FIRST_EVENT_OFFSET;
		if (sequence < 0L) {
			sequence = 0L;
		}
		return new BinlogPosition(sequence, compatibleFileName);
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}

	private static class ReaderCursor implements BinlogStreamCursor {
		private final BinlogEventReader reader;
		private final String compatibleFileName;
		private BinlogPosition currentPosition;
		private boolean closed;

		private ReaderCursor(BinlogEventReader reader, BinlogPosition startPosition, String compatibleFileName) {
			this.reader = reader;
			this.currentPosition = startPosition;
			this.compatibleFileName = compatibleFileName;
		}

		@Override
		public List<BinlogStreamEvent> read(int maxEvents, long timeoutMillis) throws BinlogStreamException {
			if (closed) {
				return new ArrayList<BinlogStreamEvent>();
			}
			try {
				BinlogEventBatch batch = reader.readAfter(currentPosition, maxEvents, timeoutMillis);
				if (batch.getNextPosition() != null) {
					currentPosition = batch.getNextPosition();
				}
				return toStreamEvents(batch.getEvents());
			} catch (SQLException exception) {
				throw mapReadFailure(exception);
			}
		}

		@Override
		public void close() {
			closed = true;
		}

		@Override
		public boolean isClosed() {
			return closed;
		}

		private List<BinlogStreamEvent> toStreamEvents(List<NebulaBinlogEvent> events) throws BinlogStreamException {
			List<BinlogStreamEvent> streamEvents = new ArrayList<BinlogStreamEvent>();
			for (NebulaBinlogEvent event : events) {
				streamEvents.add(new BinlogStreamEvent(
						event.getEventId(),
						toReplicationType(event.getEventType()),
						toStreamPosition(event.getPosition()),
						payload(event)
				));
			}
			return streamEvents;
		}

		private ReplicationStreamPosition toStreamPosition(BinlogPosition position) {
			return new ReplicationStreamPosition(compatibleFileName, position.getSequence() + FIRST_EVENT_OFFSET);
		}

		private ReplicationStreamEventType toReplicationType(NebulaBinlogEventType eventType)
				throws BinlogStreamException {
			if (eventType == NebulaBinlogEventType.WRITE_ROWS) {
				return ReplicationStreamEventType.WRITE_ROWS;
			}
			if (eventType == NebulaBinlogEventType.UPDATE_ROWS) {
				return ReplicationStreamEventType.UPDATE_ROWS;
			}
			if (eventType == NebulaBinlogEventType.DELETE_ROWS) {
				return ReplicationStreamEventType.DELETE_ROWS;
			}
			if (eventType == NebulaBinlogEventType.SCHEMA_CHANGE) {
				return ReplicationStreamEventType.QUERY;
			}
			if (eventType == NebulaBinlogEventType.HEARTBEAT) {
				return ReplicationStreamEventType.HEARTBEAT;
			}
			throw BinlogStreamException.eventEncodingFailed("Unsupported binlog event type: " + eventType);
		}

		private String payload(NebulaBinlogEvent event) {
			if (!isBlank(event.getSql())) {
				return event.getSql();
			}
			if (!isBlank(event.getNgql())) {
				return event.getNgql();
			}
			return event.getEventId();
		}

		private BinlogStreamException mapReadFailure(SQLException exception) {
			String message = exception.getMessage();
			if (message != null && (message.contains("position") || message.contains("different store")
					|| message.contains("expired") || message.contains("unknown"))) {
				return BinlogStreamException.positionUnavailable(message);
			}
			return BinlogStreamException.streamUnavailable(message == null
					? "Binlog-compatible stream is unavailable" : message);
		}
	}
}
