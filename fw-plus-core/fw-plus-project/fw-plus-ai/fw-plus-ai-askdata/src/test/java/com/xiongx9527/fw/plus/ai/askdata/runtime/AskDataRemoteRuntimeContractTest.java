package com.xiongx9527.fw.plus.ai.askdata.runtime;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AskDataRemoteRuntimeContractTest {

	@Test
	public void shouldCarryIsolationCapabilityAndAuditPayloadToRemoteRuntime() {
		AskDataRemoteRuntimeRequest request = new AskDataRemoteRuntimeRequest(
				new AskDataProjectContext("project-1", "operator-1"), "conversation-1", "request-1", "runtime-java17",
				"audit-1", "统计本月订单");

		assertEquals("project-1", request.getProjectId());
		assertEquals("operator-1", request.getOperatorId());
		assertEquals("conversation-1", request.getConversationId());
		assertEquals("request-1", request.getRequestId());
		assertEquals("runtime-java17", request.getCapabilityKey());
		assertEquals("audit-1", request.getAuditCorrelationId());
		assertEquals("统计本月订单", request.getQuestion());
	}

	@Test
	public void shouldExposeFrameworkNeutralRemoteRuntimeResultAndClientContract() {
		AskDataRuntimeClient client = new AskDataRuntimeClient() {
			@Override
			public AskDataRemoteRuntimeResult execute(AskDataRemoteRuntimeRequest request) {
				return AskDataRemoteRuntimeResult.failure("REMOTE_TIMEOUT", "远程运行时超时", true);
			}
		};

		AskDataRemoteRuntimeResult result = client.execute(new AskDataRemoteRuntimeRequest(
				new AskDataProjectContext("project-1", "operator-1"), "conversation-1", "request-1", "runtime-java17",
				"audit-1", "统计本月订单"));

		assertFalse(result.isSuccess());
		assertTrue(result.isRetryable());
		assertEquals("REMOTE_TIMEOUT", result.getErrorCode());
	}
}
