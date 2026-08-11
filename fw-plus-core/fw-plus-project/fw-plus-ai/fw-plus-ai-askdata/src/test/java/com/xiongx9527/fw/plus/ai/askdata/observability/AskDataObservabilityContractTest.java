package com.xiongx9527.fw.plus.ai.askdata.observability;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRoutePolicy;
import com.xiongx9527.fw.plus.ai.askdata.storage.AskDataMonitoringLogStore;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
		assertTrue(eventTypes.contains(AskDataEvent.Type.SUB_QUESTION_STARTED));
		assertTrue(eventTypes.contains(AskDataEvent.Type.SUB_QUESTION_COMPLETED));
		assertTrue(eventTypes.contains(AskDataEvent.Type.MODEL_ROUTED));
		assertTrue(eventTypes.contains(AskDataEvent.Type.AGENT_NODE_EXECUTED));
		assertTrue(eventTypes.contains(AskDataEvent.Type.TOOL_STARTED));
		assertTrue(eventTypes.contains(AskDataEvent.Type.TOOL_COMPLETED));
		assertTrue(eventTypes.contains(AskDataEvent.Type.SQL_PLUGIN_STARTED));
		assertTrue(eventTypes.contains(AskDataEvent.Type.SQL_PLUGIN_COMPLETED));
		assertTrue(eventTypes.contains(AskDataEvent.Type.SQL_COMPOSED));
		assertTrue(eventTypes.contains(AskDataEvent.Type.SQL_VALIDATED));
		assertTrue(eventTypes.contains(AskDataEvent.Type.SQL_EXECUTION_STARTED));
		assertTrue(eventTypes.contains(AskDataEvent.Type.SQL_EXECUTED));
		assertTrue(eventTypes.contains(AskDataEvent.Type.LONG_TERM_MEMORY_RECALL_STARTED));
		assertTrue(eventTypes.contains(AskDataEvent.Type.LONG_TERM_MEMORY_RECALL_COMPLETED));
		assertTrue(eventTypes.contains(AskDataEvent.Type.CONTEXT_COMPRESSION_DEGRADED));
		assertTrue(eventTypes.contains(AskDataEvent.Type.MONITORING_DELIVERY_DEGRADED));
		assertTrue(eventTypes.contains(AskDataEvent.Type.CHECKPOINT_SAVED));
		assertTrue(eventTypes.contains(AskDataEvent.Type.RECOVERY_ATTEMPTED));
		assertTrue(eventTypes.contains(AskDataEvent.Type.CONVERSATION_COMPLETED));
		assertTrue(eventTypes.contains(AskDataEvent.Type.CONVERSATION_FAILED));
		assertTrue(eventTypes.contains(AskDataEvent.Type.GOVERNANCE_DENIED));
	}

	@Test
	public void shouldExposeImmutableExecutionCorrelationDimensions() {
		AskDataEventCorrelation correlation = AskDataEventCorrelation.builder("request-1", "conversation-1")
				.subQuestionId("question-1")
				.graphNodeId("generate-sql")
				.agentKey("business-generator")
				.instanceId("instance-1")
				.parentEventId("parent-1")
				.build();
		AskDataEvent event = AskDataEvent.correlated(new AskDataProjectContext("project-a", "operator-a"),
				"trace-1", AskDataEvent.Type.AGENT_NODE_EXECUTED, "sanitized", correlation);

		assertEquals("request-1", event.getCorrelation().getRequestId());
		assertEquals("question-1", event.getCorrelation().getSubQuestionId());
		assertEquals("generate-sql", event.getCorrelation().getGraphNodeId());
		assertEquals("business-generator", event.getCorrelation().getAgentKey());
		assertEquals("instance-1", event.getCorrelation().getInstanceId());
		assertEquals("parent-1", event.getCorrelation().getParentEventId());
		assertTrue(event.getCorrelation().getEventId() != null);
	}

	@Test
	public void shouldFallBackWithoutThrowingWhenMonitoringStoresFail() {
		AskDataProjectContext context = new AskDataProjectContext("project-a", "operator-a");
		RecordingMonitoringLogStore fallback = new RecordingMonitoringLogStore();
		AskDataMonitoringLogStore failing = new AskDataMonitoringLogStore() {
			@Override public void append(AskDataProjectContext ignored, MonitoringLog log) {
				throw new IllegalStateException("offline");
			}
			@Override public List<MonitoringLog> list(AskDataProjectContext ignored) {
				return new ArrayList<MonitoringLog>();
			}
		};
		AskDataEvent event = new AskDataEvent(context, "trace-1",
				AskDataEvent.Type.CONVERSATION_STARTED, "sanitized");

		assertDoesNotThrow(() -> AskDataEventPublisher.toStore(failing, fallback).publish(event));
		assertEquals(1, fallback.logs.size());
		assertEquals("MONITORING_DELIVERY_DEGRADED", fallback.logs.get(0).getEventType());

		AskDataMonitoringLogStore failingFallback = new AskDataMonitoringLogStore() {
			@Override public void append(AskDataProjectContext ignored, MonitoringLog log) {
				throw new IllegalStateException("fallback offline");
			}
			@Override public List<MonitoringLog> list(AskDataProjectContext ignored) {
				return new ArrayList<MonitoringLog>();
			}
		};
		assertDoesNotThrow(() -> AskDataEventPublisher.toStore(failing, failingFallback).publish(event));
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

	@Test
	public void shouldPreserveGovernanceDimensionsInStoredMonitoringLog() {
		AskDataProjectContext context = new AskDataProjectContext("project-a", "operator-a");
		AskDataEvent event = AskDataEvent.governanceDenied(context, "trace-2", "analytics",
				"COLUMN_DENIED", "policy-v7", "iam-main");
		RecordingMonitoringLogStore store = new RecordingMonitoringLogStore();

		AskDataEventPublisher.toStore(store).publish(event);

		AskDataMonitoringLogStore.MonitoringLog log = store.logs.get(0);
		assertEquals("analytics", log.getDatasourceKey());
		assertEquals("COLUMN_DENIED", log.getDenialCode());
		assertEquals("policy-v7", log.getAuthorizationVersion());
		assertEquals("iam-main", log.getAuthorizationSource());
	}

	@Test
	public void shouldPreserveStructuredModelAttemptDimensions() {
		AskDataProjectContext context = new AskDataProjectContext("project-a", "operator-a");
		AskDataEvent event = AskDataEvent.modelAttempt(context, "trace-model", AskDataEvent.ModelType.EMBEDDING,
				"embedding-primary",
				AskDataModelRoutePolicy.REQUEST_MODEL_KEY, AskDataEvent.ModelState.FAILED,
				"MODEL_INFRASTRUCTURE_FAILURE", "RETRYABLE_INFRASTRUCTURE_FAILURE", null);
		RecordingMonitoringLogStore store = new RecordingMonitoringLogStore();

		AskDataEventPublisher.toStore(store).publish(event);

		AskDataMonitoringLogStore.MonitoringLog log = store.logs.get(0);
		assertEquals("EMBEDDING", log.getModelType());
		assertEquals("embedding-primary", log.getModelKey());
		assertEquals("REQUEST_MODEL_KEY", log.getModelRoutePolicy());
		assertEquals("FAILED", log.getModelState());
		assertEquals("MODEL_INFRASTRUCTURE_FAILURE", log.getModelFailureCode());
		assertEquals("RETRYABLE_INFRASTRUCTURE_FAILURE", log.getModelSwitchReason());
		assertEquals(null, log.getActualModelKey());
	}

	@Test
	public void shouldPreserveCorrelationInMonitoringStorage() {
		AskDataProjectContext context = new AskDataProjectContext("project-a", "operator-a");
		AskDataEvent event = AskDataEvent.correlated(context, "trace-1", AskDataEvent.Type.SQL_COMPOSED,
				"sanitized", AskDataEventCorrelation.builder("request-1", "conversation-1")
						.graphNodeId("generate-sql").build());
		RecordingMonitoringLogStore store = new RecordingMonitoringLogStore();

		AskDataEventPublisher.toStore(store).publish(event);

		assertEquals("request-1", store.logs.get(0).getCorrelation().getRequestId());
		assertEquals("generate-sql", store.logs.get(0).getCorrelation().getGraphNodeId());
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
