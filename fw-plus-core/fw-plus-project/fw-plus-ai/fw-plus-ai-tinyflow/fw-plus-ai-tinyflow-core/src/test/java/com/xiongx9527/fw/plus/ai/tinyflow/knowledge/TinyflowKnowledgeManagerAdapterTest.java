package com.xiongx9527.fw.plus.ai.tinyflow.knowledge;

import com.xiongx9527.fw.plus.ai.tinyflow.TinyflowException;
import dev.tinyflow.core.knowledge.Knowledge;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class TinyflowKnowledgeManagerAdapterTest {

	@Test
	public void shouldRouteProviderAndKnowledgeIdAndPreserveSourceIdentity() {
		AtomicReference<TinyflowKnowledgeRequest> captured = new AtomicReference<TinyflowKnowledgeRequest>();
		TinyflowKnowledgeProvider provider = provider("dify", captured);
		TinyflowKnowledgeManagerAdapter adapter = new TinyflowKnowledgeManagerAdapter(
				new TinyflowKnowledgeProviderRegistry(Collections.singletonList(provider)));
		Map<String, Object> reference = new LinkedHashMap<String, Object>();
		reference.put("provider", "dify");
		reference.put("tenantId", "tenant-1");
		reference.put("knowledgeId", "knowledge-1");

		Knowledge knowledge = adapter.getKnowledge(reference);
		List<Map<String, Object>> documents = knowledge.search("question", 3, null, null);

		assertEquals("dify", captured.get().getProviderKey());
		assertEquals("tenant-1", captured.get().getTenantId());
		assertEquals("knowledge-1", captured.get().getKnowledgeId());
		assertEquals("question", captured.get().getQuery());
		assertEquals(3, captured.get().getTopK());
		assertEquals("source-1", documents.get(0).get("sourceId"));
		assertEquals("file", documents.get(0).get("sourceType"));
		assertEquals("document-1", documents.get(0).get("documentId"));
		assertEquals("chunk-1", documents.get(0).get("chunkId"));
		assertEquals("answer", documents.get(0).get("content"));
	}

	@Test
	public void shouldThrowWhenKnowledgeProviderIsMissing() {
		TinyflowKnowledgeManagerAdapter adapter = new TinyflowKnowledgeManagerAdapter(
				new TinyflowKnowledgeProviderRegistry(Collections.<TinyflowKnowledgeProvider>emptyList()));
		Map<String, Object> reference = new LinkedHashMap<String, Object>();
		reference.put("provider", "missing");
		reference.put("tenantId", "tenant-1");
		reference.put("knowledgeId", "knowledge-1");

		assertThrows(TinyflowException.class, () -> adapter.getKnowledge(reference));
	}

	private TinyflowKnowledgeProvider provider(final String key,
			AtomicReference<TinyflowKnowledgeRequest> captured) {
		return new TinyflowKnowledgeProvider() {
			@Override
			public String getKey() {
				return key;
			}

			@Override
			public TinyflowKnowledgeCapabilities capabilities() {
				return TinyflowKnowledgeCapabilities.retrievalOnly();
			}

			@Override
			public TinyflowKnowledgeResult retrieve(TinyflowKnowledgeRequest request) {
				captured.set(request);
				TinyflowKnowledgeMatch match = TinyflowKnowledgeMatch.builder()
						.sourceId("source-1")
						.sourceType("file")
						.documentId("document-1")
						.chunkId("chunk-1")
						.content("answer")
						.score(0.9D)
						.build();
				return TinyflowKnowledgeResult.builder().matches(Collections.singletonList(match)).build();
			}
		};
	}
}
