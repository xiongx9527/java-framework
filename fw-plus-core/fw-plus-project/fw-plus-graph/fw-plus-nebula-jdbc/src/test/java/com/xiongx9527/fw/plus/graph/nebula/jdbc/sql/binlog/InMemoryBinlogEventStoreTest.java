package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.binlog;

import org.junit.Test;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class InMemoryBinlogEventStoreTest {

	@Test
	public void appendShouldAssignMonotonicPositionsAndReadAfterKnownPosition() throws Exception {
		InMemoryBinlogEventStore store = new InMemoryBinlogEventStore("store-a");
		NebulaBinlogEventDraft first = draft("event-1", NebulaBinlogEventType.WRITE_ROWS, "person");
		NebulaBinlogEventDraft second = draft("event-2", NebulaBinlogEventType.UPDATE_ROWS, "person");

		BinlogPosition firstPosition = store.append(first);
		BinlogPosition secondPosition = store.append(second);
		BinlogEventBatch batch = store.readAfter(firstPosition, 10);

		assertEquals("store-a", firstPosition.getStoreId());
		assertEquals(1L, firstPosition.getSequence());
		assertEquals(2L, secondPosition.getSequence());
		assertEquals(secondPosition, store.currentPosition());
		assertEquals(1, batch.getEvents().size());
		assertEquals("event-2", batch.getEvents().get(0).getEventId());
		assertEquals(secondPosition, batch.getNextPosition());
	}

	@Test
	public void readAfterNullShouldStartFromBeginningAndRespectMaxEvents() throws Exception {
		InMemoryBinlogEventStore store = new InMemoryBinlogEventStore("store-a");
		store.append(draft("event-1", NebulaBinlogEventType.WRITE_ROWS, "person"));
		BinlogPosition secondPosition = store.append(draft("event-2", NebulaBinlogEventType.DELETE_ROWS, "person"));

		BinlogEventBatch batch = store.readAfter(null, 1);

		assertEquals(1, batch.getEvents().size());
		assertEquals("event-1", batch.getEvents().get(0).getEventId());
		assertEquals(secondPosition, store.currentPosition());
		assertFalse(batch.isEmpty());
	}

	@Test
	public void eventPayloadAndBatchEventsShouldBeImmutable() throws Exception {
		InMemoryBinlogEventStore store = new InMemoryBinlogEventStore("store-a");
		Map<String, Object> payload = new LinkedHashMap<String, Object>();
		payload.put("targetName", "person");
		NebulaBinlogEventDraft draft = new NebulaBinlogEventDraft(
				"event-1",
				NebulaBinlogEventType.WRITE_ROWS,
				"gateway-1",
				"INSERT INTO person(id) VALUES ('p1')",
				"INSERT VERTEX person() VALUES \"p1\":()",
				"person",
				payload,
				100L
		);
		store.append(draft);
		payload.put("targetName", "mutated");

		NebulaBinlogEvent event = store.readAfter(null, 10).getEvents().get(0);

		assertEquals("person", event.getPayload().get("targetName"));
		assertThrowsUnsupportedMapMutation(event.getPayload());
		assertThrowsUnsupportedListMutation(store.readAfter(null, 10).getEvents());
	}

	@Test
	public void readAfterShouldRejectPositionFromAnotherStoreOrBeforeRetention() throws Exception {
		InMemoryBinlogEventStore store = new InMemoryBinlogEventStore("store-a");
		store.append(draft("event-1", NebulaBinlogEventType.WRITE_ROWS, "person"));

		assertReadFails(store, new BinlogPosition(1L, "store-b"), "different store");
		assertReadFails(store, new BinlogPosition(-1L, "store-a"), "expired");
	}

	@Test
	public void readerShouldWaitForAppendedEvents() throws Exception {
		final InMemoryBinlogEventStore store = new InMemoryBinlogEventStore("store-a");
		final BinlogPosition start = store.currentPosition();
		Thread appender = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(50L);
					store.append(draft("event-1", NebulaBinlogEventType.WRITE_ROWS, "person"));
				} catch (Exception exception) {
					throw new RuntimeException(exception);
				}
			}
		});

		appender.start();
		BinlogEventBatch batch = store.readAfter(start, 10, 500L);
		appender.join();

		assertEquals(1, batch.getEvents().size());
		assertEquals("event-1", batch.getEvents().get(0).getEventId());
	}

	private NebulaBinlogEventDraft draft(String eventId, NebulaBinlogEventType type, String schemaName) {
		Map<String, Object> payload = new LinkedHashMap<String, Object>();
		payload.put("schemaName", schemaName);
		return new NebulaBinlogEventDraft(
				eventId,
				type,
				"gateway-1",
				"source sql",
				"ngql",
				schemaName,
				payload,
				System.currentTimeMillis()
		);
	}

	private void assertReadFails(InMemoryBinlogEventStore store, BinlogPosition position, String messagePart)
			throws Exception {
		try {
			store.readAfter(position, 10);
			fail("Expected SQLException");
		} catch (SQLException exception) {
			assertTrue(exception.getMessage().contains(messagePart));
		}
	}

	private void assertThrowsUnsupportedMapMutation(Map<String, Object> payload) {
		try {
			payload.put("x", "y");
			fail("Expected UnsupportedOperationException");
		} catch (UnsupportedOperationException exception) {
			assertSame(UnsupportedOperationException.class, exception.getClass());
		}
	}

	private void assertThrowsUnsupportedListMutation(List<NebulaBinlogEvent> events) {
		try {
			events.clear();
			fail("Expected UnsupportedOperationException");
		} catch (UnsupportedOperationException exception) {
			assertSame(UnsupportedOperationException.class, exception.getClass());
		}
	}
}
