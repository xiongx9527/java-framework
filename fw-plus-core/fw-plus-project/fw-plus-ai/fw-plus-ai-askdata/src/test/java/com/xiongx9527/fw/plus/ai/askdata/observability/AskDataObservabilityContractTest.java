package com.xiongx9527.fw.plus.ai.askdata.observability;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.storage.AskDataMonitoringLogStore;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AskData 观测核心契约测试。
 */
public class AskDataObservabilityContractTest {

	@Test
	public void shouldExposeAllRequiredLifecycleEventTypes() {
		EnumSet<AskDataEvent.Type> eventTypes = EnumSet.copyOf(Arrays.asList(AskDataEvent.Type.values()));

		assertTrue(eventTypes.contains(AskDataEvent.Type.CONVERSATION_STARTED));
		assertTrue(eventTypes.contains(AskDataEvent.Type.QUESTION_PLANNED));
		assertTrue(eventTypes.contains(AskDataEvent.Type.MODEL_ROUTED));
		assertTrue(eventTypes.contains(AskDataEvent.Type.AGENT_NODE_EXECUTED));
		assertTrue(eventTypes.contains(AskDataEvent.Type.SQL_VALIDATED));
		assertTrue(eventTypes.contains(AskDataEvent.Type.SQL_EXECUTED));
		assertTrue(eventTypes.contains(AskDataEvent.Type.CHECKPOINT_SAVED));
		assertTrue(eventTypes.contains(AskDataEvent.Type.RECOVERY_ATTEMPTED));
		assertTrue(eventTypes.contains(AskDataEvent.Type.CONVERSATION_COMPLETED));
		assertTrue(eventTypes.contains(AskDataEvent.Type.CONVERSATION_FAILED));
		assertTrue(eventTypes.contains(AskDataEvent.Type.GOVERNANCE_DENIED));
	}

	@Test
	public void shouldExposeSanitizedGovernanceDenialFields() {
		AskDataEvent event = AskDataEvent.governanceDenied(
				new AskDataProjectContext("project-a", "operator-a"), "conversation-1", "analytics",
				"DATASOURCE_DENIED", "policy-v7", "iam-main");

		assertEquals(AskDataEvent.Type.GOVERNANCE_DENIED, event.getType());
		assertEquals("analytics", event.getDatasourceKey());
		assertEquals("DATASOURCE_DENIED", event.getDenialCode());
		assertEquals("policy-v7", event.getAuthorizationVersion());
		assertEquals("iam-main", event.getAuthorizationSource());
	}

	@Test
	public void shouldAllowProjectAdministratorToReadProjectLogButDenyNormalOperatorAdminLog() {
		AskDataProjectContext administrator = new AskDataProjectContext("project-a", "admin-a");
		AskDataProjectContext normalOperator = new AskDataProjectContext("project-a", "operator-a");
		AskDataMonitoringLog administratorLog = new AskDataMonitoringLog(administrator, "trace-1",
				AskDataEvent.Type.CONVERSATION_STARTED, "已脱敏的问题摘要");
		AskDataMonitoringLogAccessPolicy accessPolicy = AskDataMonitoringLogAccessPolicy.projectAdministratorOnly();

		assertTrue(accessPolicy.canRead(administrator, true, administratorLog));
		assertFalse(accessPolicy.canRead(normalOperator, false, administratorLog));
	}

	@Test
	public void shouldPersistPublishedEventAsSanitizedMonitoringLog() {
		AskDataProjectContext context = new AskDataProjectContext("project-a", "operator-a");
		AskDataEvent event = new AskDataEvent(context, "trace-1", AskDataEvent.Type.SQL_VALIDATED,
				"已脱敏 SQL 校验摘要");
		RecordingMonitoringLogStore store = new RecordingMonitoringLogStore();

		AskDataEventPublisher.toStore(store).publish(event);

		assertEquals(context, store.context);
		assertEquals(1, store.logs.size());
		AskDataMonitoringLogStore.MonitoringLog log = store.logs.get(0);
		assertEquals("trace-1", log.getTraceId());
		assertEquals("SQL_VALIDATED", log.getEventType());
		assertEquals("已脱敏 SQL 校验摘要", log.getMessage());
	}

	private static class RecordingMonitoringLogStore implements AskDataMonitoringLogStore {
		private AskDataProjectContext context;
		private final List<MonitoringLog> logs = new ArrayList<MonitoringLog>();

		@Override
		public void append(AskDataProjectContext context, MonitoringLog log) {
			this.context = context;
			this.logs.add(log);
		}

		@Override
		public List<MonitoringLog> list(AskDataProjectContext context) {
			return logs;
		}
	}
}
