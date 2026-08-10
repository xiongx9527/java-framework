package com.xiongx9527.fw.plus.ai.tinyflow.local;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowChunk;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowChunkStrategy;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowParsedDocument;
import com.xiongx9527.fw.plus.ai.tinyflow.local.chunk.NormalChunkStrategy;
import com.xiongx9527.fw.plus.ai.tinyflow.local.chunk.ParentChildChunkStrategy;
import com.xiongx9527.fw.plus.ai.tinyflow.local.chunk.QaChunkStrategy;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TinyflowChunkStrategyTest {

	@Test
	public void shouldCreateNormalChunksWithUniqueIdsAndExplicitIdentity() {
		Map<String, Object> options = new HashMap<String, Object>();
		options.put("chunkSize", 8);
		options.put("overlap", 2);
		options.put("sourceUri", "object://manual.txt");
		options.put("sourceVersion", "sha256-a");
		List<TinyflowChunk> chunks = new NormalChunkStrategy().chunk(parsed("abcdefghijklmno"), options(options));

		assertTrue(chunks.size() > 1);
		assertEquals(chunks.size(), new HashSet<String>(ids(chunks)).size());
		for (TinyflowChunk chunk : chunks) {
			assertIdentity(chunk);
			assertEquals("text-markdown-html", chunk.getMetadata().get("parserKey"));
			assertEquals("line:1", chunk.getLocation());
			assertEquals("object://manual.txt", chunk.getSourceUri());
			assertEquals("sha256-a", chunk.getSourceVersion());
		}
	}

	@Test
	public void shouldCreateQuestionAnswerChunks() {
		TinyflowParsedDocument parsed = new TinyflowParsedDocument("FAQ",
				"Q: What is Tinyflow?\nA: A workflow engine.\nQ: Java version?\nA: Java 8.",
				Collections.<TinyflowParsedDocument.Section>emptyList(),
				Collections.<String, Object>singletonMap("parserKey", "text-markdown-html"));

		List<TinyflowChunk> chunks = new QaChunkStrategy().chunk(parsed, options(Collections.<String, Object>emptyMap()));

		assertEquals(2, chunks.size());
		assertEquals("What is Tinyflow?", chunks.get(0).getMetadata().get("question"));
		assertEquals("A workflow engine.", chunks.get(0).getMetadata().get("answer"));
		assertTrue(chunks.get(0).getContent().contains("Q: What is Tinyflow?"));
		assertIdentity(chunks.get(0));
	}

	@Test
	public void shouldCreateParentAndChildChunksWithParentReferences() {
		Map<String, Object> options = new HashMap<String, Object>();
		options.put("parentSize", 12);
		options.put("childSize", 5);
		List<TinyflowChunk> chunks = new ParentChildChunkStrategy().chunk(
				parsed("abcdefghijklmnopqrstuvwxyz"), options(options));

		assertEquals(chunks.size(), new HashSet<String>(ids(chunks)).size());
		int parents = 0;
		int children = 0;
		for (TinyflowChunk chunk : chunks) {
			assertIdentity(chunk);
			if ("parent".equals(chunk.getMetadata().get("chunkRole"))) {
				parents++;
				assertEquals(null, chunk.getParentChunkId());
			} else {
				children++;
				assertNotNull(chunk.getParentChunkId());
				assertTrue(ids(chunks).contains(chunk.getParentChunkId()));
			}
		}
		assertTrue(parents > 1);
		assertTrue(children > parents);
	}

	private TinyflowParsedDocument parsed(String content) {
		Map<String, Object> parserMetadata = new HashMap<String, Object>();
		parserMetadata.put("parserKey", "text-markdown-html");
		parserMetadata.put("parserVersion", "1");
		Map<String, Object> sectionMetadata = new HashMap<String, Object>();
		sectionMetadata.put("line", 1);
		TinyflowParsedDocument.Section section = new TinyflowParsedDocument.Section(
				content, "line:1", sectionMetadata);
		return new TinyflowParsedDocument("Fixture", content, Arrays.asList(section), parserMetadata);
	}

	private TinyflowChunkStrategy.ChunkOptions options(Map<String, Object> values) {
		return new TinyflowChunkStrategy.ChunkOptions("tenant-a", "kb-1", "source-1", "file",
				"doc-1", values);
	}

	private void assertIdentity(TinyflowChunk chunk) {
		assertEquals("tenant-a", chunk.getTenantId());
		assertEquals("kb-1", chunk.getKnowledgeId());
		assertEquals("source-1", chunk.getSourceId());
		assertEquals("file", chunk.getSourceType());
		assertEquals("doc-1", chunk.getDocumentId());
		assertFalse(chunk.getChunkId().trim().isEmpty());
	}

	private List<String> ids(List<TinyflowChunk> chunks) {
		java.util.ArrayList<String> values = new java.util.ArrayList<String>();
		for (TinyflowChunk chunk : chunks) {
			values.add(chunk.getChunkId());
		}
		return values;
	}
}
