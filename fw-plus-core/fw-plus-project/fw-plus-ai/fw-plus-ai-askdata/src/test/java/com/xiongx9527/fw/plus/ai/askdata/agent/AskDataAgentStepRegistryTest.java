package com.xiongx9527.fw.plus.ai.askdata.agent;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.metadata.AskDataMetadata;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AskDataAgentStepRegistryTest {

	@Test
	void shouldExposeStableReplaceableStepKeys() {
		assertEquals("select-tables", AskDataAgentStepKey.SELECT_TABLES);
		assertEquals("generate-sql", AskDataAgentStepKey.GENERATE_SQL);
		assertEquals("validate-sql", AskDataAgentStepKey.VALIDATE_SQL);
		assertEquals("execute-sql", AskDataAgentStepKey.EXECUTE_SQL);
		assertEquals("interpret-result", AskDataAgentStepKey.INTERPRET_RESULT);
	}

	@Test
	void shouldSelectOnlyUniqueHighestPriorityAgentPerStep() {
		AskDataAgent lower = agent("lower", AskDataAgentStepKey.GENERATE_SQL, 10,
				AskDataAgentStepResult.generatedSql("select 1"));
		AskDataAgent higher = agent("higher", AskDataAgentStepKey.GENERATE_SQL, 20,
				AskDataAgentStepResult.generatedSql("select 2"));

		AskDataAgentStepRegistry registry = new AskDataAgentStepRegistry(Arrays.asList(lower, higher));

		assertSame(higher, registry.get(AskDataAgentStepKey.GENERATE_SQL));
		assertNull(registry.get(AskDataAgentStepKey.EXECUTE_SQL));
	}

	@Test
	void shouldRejectAmbiguousHighestPriorityAgentsAndListConflicts() {
		AskDataAgent first = agent("first", AskDataAgentStepKey.GENERATE_SQL, 20,
				AskDataAgentStepResult.generatedSql("select 1"));
		AskDataAgent second = agent("second", AskDataAgentStepKey.GENERATE_SQL, 20,
				AskDataAgentStepResult.generatedSql("select 2"));

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new AskDataAgentStepRegistry(Arrays.asList(first, second)));

		assertTrue(exception.getMessage().contains(AskDataAgentStepKey.GENERATE_SQL));
		assertTrue(exception.getMessage().contains("first"));
		assertTrue(exception.getMessage().contains("second"));
	}

	@Test
	void shouldRejectUnknownStepKey() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new AskDataAgentStepRegistry(Collections.singletonList(
						agent("unknown", "unknown-step", 1, AskDataAgentStepResult.interpreted("done")))));

		assertTrue(exception.getMessage().contains("unknown-step"));
	}

	@Test
	void shouldBuildImmutableTypedStepContextAndResult() {
		AskDataAgentStepContext context = AskDataAgentStepContext.builder(AskDataAgentStepKey.SELECT_TABLES,
				new AskDataProjectContext("project", "operator"), "订单数量")
				.datasourceKey("orders")
				.conversationId("conversation")
				.metadata(new AskDataMetadata("orders", Arrays.asList("orders", "payments")))
				.rowLimit(100)
				.timeoutMillis(30000L)
				.build();
		AskDataAgentStepResult selected = AskDataAgentStepResult.selectedTables(
				Collections.singletonList("orders"));

		assertEquals("project", context.getProjectContext().getProjectId());
		assertEquals(Arrays.asList("orders", "payments"), context.getMetadata().getResources());
		assertEquals(Collections.singletonList("orders"), selected.getSelectedResources());
		assertThrows(UnsupportedOperationException.class, () -> selected.getSelectedResources().clear());
	}

	private AskDataAgent agent(final String key, final String stepKey, final int priority,
			final AskDataAgentStepResult result) {
		return new AskDataAgent() {
			@Override public String getKey() { return key; }
			@Override public String getStepKey() { return stepKey; }
			@Override public int getPriority() { return priority; }
			@Override public AskDataAgentStepResult execute(AskDataAgentStepContext context) { return result; }
		};
	}
}
