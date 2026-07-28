package com.xiongx9527.fw.plus.ai.askdata.observability;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumSet;

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
}
