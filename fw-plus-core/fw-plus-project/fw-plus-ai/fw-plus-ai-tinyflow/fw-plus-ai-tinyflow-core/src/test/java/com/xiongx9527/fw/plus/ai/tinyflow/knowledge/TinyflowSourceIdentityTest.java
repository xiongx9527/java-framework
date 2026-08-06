package com.xiongx9527.fw.plus.ai.tinyflow.knowledge;

import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class TinyflowSourceIdentityTest {

	@Test
	public void shouldRequireAllChunkSourceIdentifiers() {
		assertThrows(IllegalArgumentException.class, () -> TinyflowChunk.builder()
				.sourceId("source-1").sourceType("file").documentId("doc-1").content("text").build());
	}

	@Test
	public void shouldAcceptBuiltInAndExtensionSourceTypes() {
		assertEquals("file", chunk("file").getSourceType());
		assertEquals("custom-connector", chunk("custom-connector").getSourceType());
		assertThrows(IllegalArgumentException.class, () -> chunk(" "));
	}

	@Test
	public void shouldDefensivelyCopyMetadataAndVectorIds() {
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		metadata.put("page", 1);
		List<String> vectorIds = new ArrayList<String>();
		vectorIds.add("vector-1");
		TinyflowChunk chunk = TinyflowChunk.builder()
				.sourceId("source-1")
				.sourceType("url")
				.documentId("doc-1")
				.chunkId("chunk-1")
				.content("text")
				.metadata(metadata)
				.vectorIds(vectorIds)
				.build();

		metadata.put("page", 2);
		vectorIds.add("vector-2");

		assertEquals(1, chunk.getMetadata().get("page"));
		assertEquals(1, chunk.getVectorIds().size());
		assertThrows(UnsupportedOperationException.class, () -> chunk.getMetadata().put("page", 3));
		assertThrows(UnsupportedOperationException.class, () -> chunk.getVectorIds().add("vector-3"));
	}

	@Test
	public void shouldValidateKnowledgeRequestBoundaries() {
		TinyflowKnowledgeRequest.Builder builder = TinyflowKnowledgeRequest.builder()
				.providerKey("dify")
				.tenantId("tenant-1")
				.knowledgeId("knowledge-1")
				.query("question");

		assertThrows(IllegalArgumentException.class, () -> builder.topK(0).build());
		assertThrows(IllegalArgumentException.class, () -> builder.topK(1).threshold(-0.01D).build());
		assertThrows(IllegalArgumentException.class, () -> builder.threshold(1.01D).build());
	}

	private TinyflowChunk chunk(String sourceType) {
		return TinyflowChunk.builder()
				.sourceId("source-1")
				.sourceType(sourceType)
				.documentId("doc-1")
				.chunkId("chunk-1")
				.content("text")
				.build();
	}
}
