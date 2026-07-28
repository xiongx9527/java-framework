package com.xiongx9527.fw.plus.ai.askdata.model;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.metadata.AskDataMetadata;
import com.xiongx9527.fw.plus.ai.askdata.metadata.AskDataMetadataProvider;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataPermissionEvaluator;
import com.xiongx9527.fw.plus.ai.askdata.tool.AskDataTool;
import com.xiongx9527.fw.plus.ai.askdata.tool.AskDataToolRequest;
import com.xiongx9527.fw.plus.ai.askdata.tool.AskDataToolResult;
import com.xiongx9527.fw.plus.ai.askdata.vector.AskDataEmbeddingModel;
import com.xiongx9527.fw.plus.ai.askdata.vector.AskDataRagRetriever;
import com.xiongx9527.fw.plus.ai.askdata.vector.AskDataVectorDocument;
import com.xiongx9527.fw.plus.ai.askdata.vector.AskDataVectorSearchRequest;
import com.xiongx9527.fw.plus.ai.askdata.vector.AskDataVectorStore;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AskDataCoreSpiContractTest {

	@Test
	void 核心SPI必须携带隔离上下文且不依赖具体AI框架() {
		AskDataProjectContext context = new AskDataProjectContext("project", "operator");
		AskDataChatModel chatModel = new AskDataChatModel() {
			@Override
			public AskDataChatResult chat(AskDataChatRequest request) {
				return AskDataChatResult.success(request.getQuestion() + "-answer");
			}
		};
		AskDataEmbeddingModel embeddingModel = new AskDataEmbeddingModel() {
			@Override
			public float[] embed(AskDataProjectContext actualContext, String text) {
				assertEquals(context, actualContext);
				return new float[] {1F, 2F};
			}
		};
		AskDataVectorStore store = new AskDataVectorStore() {
			@Override
			public void upsert(AskDataProjectContext actualContext, AskDataVectorDocument document) {
				assertEquals(context, actualContext);
			}

			@Override
			public java.util.List<AskDataVectorDocument> search(AskDataProjectContext actualContext,
					AskDataVectorSearchRequest request) {
				assertEquals(context, actualContext);
				return Collections.singletonList(new AskDataVectorDocument("doc", "knowledge", new float[] {1F}));
			}
		};
		AskDataTool tool = new AskDataTool() {
			@Override
			public String getKey() { return "tool"; }
			@Override
			public AskDataToolResult execute(AskDataToolRequest request) {
				return AskDataToolResult.success(request.getArguments().get("key"));
			}
		};

		assertEquals("question-answer", chatModel.chat(new AskDataChatRequest(context, "question")).getAnswer());
		assertEquals(2, embeddingModel.embed(context, "knowledge").length);
		store.upsert(context, new AskDataVectorDocument("doc", "knowledge", new float[] {1F}));
		assertEquals("doc", new AskDataRagRetriever(store).retrieve(context,
				new AskDataVectorSearchRequest(new float[] {1F}, 1)).get(0).getId());
		assertEquals("value", tool.execute(new AskDataToolRequest(context, "tool", Collections.singletonMap("key", "value"))).getContent());
	}

	@Test
	void 元数据和权限SPI必须基于同一隔离上下文决策() {
		AskDataProjectContext context = new AskDataProjectContext("project", "operator");
		AskDataMetadataProvider provider = new AskDataMetadataProvider() {
			@Override
			public AskDataMetadata load(AskDataProjectContext actualContext, String datasourceKey) {
				return new AskDataMetadata(datasourceKey, Arrays.asList("orders"));
			}
		};
		AskDataPermissionEvaluator permissions = new AskDataPermissionEvaluator() {
			@Override
			public boolean isAllowed(AskDataProjectContext actualContext, String capability, String resource) {
				return context == actualContext && "metadata:read".equals(capability) && "orders".equals(resource);
			}
		};

		assertEquals("orders", provider.load(context, "ds").getResources().get(0));
		assertTrue(permissions.isAllowed(context, "metadata:read", "orders"));
		assertFalse(permissions.isAllowed(context, "tool:execute", "orders"));
	}
}
