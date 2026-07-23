package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.trigger;

import org.junit.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TriggerMetadataServiceTest {

	@Test
	public void triggerDefinitionShouldExposeImmutableMetadata() {
		TriggerDefinition definition = new TriggerDefinition(
				"test_space",
				"person_bi",
				"person",
				TriggerEvent.INSERT,
				TriggerTiming.BEFORE,
				"INSERT INTO audit(id, name) VALUES ('a1', 'Tom')",
				10L,
				true
		);

		assertEquals("test_space", definition.getSchemaName());
		assertEquals("person_bi", definition.getTriggerName());
		assertEquals("person", definition.getTargetTable());
		assertEquals(TriggerEvent.INSERT, definition.getEvent());
		assertEquals(TriggerTiming.BEFORE, definition.getTiming());
		assertEquals("INSERT INTO audit(id, name) VALUES ('a1', 'Tom')", definition.getBodySql());
		assertEquals(10L, definition.getOrder());
		assertTrue(definition.isEnabled());
	}

	@Test
	public void triggerDefinitionShouldRejectBlankRequiredFields() {
		assertInvalid(new TriggerFactory() {
			public TriggerDefinition create() {
				return new TriggerDefinition("", "person_bi", "person", TriggerEvent.INSERT,
						TriggerTiming.BEFORE, "INSERT INTO audit(id) VALUES ('a1')", 1L, true);
			}
		});
		assertInvalid(new TriggerFactory() {
			public TriggerDefinition create() {
				return new TriggerDefinition("test_space", "person_bi", "person", TriggerEvent.INSERT,
						TriggerTiming.BEFORE, "   ", 1L, true);
			}
		});
	}

	@Test
	public void triggerEventAndTimingShouldParseSupportedValues() {
		assertEquals(TriggerEvent.INSERT, TriggerEvent.fromSql("insert"));
		assertEquals(TriggerEvent.UPDATE, TriggerEvent.fromSql("UPDATE"));
		assertEquals(TriggerEvent.DELETE, TriggerEvent.fromSql("Delete"));
		assertEquals(TriggerTiming.BEFORE, TriggerTiming.fromSql("before"));
		assertEquals(TriggerTiming.AFTER, TriggerTiming.fromSql("AFTER"));

		assertFalse(TriggerEvent.INSERT.matches(null));
		assertTrue(TriggerEvent.INSERT.matches(TriggerEvent.INSERT));
	}

	@Test
	public void triggerDefinitionDraftShouldBuildEnabledDefinition() {
		TriggerDefinitionDraft draft = new TriggerDefinitionDraft("test_space", "person_ai", "person",
				TriggerEvent.INSERT, TriggerTiming.AFTER, "INSERT INTO audit(id) VALUES ('a1')");

		TriggerDefinition definition = draft.toDefinition(5L);

		assertEquals("person_ai", definition.getTriggerName());
		assertEquals(5L, definition.getOrder());
		assertTrue(definition.isEnabled());
		List<TriggerDefinition> singleton = java.util.Collections.singletonList(definition);
		assertEquals(1, singleton.size());
	}

	@Test
	public void metadataServiceShouldCreateDropAndSortEnabledDefinitions() throws Exception {
		TriggerMetadataService service = new TriggerMetadataService(new InMemoryTriggerMetadataStore());

		TriggerDefinition after = service.create(new TriggerDefinitionDraft("test_space", "person_after", "person",
				TriggerEvent.INSERT, TriggerTiming.AFTER, "INSERT INTO audit(id) VALUES ('after')"));
		TriggerDefinition beforeTwo = service.create(new TriggerDefinitionDraft("test_space", "person_before_b", "person",
				TriggerEvent.INSERT, TriggerTiming.BEFORE, "INSERT INTO audit(id) VALUES ('b')"));
		TriggerDefinition beforeOne = service.create(new TriggerDefinitionDraft("test_space", "person_before_a", "person",
				TriggerEvent.INSERT, TriggerTiming.BEFORE, "INSERT INTO audit(id) VALUES ('a')"));

		assertEquals(1L, after.getOrder());
		assertEquals(2L, beforeTwo.getOrder());
		assertEquals(3L, beforeOne.getOrder());

		List<TriggerDefinition> beforeTriggers = service.findEnabled("test_space", "person",
				TriggerEvent.INSERT, TriggerTiming.BEFORE);
		assertEquals("person_before_b", beforeTriggers.get(0).getTriggerName());
		assertEquals("person_before_a", beforeTriggers.get(1).getTriggerName());

		assertTrue(service.drop("test_space", "person_before_b", false).isDropped());
		assertEquals(1, service.findEnabled("test_space", "person", TriggerEvent.INSERT, TriggerTiming.BEFORE).size());
	}

	@Test
	public void metadataServiceShouldRejectDuplicateAndMissingDrop() throws Exception {
		TriggerMetadataService service = new TriggerMetadataService(new InMemoryTriggerMetadataStore());
		TriggerDefinitionDraft draft = new TriggerDefinitionDraft("test_space", "person_before", "person",
				TriggerEvent.UPDATE, TriggerTiming.BEFORE, "INSERT INTO audit(id) VALUES ('a1')");
		service.create(draft);

		assertSqlError("Trigger already exists", new SqlRunnable() {
			public void run() throws SQLException {
				service.create(new TriggerDefinitionDraft("test_space", "person_before", "person",
						TriggerEvent.UPDATE, TriggerTiming.BEFORE, "INSERT INTO audit(id) VALUES ('a2')"));
			}
		});
		assertSqlError("Trigger does not exist", new SqlRunnable() {
			public void run() throws SQLException {
				service.drop("test_space", "missing", false);
			}
		});
		assertFalse(service.drop("test_space", "missing", true).isDropped());
	}

	@Test
	public void metadataServiceShouldFilterDisabledDefinitions() throws Exception {
		InMemoryTriggerMetadataStore store = new InMemoryTriggerMetadataStore();
		store.save(new TriggerDefinition("test_space", "disabled_trigger", "person",
				TriggerEvent.DELETE, TriggerTiming.AFTER, "INSERT INTO audit(id) VALUES ('disabled')",
				1L, false));
		TriggerMetadataService service = new TriggerMetadataService(store);

		assertEquals(0, service.findEnabled("test_space", "person", TriggerEvent.DELETE, TriggerTiming.AFTER).size());
	}

	@Test
	public void triggerExecutionContextShouldLimitRecursionDepth() throws Exception {
		TriggerDefinition definition = new TriggerDefinition("test_space", "person_before", "person",
				TriggerEvent.INSERT, TriggerTiming.BEFORE, "INSERT INTO person(id) VALUES ('p2')", 1L, true);
		TriggerExecutionContext root = TriggerExecutionContext.root(1);
		TriggerExecutionContext first = root.enter(definition);

		assertEquals(1, first.getDepth());
		assertTrue(first.isTriggeredBy("test_space.person_before"));
		assertSqlError("Trigger recursion depth exceeded", new SqlRunnable() {
			public void run() throws SQLException {
				first.enter(definition);
			}
		});
	}

	private void assertInvalid(TriggerFactory factory) {
		try {
			factory.create();
		} catch (IllegalArgumentException expected) {
			return;
		}
		throw new AssertionError("Expected IllegalArgumentException");
	}

	private interface TriggerFactory {
		TriggerDefinition create();
	}

	private void assertSqlError(String messagePart, SqlRunnable runnable) throws Exception {
		try {
			runnable.run();
		} catch (SQLException expected) {
			assertTrue(expected.getMessage().contains(messagePart));
			return;
		}
		throw new AssertionError("Expected SQLException");
	}

	private interface SqlRunnable {
		void run() throws SQLException;
	}
}
