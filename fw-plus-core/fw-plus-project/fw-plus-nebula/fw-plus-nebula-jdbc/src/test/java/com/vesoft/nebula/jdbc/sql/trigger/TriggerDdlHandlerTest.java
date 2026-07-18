package com.vesoft.nebula.jdbc.sql.trigger;

import org.junit.Test;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TriggerDdlHandlerTest {

	@Test
	public void supportsShouldRecognizeCreateAndDropTrigger() {
		TriggerDdlHandler handler = new TriggerDdlHandler(new TriggerMetadataService(new InMemoryTriggerMetadataStore()));

		assertTrue(handler.supports("CREATE TRIGGER person_bi BEFORE INSERT ON person FOR EACH ROW INSERT INTO audit(id) VALUES ('a1')"));
		assertTrue(handler.supports("drop trigger if exists person_bi"));
		assertFalse(handler.supports("CREATE TABLE person(id varchar(32))"));
	}

	@Test
	public void createTriggerShouldSaveMetadataDefinition() throws Exception {
		TriggerMetadataService service = new TriggerMetadataService(new InMemoryTriggerMetadataStore());
		TriggerDdlHandler handler = new TriggerDdlHandler(service);

		TriggerDdlResult result = handler.handle("test_space",
				"CREATE TRIGGER person_bi BEFORE INSERT ON person FOR EACH ROW INSERT INTO audit(id) VALUES ('a1')");

		assertTrue(result.isHandled());
		assertEquals("person_bi", result.getDefinition().getTriggerName());
		assertEquals("person", result.getDefinition().getTargetTable());
		assertEquals(TriggerEvent.INSERT, result.getDefinition().getEvent());
		assertEquals(TriggerTiming.BEFORE, result.getDefinition().getTiming());
		assertEquals("INSERT INTO audit(id) VALUES ('a1')", result.getDefinition().getBodySql());
		assertEquals(1, service.findEnabled("test_space", "person", TriggerEvent.INSERT, TriggerTiming.BEFORE).size());
	}

	@Test
	public void dropTriggerShouldRemoveMetadataDefinition() throws Exception {
		TriggerMetadataService service = new TriggerMetadataService(new InMemoryTriggerMetadataStore());
		TriggerDdlHandler handler = new TriggerDdlHandler(service);
		handler.handle("test_space",
				"CREATE TRIGGER person_ad AFTER DELETE ON person FOR EACH ROW INSERT INTO audit(id) VALUES ('gone')");

		TriggerDdlResult result = handler.handle("test_space", "DROP TRIGGER person_ad");

		assertTrue(result.isHandled());
		assertTrue(result.getDropResult().isDropped());
		assertEquals(0, service.findEnabled("test_space", "person", TriggerEvent.DELETE, TriggerTiming.AFTER).size());
	}

	@Test
	public void dropTriggerIfExistsShouldBeIdempotent() throws Exception {
		TriggerDdlResult result = new TriggerDdlHandler(new TriggerMetadataService(new InMemoryTriggerMetadataStore()))
				.handle("test_space", "DROP TRIGGER IF EXISTS missing_trigger");

		assertTrue(result.isHandled());
		assertFalse(result.getDropResult().isDropped());
	}

	@Test
	public void createTriggerShouldRejectUnsupportedSyntax() throws Exception {
		TriggerDdlHandler handler = new TriggerDdlHandler(new TriggerMetadataService(new InMemoryTriggerMetadataStore()));

		assertSqlError("Unsupported trigger body", handler,
				"CREATE TRIGGER person_call BEFORE INSERT ON person FOR EACH ROW CALL sync_person()");
		assertSqlError("Unsupported trigger body", handler,
				"CREATE TRIGGER person_begin BEFORE INSERT ON person FOR EACH ROW BEGIN INSERT INTO audit(id) VALUES ('a1'); END");
		assertSqlError("Unsupported trigger body", handler,
				"CREATE TRIGGER person_tx BEFORE INSERT ON person FOR EACH ROW START TRANSACTION");
		assertSqlError("Unsupported trigger body", handler,
				"CREATE TRIGGER nested BEFORE INSERT ON person FOR EACH ROW CREATE TRIGGER child BEFORE INSERT ON audit FOR EACH ROW INSERT INTO x(id) VALUES ('x')");
		assertSqlError("Unsupported CREATE TRIGGER syntax", handler,
				"CREATE TRIGGER person_missing BEFORE INSERT ON person INSERT INTO audit(id) VALUES ('a1')");
	}

	private void assertSqlError(String messagePart, TriggerDdlHandler handler, String sql) throws Exception {
		try {
			handler.handle("test_space", sql);
		} catch (SQLException expected) {
			assertTrue(expected.getMessage().contains(messagePart));
			return;
		}
		throw new AssertionError("Expected SQLException");
	}
}
