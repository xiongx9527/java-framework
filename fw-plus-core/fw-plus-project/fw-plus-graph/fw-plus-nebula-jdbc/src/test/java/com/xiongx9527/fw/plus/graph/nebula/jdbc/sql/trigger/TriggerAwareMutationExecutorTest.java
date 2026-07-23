package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.trigger;

import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.write.NebulaDdlPlan;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.write.NebulaMutationDescriptor;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.write.NebulaMutationKind;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.write.NebulaMutationPlan;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.write.NebulaMutationResult;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.write.NebulaSqlMutationExecutor;
import org.junit.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TriggerAwareMutationExecutorTest {

	@Test
	public void insertShouldExecuteBeforePrimaryAndAfterTriggersInOrder() throws Exception {
		List<String> calls = new ArrayList<String>();
		TriggerMetadataService service = new TriggerMetadataService(new InMemoryTriggerMetadataStore());
		service.create(draft("person_before", TriggerEvent.INSERT, TriggerTiming.BEFORE));
		service.create(draft("person_after", TriggerEvent.INSERT, TriggerTiming.AFTER));

		new TriggerAwareMutationExecutor("test_space", service, new RecordingBodyExecutor(calls),
				new RecordingMutationExecutor(calls), 8)
				.executeMutation(plan(NebulaMutationKind.INSERT_VERTEX, "person"));

		assertEquals(list("body:person_before:depth=1", "primary:INSERT_VERTEX:person",
				"body:person_after:depth=1"), calls);
	}

	@Test
	public void updateAndDeleteShouldMapToMatchingTriggerEvents() throws Exception {
		List<String> calls = new ArrayList<String>();
		TriggerMetadataService service = new TriggerMetadataService(new InMemoryTriggerMetadataStore());
		service.create(draft("person_update", TriggerEvent.UPDATE, TriggerTiming.BEFORE));
		service.create(draft("person_delete", TriggerEvent.DELETE, TriggerTiming.AFTER));

		TriggerAwareMutationExecutor executor = new TriggerAwareMutationExecutor("test_space", service,
				new RecordingBodyExecutor(calls), new RecordingMutationExecutor(calls), 8);
		executor.executeMutation(plan(NebulaMutationKind.UPDATE_VERTEX, "person"));
		executor.executeMutation(plan(NebulaMutationKind.DELETE_VERTEX, "person"));

		assertEquals(list("body:person_update:depth=1", "primary:UPDATE_VERTEX:person",
				"primary:DELETE_VERTEX:person", "body:person_delete:depth=1"), calls);
	}

	@Test
	public void noMatchingTriggerShouldOnlyExecutePrimaryMutation() throws Exception {
		List<String> calls = new ArrayList<String>();

		new TriggerAwareMutationExecutor("test_space",
				new TriggerMetadataService(new InMemoryTriggerMetadataStore()),
				new RecordingBodyExecutor(calls), new RecordingMutationExecutor(calls), 8)
				.executeMutation(plan(NebulaMutationKind.INSERT_EDGE, "follow"));

		assertEquals(list("primary:INSERT_EDGE:follow"), calls);
	}

	@Test
	public void ddlExecutionShouldDelegateWithoutTriggerHooks() throws Exception {
		List<String> calls = new ArrayList<String>();

		new TriggerAwareMutationExecutor("test_space",
				new TriggerMetadataService(new InMemoryTriggerMetadataStore()),
				new RecordingBodyExecutor(calls), new RecordingMutationExecutor(calls), 8)
				.executeDdl(null);

		assertEquals(list("ddl"), calls);
	}

	@Test
	public void beforeFailureShouldSkipPrimaryAndAfterTriggers() throws Exception {
		List<String> calls = new ArrayList<String>();
		TriggerMetadataService service = new TriggerMetadataService(new InMemoryTriggerMetadataStore());
		service.create(draft("person_before", TriggerEvent.INSERT, TriggerTiming.BEFORE));
		service.create(draft("person_after", TriggerEvent.INSERT, TriggerTiming.AFTER));

		assertSqlError("before failed", new SqlRunnable() {
			public void run() throws SQLException {
				new TriggerAwareMutationExecutor("test_space", service,
						new FailingBodyExecutor(calls, "person_before", "before failed"),
						new RecordingMutationExecutor(calls), 8)
						.executeMutation(plan(NebulaMutationKind.INSERT_VERTEX, "person"));
			}
		});

		assertEquals(list("body:person_before"), calls);
	}

	@Test
	public void primaryFailureShouldSkipAfterTriggers() throws Exception {
		List<String> calls = new ArrayList<String>();
		TriggerMetadataService service = new TriggerMetadataService(new InMemoryTriggerMetadataStore());
		service.create(draft("person_after", TriggerEvent.INSERT, TriggerTiming.AFTER));

		assertSqlError("primary failed", new SqlRunnable() {
			public void run() throws SQLException {
				new TriggerAwareMutationExecutor("test_space", service, new RecordingBodyExecutor(calls),
						new FailingMutationExecutor(calls), 8)
						.executeMutation(plan(NebulaMutationKind.INSERT_VERTEX, "person"));
			}
		});

		assertEquals(list("primary:INSERT_VERTEX:person"), calls);
	}

	@Test
	public void unsuccessfulPrimaryResultShouldSkipAfterTriggers() throws Exception {
		List<String> calls = new ArrayList<String>();
		TriggerMetadataService service = new TriggerMetadataService(new InMemoryTriggerMetadataStore());
		service.create(draft("person_after", TriggerEvent.INSERT, TriggerTiming.AFTER));

		NebulaMutationResult result = new TriggerAwareMutationExecutor("test_space", service,
				new RecordingBodyExecutor(calls), new UnsuccessfulMutationExecutor(calls), 8)
				.executeMutation(plan(NebulaMutationKind.INSERT_VERTEX, "person"));

		assertTrue(!result.isSuccess());
		assertEquals(list("primary:INSERT_VERTEX:person"), calls);
	}

	@Test
	public void afterFailureShouldReportErrorAfterPrimaryMutation() throws Exception {
		List<String> calls = new ArrayList<String>();
		TriggerMetadataService service = new TriggerMetadataService(new InMemoryTriggerMetadataStore());
		service.create(draft("person_after", TriggerEvent.INSERT, TriggerTiming.AFTER));

		assertSqlError("after failed", new SqlRunnable() {
			public void run() throws SQLException {
				new TriggerAwareMutationExecutor("test_space", service,
						new FailingBodyExecutor(calls, "person_after", "after failed"),
						new RecordingMutationExecutor(calls), 8)
						.executeMutation(plan(NebulaMutationKind.INSERT_VERTEX, "person"));
			}
		});

		assertEquals(list("primary:INSERT_VERTEX:person", "body:person_after"), calls);
	}

	@Test
	public void recursiveBodyShouldStopAtMaxDepth() throws Exception {
		List<String> calls = new ArrayList<String>();
		TriggerMetadataService service = new TriggerMetadataService(new InMemoryTriggerMetadataStore());
		service.create(draft("person_before", TriggerEvent.INSERT, TriggerTiming.BEFORE));
		RecordingMutationExecutor delegate = new RecordingMutationExecutor(calls);
		final TriggerAwareMutationExecutor[] holder = new TriggerAwareMutationExecutor[1];
		holder[0] = new TriggerAwareMutationExecutor("test_space", service, new TriggerBodyExecutor() {
			public NebulaMutationResult executeBody(TriggerDefinition definition, NebulaMutationPlan parentPlan,
					TriggerExecutionContext context) throws SQLException {
				calls.add("body:" + definition.getTriggerName() + ":depth=" + context.getDepth());
				return holder[0].executeMutation(parentPlan, context);
			}
		}, delegate, 1);

		assertSqlError("Trigger recursion depth exceeded", new SqlRunnable() {
			public void run() throws SQLException {
				holder[0].executeMutation(plan(NebulaMutationKind.INSERT_VERTEX, "person"));
			}
		});

		assertEquals(list("body:person_before:depth=1"), calls);
	}

	private TriggerDefinitionDraft draft(String name, TriggerEvent event, TriggerTiming timing) {
		return new TriggerDefinitionDraft("test_space", name, "person", event, timing,
				"INSERT INTO audit(id) VALUES ('a1')");
	}

	private NebulaMutationPlan plan(NebulaMutationKind kind, String targetName) {
		NebulaMutationDescriptor descriptor = new NebulaMutationDescriptor(kind, targetName,
				Collections.<String, Object>emptyMap(), Collections.<String, Object>emptyMap(),
				"source", "ngql");
		return new NebulaMutationPlan(kind, "ngql", targetName,
				Collections.<String, Object>emptyMap(), Collections.<String, Object>emptyMap(),
				"single-plan-success", descriptor, Collections.<String>emptyList());
	}

	private List<String> list(String... values) {
		List<String> result = new ArrayList<String>();
		Collections.addAll(result, values);
		return result;
	}

	private static class RecordingBodyExecutor implements TriggerBodyExecutor {
		private final List<String> calls;

		private RecordingBodyExecutor(List<String> calls) {
			this.calls = calls;
		}

		public NebulaMutationResult executeBody(TriggerDefinition definition, NebulaMutationPlan parentPlan,
				TriggerExecutionContext context) {
			calls.add("body:" + definition.getTriggerName() + ":depth=" + context.getDepth());
			return new NebulaMutationResult(true, 1, parentPlan.getDescriptor(), null,
					Collections.<String>emptyList(), "body");
		}
	}

	private static class RecordingMutationExecutor implements NebulaSqlMutationExecutor {
		private final List<String> calls;

		private RecordingMutationExecutor(List<String> calls) {
			this.calls = calls;
		}

		public NebulaMutationResult executeMutation(NebulaMutationPlan plan) throws SQLException {
			calls.add("primary:" + plan.getMutationKind().name() + ":" + plan.getTargetName());
			return new NebulaMutationResult(true, 1, plan.getDescriptor(), null,
					Collections.<String>emptyList(), "primary");
		}

		public NebulaMutationResult executeDdl(NebulaDdlPlan plan) throws SQLException {
			calls.add("ddl");
			return new NebulaMutationResult(true, 0, null, null, Collections.<String>emptyList(), "ddl");
		}
	}

	private static class FailingBodyExecutor implements TriggerBodyExecutor {
		private final List<String> calls;
		private final String triggerName;
		private final String message;

		private FailingBodyExecutor(List<String> calls, String triggerName, String message) {
			this.calls = calls;
			this.triggerName = triggerName;
			this.message = message;
		}

		public NebulaMutationResult executeBody(TriggerDefinition definition, NebulaMutationPlan parentPlan,
				TriggerExecutionContext context) throws SQLException {
			calls.add("body:" + definition.getTriggerName());
			if (triggerName.equals(definition.getTriggerName())) {
				throw new SQLException(message, "HY000");
			}
			return new NebulaMutationResult(true, 1, parentPlan.getDescriptor(), null,
					Collections.<String>emptyList(), "body");
		}
	}

	private static class FailingMutationExecutor extends RecordingMutationExecutor {
		private FailingMutationExecutor(List<String> calls) {
			super(calls);
		}

		public NebulaMutationResult executeMutation(NebulaMutationPlan plan) throws SQLException {
			super.executeMutation(plan);
			throw new SQLException("primary failed", "HY000");
		}
	}

	private static class UnsuccessfulMutationExecutor extends RecordingMutationExecutor {
		private UnsuccessfulMutationExecutor(List<String> calls) {
			super(calls);
		}

		public NebulaMutationResult executeMutation(NebulaMutationPlan plan) throws SQLException {
			super.executeMutation(plan);
			return new NebulaMutationResult(false, 0, plan.getDescriptor(), null,
					Collections.<String>emptyList(), "primary-false");
		}
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
