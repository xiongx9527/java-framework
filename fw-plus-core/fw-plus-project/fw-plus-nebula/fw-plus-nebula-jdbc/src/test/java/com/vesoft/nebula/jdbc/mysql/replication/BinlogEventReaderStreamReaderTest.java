package com.vesoft.nebula.jdbc.mysql.replication;

import com.vesoft.nebula.jdbc.sql.binlog.InMemoryBinlogEventStore;
import com.vesoft.nebula.jdbc.sql.binlog.NebulaBinlogEventDraft;
import com.vesoft.nebula.jdbc.sql.binlog.NebulaBinlogEventType;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BinlogEventReaderStreamReaderTest {

	@Test
	public void replicationDumpShouldReadEventsFromBinlogCompatibleReader() throws Exception {
		InMemoryBinlogEventStore store = new InMemoryBinlogEventStore("nebula-binlog.000001");
		store.append(draft("event-1", NebulaBinlogEventType.WRITE_ROWS,
				"INSERT INTO person(id) VALUES ('p1')"));
		store.append(draft("event-2", NebulaBinlogEventType.SCHEMA_CHANGE,
				"CREATE TAG person(name string)"));
		ReplicationCommandHandler handler = new ReplicationCommandHandler(
				new BinlogEventReaderStreamReader(store, "nebula-binlog.000001"),
				new ReplicationPositionCodec(),
				true
		);
		ReplicationSessionState session = new ReplicationSessionState("connection-1");
		session.markAuthenticated("cdc_user");

		ReplicationCommandResult result = handler.handle(
				ReplicationCommandRequest.binlogDump("nebula-binlog.000001", 4L, 0, 27L),
				session
		);

		assertTrue(result.isOk());
		assertEquals(3, result.getEventPackets().size());
		assertEquals(ReplicationEventPacketType.FORMAT_DESCRIPTION, result.getEventPackets().get(0).getType());
		assertEquals(ReplicationEventPacketType.WRITE_ROWS, result.getEventPackets().get(1).getType());
		assertEquals("INSERT INTO person(id) VALUES ('p1')", result.getEventPackets().get(1).getPayload());
		assertEquals(ReplicationEventPacketType.QUERY, result.getEventPackets().get(2).getType());
		assertEquals("CREATE TAG person(name string)", result.getEventPackets().get(2).getPayload());
		assertEquals("nebula-binlog.000001:6", session.getLastSentPosition().getToken());
	}

	@Test
	public void replicationDumpShouldMapReaderSqlExceptionToPositionUnavailable() throws Exception {
		InMemoryBinlogEventStore store = new InMemoryBinlogEventStore("different-store");
		ReplicationCommandHandler handler = new ReplicationCommandHandler(
				new BinlogEventReaderStreamReader(store, "nebula-binlog.000001"),
				new ReplicationPositionCodec(),
				true
		);
		ReplicationSessionState session = new ReplicationSessionState("connection-1");
		session.markAuthenticated("cdc_user");

		ReplicationCommandResult result = handler.handle(
				ReplicationCommandRequest.binlogDump("nebula-binlog.000001", 4L, 0, 27L),
				session
		);

		assertEquals(1236, result.getError().getErrorCode());
		assertFalse(session.isStreaming());
	}

	private NebulaBinlogEventDraft draft(String eventId, NebulaBinlogEventType type, String sql) {
		Map<String, Object> payload = new LinkedHashMap<String, Object>();
		payload.put("sql", sql);
		return new NebulaBinlogEventDraft(
				eventId,
				type,
				"gateway-1",
				sql,
				"ngql",
				"person",
				payload,
				100L
		);
	}
}
