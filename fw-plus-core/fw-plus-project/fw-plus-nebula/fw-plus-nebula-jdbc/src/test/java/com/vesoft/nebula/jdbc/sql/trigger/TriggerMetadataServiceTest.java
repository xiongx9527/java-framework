package com.vesoft.nebula.jdbc.sql.trigger;

import org.junit.Test;

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
}
