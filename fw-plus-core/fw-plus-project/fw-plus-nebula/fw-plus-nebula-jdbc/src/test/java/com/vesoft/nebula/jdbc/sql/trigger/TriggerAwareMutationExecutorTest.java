package com.vesoft.nebula.jdbc.sql.trigger;

import com.vesoft.nebula.jdbc.sql.write.NebulaDdlPlan;
import com.vesoft.nebula.jdbc.sql.write.NebulaMutationDescriptor;
import com.vesoft.nebula.jdbc.sql.write.NebulaMutationKind;
import com.vesoft.nebula.jdbc.sql.write.NebulaMutationPlan;
import com.vesoft.nebula.jdbc.sql.write.NebulaMutationResult;
import com.vesoft.nebula.jdbc.sql.write.NebulaSqlMutationExecutor;
import org.junit.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

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

		public NebulaMutationResult executeMutation(NebulaMutationPlan plan) {
			calls.add("primary:" + plan.getMutationKind().name() + ":" + plan.getTargetName());
			return new NebulaMutationResult(true, 1, plan.getDescriptor(), null,
					Collections.<String>emptyList(), "primary");
		}

		public NebulaMutationResult executeDdl(NebulaDdlPlan plan) throws SQLException {
			calls.add("ddl");
			return new NebulaMutationResult(true, 0, null, null, Collections.<String>emptyList(), "ddl");
		}
	}
}
