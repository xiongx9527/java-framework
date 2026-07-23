package com.xiongx9527.fw.plus.graph.nebula.jdbc.mysql.replication;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReplicationDumpFlowTest {

	@Test
	public void binlogDumpShouldOpenStreamAtDecodedPositionAndReturnEvents() {
		FakeBinlogStreamReader reader = new FakeBinlogStreamReader();
		reader.events.add(new BinlogStreamEvent(
				"event-1",
				ReplicationStreamEventType.WRITE_ROWS,
				new ReplicationStreamPosition("nebula-binlog.000001", 43L),
				"payload"
		));
		ReplicationCommandHandler handler = new ReplicationCommandHandler(reader, new ReplicationPositionCodec(), true);
		ReplicationSessionState session = authenticatedSession();

		ReplicationCommandResult result = handler.handle(
				ReplicationCommandRequest.binlogDump("nebula-binlog.000001", 42L, 0, 17L),
				session
		);

		assertTrue(result.isOk());
		assertTrue(session.isStreaming());
		assertEquals("nebula-binlog.000001:42", reader.lastRequest.getStartPosition().getToken());
		assertEquals(17L, reader.lastRequest.getReplicaServerId());
		assertEquals(2, result.getEventPackets().size());
		assertEquals(ReplicationEventPacketType.FORMAT_DESCRIPTION, result.getEventPackets().get(0).getType());
		assertEquals(ReplicationEventPacketType.WRITE_ROWS, result.getEventPackets().get(1).getType());
		assertEquals("nebula-binlog.000001:43", session.getLastSentPosition().getToken());
	}

	@Test
	public void binlogDumpShouldMapUnavailablePositionToMysqlError1236() {
		FakeBinlogStreamReader reader = new FakeBinlogStreamReader();
		reader.openFailure = BinlogStreamException.positionUnavailable("Requested binlog position is no longer available");
		ReplicationCommandHandler handler = new ReplicationCommandHandler(reader, new ReplicationPositionCodec(), true);

		ReplicationCommandResult result = handler.handle(
				ReplicationCommandRequest.binlogDump("nebula-binlog.000001", 42L, 0, 17L),
				authenticatedSession()
		);

		assertFalse(result.isOk());
		assertEquals(1236, result.getError().getErrorCode());
		assertEquals("Requested binlog position is no longer available", result.getError().getMessage());
	}

	@Test
	public void binlogDumpShouldMapStreamUnavailableToMysqlError1105() {
		FakeBinlogStreamReader reader = new FakeBinlogStreamReader();
		reader.openFailure = BinlogStreamException.streamUnavailable("Binlog-compatible stream is unavailable");
		ReplicationCommandHandler handler = new ReplicationCommandHandler(reader, new ReplicationPositionCodec(), true);

		ReplicationCommandResult result = handler.handle(
				ReplicationCommandRequest.binlogDump("nebula-binlog.000001", 42L, 0, 17L),
				authenticatedSession()
		);

		assertFalse(result.isOk());
		assertEquals(1105, result.getError().getErrorCode());
		assertEquals("Binlog-compatible stream is unavailable", result.getError().getMessage());
	}

	private ReplicationSessionState authenticatedSession() {
		ReplicationSessionState session = new ReplicationSessionState("connection-1");
		session.markAuthenticated("cdc_user");
		return session;
	}

	private static class FakeBinlogStreamReader implements BinlogStreamReader {
		private BinlogReadRequest lastRequest;
		private final List<BinlogStreamEvent> events = new ArrayList<BinlogStreamEvent>();
		private BinlogStreamException openFailure;

		@Override
		public BinlogStreamCursor open(BinlogReadRequest request) throws BinlogStreamException {
			this.lastRequest = request;
			if (openFailure != null) {
				throw openFailure;
			}
			return new FixedBinlogStreamCursor(events);
		}
	}

	private static class FixedBinlogStreamCursor implements BinlogStreamCursor {
		private final List<BinlogStreamEvent> events;
		private boolean closed;

		private FixedBinlogStreamCursor(List<BinlogStreamEvent> events) {
			this.events = events;
		}

		@Override
		public List<BinlogStreamEvent> read(int maxEvents, long timeoutMillis) {
			return Collections.unmodifiableList(events);
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
