package com.xiongx9527.fw.plus.ai.askdata.runtime;

import com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgent;
import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataChatModel;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEventPublisher;
import com.xiongx9527.fw.plus.ai.askdata.vector.AskDataEmbeddingModel;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AskDataRuntimeAdapterContractTest {

	@Test
	void 运行时适配器必须显式映射核心能力和观测状态() {
		AskDataRuntimeAdapter adapter = new AskDataRuntimeAdapter() {
			@Override public AskDataChatModel chatModel() { return new AskDataChatModel() {
				@Override public com.xiongx9527.fw.plus.ai.askdata.model.AskDataChatResult chat(
						com.xiongx9527.fw.plus.ai.askdata.model.AskDataChatRequest request) {
					return com.xiongx9527.fw.plus.ai.askdata.model.AskDataChatResult.success("ok");
				}
			}; }
			@Override public AskDataEmbeddingModel embeddingModel() { return null; }
			@Override public com.xiongx9527.fw.plus.ai.askdata.vector.AskDataVectorStore vectorStore() { return null; }
			@Override public com.xiongx9527.fw.plus.ai.askdata.tool.AskDataTool tool(String key) { return null; }
			@Override public com.xiongx9527.fw.plus.ai.askdata.vector.AskDataRagRetriever ragRetriever() { return null; }
			@Override public AskDataAgent agent(String key) { return null; }
			@Override public AskDataRuntimeStatus status() { return AskDataRuntimeStatus.READY; }
			@Override public AskDataEventPublisher eventPublisher() { return AskDataEventPublisher.toStore(com.xiongx9527.fw.plus.ai.askdata.storage.InMemoryAskDataStores.singleNode().monitoringLogs()); }
		};

		assertEquals(AskDataRuntimeStatus.READY, adapter.status());
		assertTrue(adapter.supports("chat"));
		assertFalse(adapter.supports("missing"));
	}

	@Test
	void 远程协议必须保留版本鉴权幂等流式审计和回滚边界() {
		AskDataProjectContext context = new AskDataProjectContext("project", "operator");
		AskDataRemoteRuntimeRequest request = new AskDataRemoteRuntimeRequest(context, "conversation", "request", "capability",
				"audit", "question", "v1", "subject", "idem-1");
		AskDataRemoteRuntimeEvent event = AskDataRemoteRuntimeEvent.delta("request", "answer");
		AskDataRemoteRuntimeResult error = AskDataRemoteRuntimeResult.failure("REMOTE_TIMEOUT", "timeout", true);

		assertEquals("v1", request.getProtocolVersion());
		assertEquals("subject", request.getAuthenticationSubject());
		assertEquals("idem-1", request.getIdempotencyKey());
		assertEquals("answer", event.getContent());
		assertTrue(AskDataRemoteRuntimeErrorMapper.defaultMapper().isRollbackRequired(error));
		assertEquals(Collections.singletonList("audit"), AskDataRemoteRuntimeAudit.from(request, error).getCorrelationIds());
	}
}
