package com.xiongx9527.fw.plus.ai.tinyflow.knowledge;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class TinyflowKnowledgeRepositoryContractTest {

	private TinyflowKnowledgeRepository repository;

	@Before
	public void setUp() {
		repository = new InMemoryTinyflowKnowledgeRepository();
	}

	@Test
	public void shouldOnlyExposeActiveDocumentVersionAfterAtomicSwitch() {
		repository.saveDocument(document("tenant-a", "kb-1", "source-1", "doc-v1", "sum-1"));
		repository.saveDocument(document("tenant-a", "kb-1", "source-1", "doc-v2", "sum-2"));

		repository.activateDocumentVersion("tenant-a", "kb-1", "source-1", "doc-v2");

		assertEquals("doc-v2", repository.findActiveDocument("tenant-a", "kb-1", "source-1").getDocumentId());
		assertNull(repository.findActiveDocument("tenant-b", "kb-1", "source-1"));
		assertEquals("doc-v1", repository.findBySourceAndChecksum("tenant-a", "kb-1", "source-1", "sum-1").getDocumentId());
	}

	@Test
	public void shouldRejectStaleKnowledgeDefinitionVersion() {
		TinyflowKnowledgeDefinition definition = knowledge("tenant-a", "kb-1");
		repository.saveKnowledge(definition, 0L);

		assertEquals(1L, repository.findKnowledgeVersion("tenant-a", "kb-1"));
		assertThrows(TinyflowKnowledgeRepository.OptimisticLockException.class,
				() -> repository.saveKnowledge(definition, 0L));
	}

	@Test
	public void shouldDeleteOnlyRequestedSourceVersionsWithinTenantAndKnowledge() {
		repository.saveDocument(document("tenant-a", "kb-1", "source-1", "doc-a1", "sum-a1"));
		repository.saveDocument(document("tenant-a", "kb-1", "source-1", "doc-a2", "sum-a2"));
		repository.saveDocument(document("tenant-a", "kb-1", "source-2", "doc-a3", "sum-a3"));
		repository.saveDocument(document("tenant-b", "kb-1", "source-1", "doc-b1", "sum-b1"));

		assertEquals(2, repository.findDocumentVersions("tenant-a", "kb-1", "source-1").size());
		repository.deleteDocumentsBySource("tenant-a", "kb-1", "source-1");

		assertTrue(repository.findDocumentVersions("tenant-a", "kb-1", "source-1").isEmpty());
		assertEquals(1, repository.findDocumentVersions("tenant-a", "kb-1", "source-2").size());
		assertEquals(1, repository.findDocumentVersions("tenant-b", "kb-1", "source-1").size());
	}

	@Test
	public void shouldPersistAndResolveVectorIdsWithinTenantAndKnowledge() {
		repository.saveChunks(Arrays.asList(
				chunk("tenant-a", "kb-1", "doc-1", "chunk-1", "old", true, "vec-1", "vec-2"),
				chunk("tenant-b", "kb-1", "doc-2", "chunk-2", "other", true, "vec-1")));

		assertEquals(Arrays.asList("vec-1", "vec-2"), repository.findChunk("tenant-a", "chunk-1").getVectorIds());
		List<TinyflowChunk> matches = repository.findChunksByVectorIds(
				"tenant-a", "kb-1", Arrays.asList("vec-2", "vec-1"));
		assertEquals(1, matches.size());
		assertEquals("chunk-1", matches.get(0).getChunkId());
	}

	@Test
	public void shouldUpdateChunkWithoutLosingSourceIdentity() {
		repository.saveChunks(Collections.singletonList(
				chunk("tenant-a", "kb-1", "doc-1", "chunk-1", "old", true, "vec-1")));
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		metadata.put("page", 2);

		repository.updateChunk("tenant-a", "kb-1", "chunk-1", false, "new", metadata);

		TinyflowChunk updated = repository.findChunk("tenant-a", "chunk-1");
		assertEquals("tenant-a", updated.getTenantId());
		assertEquals("kb-1", updated.getKnowledgeId());
		assertEquals("source-1", updated.getSourceId());
		assertEquals("file", updated.getSourceType());
		assertEquals("doc-1", updated.getDocumentId());
		assertEquals("new", updated.getContent());
		assertFalse(updated.isEnabled());
		assertEquals(2, updated.getMetadata().get("page"));
	}

	@Test
	public void shouldDefensivelyPersistVectorCompensationRecords() {
		List<String> vectorIds = new ArrayList<String>(Arrays.asList("vec-1", "vec-2"));
		TinyflowKnowledgeRepository.VectorCompensation compensation =
				new TinyflowKnowledgeRepository.VectorCompensation(
						"comp-1", "tenant-a", "kb-1", "source-1", "doc-1", vectorIds, "delete");

		repository.saveVectorCompensation(compensation);
		vectorIds.add("vec-3");

		List<TinyflowKnowledgeRepository.VectorCompensation> records =
				repository.findVectorCompensations("tenant-a", "kb-1");
		assertEquals(Arrays.asList("vec-1", "vec-2"), records.get(0).getVectorIds());
		assertThrows(UnsupportedOperationException.class, () -> records.get(0).getVectorIds().add("vec-4"));
		assertTrue(repository.findVectorCompensations("tenant-b", "kb-1").isEmpty());
	}

	@Test
	public void shouldListTasksWithinTenantAndKnowledge() {
		repository.saveTask(task("task-a", "tenant-a", "kb-1"));
		repository.saveTask(task("task-b", "tenant-b", "kb-1"));

		assertEquals(1, repository.findTasks("tenant-a", "kb-1").size());
		assertEquals("task-a", repository.findTasks("tenant-a", "kb-1").get(0).getTaskId());
	}

	private TinyflowKnowledgeDefinition knowledge(String tenantId, String knowledgeId) {
		return TinyflowKnowledgeDefinition.builder()
				.providerKey("local")
				.tenantId(tenantId)
				.knowledgeId(knowledgeId)
				.name("Knowledge")
				.build();
	}

	private TinyflowDocument document(String tenantId, String knowledgeId, String sourceId,
			String documentId, String checksum) {
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		metadata.put("checksum", checksum);
		return TinyflowDocument.builder()
				.tenantId(tenantId)
				.knowledgeId(knowledgeId)
				.sourceId(sourceId)
				.sourceType("file")
				.documentId(documentId)
				.name(documentId)
				.metadata(metadata)
				.build();
	}

	private TinyflowChunk chunk(String tenantId, String knowledgeId, String documentId,
			String chunkId, String content, boolean enabled, String... vectorIds) {
		return TinyflowChunk.builder()
				.tenantId(tenantId)
				.knowledgeId(knowledgeId)
				.sourceId("source-1")
				.sourceType("file")
				.documentId(documentId)
				.chunkId(chunkId)
				.content(content)
				.enabled(enabled)
				.vectorIds(Arrays.asList(vectorIds))
				.build();
	}

	private TinyflowIngestionTask task(String taskId, String tenantId, String knowledgeId) {
		return TinyflowIngestionTask.builder()
				.taskId(taskId)
				.providerKey("local")
				.tenantId(tenantId)
				.knowledgeId(knowledgeId)
				.documentId("doc-1")
				.status("pending")
				.build();
	}

	private static final class InMemoryTinyflowKnowledgeRepository implements TinyflowKnowledgeRepository {

		private final Map<String, TinyflowKnowledgeDefinition> knowledge = new LinkedHashMap<String, TinyflowKnowledgeDefinition>();
		private final Map<String, Long> versions = new LinkedHashMap<String, Long>();
		private final Map<String, TinyflowDocument> documents = new LinkedHashMap<String, TinyflowDocument>();
		private final Map<String, String> activeDocuments = new LinkedHashMap<String, String>();
		private final Map<String, TinyflowChunk> chunks = new LinkedHashMap<String, TinyflowChunk>();
		private final List<TinyflowIngestionTask> tasks = new ArrayList<TinyflowIngestionTask>();
		private final List<VectorCompensation> compensations = new ArrayList<VectorCompensation>();

		@Override
		public TinyflowKnowledgeDefinition findKnowledge(String tenantId, String knowledgeId) {
			return knowledge.get(key(tenantId, knowledgeId));
		}

		@Override
		public long findKnowledgeVersion(String tenantId, String knowledgeId) {
			Long version = versions.get(key(tenantId, knowledgeId));
			return version == null ? 0L : version;
		}

		@Override
		public void saveKnowledge(TinyflowKnowledgeDefinition definition, long expectedVersion) {
			String key = key(definition.getTenantId(), definition.getKnowledgeId());
			long actualVersion = findKnowledgeVersion(definition.getTenantId(), definition.getKnowledgeId());
			if (actualVersion != expectedVersion) {
				throw new OptimisticLockException(expectedVersion, actualVersion);
			}
			knowledge.put(key, definition);
			versions.put(key, actualVersion + 1L);
		}

		@Override
		public TinyflowDocument findBySourceAndChecksum(String tenantId, String knowledgeId,
				String sourceId, String checksum) {
			for (TinyflowDocument document : findDocumentVersions(tenantId, knowledgeId, sourceId)) {
				if (checksum.equals(document.getMetadata().get("checksum"))) {
					return document;
				}
			}
			return null;
		}

		@Override
		public TinyflowDocument findActiveDocument(String tenantId, String knowledgeId, String sourceId) {
			String documentId = activeDocuments.get(sourceKey(tenantId, knowledgeId, sourceId));
			return documentId == null ? null : documents.get(documentKey(tenantId, knowledgeId, documentId));
		}

		@Override
		public void saveDocument(TinyflowDocument document) {
			documents.put(documentKey(document.getTenantId(), document.getKnowledgeId(), document.getDocumentId()), document);
		}

		@Override
		public List<TinyflowDocument> findDocumentVersions(String tenantId, String knowledgeId, String sourceId) {
			List<TinyflowDocument> result = new ArrayList<TinyflowDocument>();
			for (TinyflowDocument document : documents.values()) {
				if (tenantId.equals(document.getTenantId())
						&& knowledgeId.equals(document.getKnowledgeId())
						&& sourceId.equals(document.getSourceId())) {
					result.add(document);
				}
			}
			return result;
		}

		@Override
		public void activateDocumentVersion(String tenantId, String knowledgeId, String sourceId, String documentId) {
			TinyflowDocument document = documents.get(documentKey(tenantId, knowledgeId, documentId));
			if (document == null || !sourceId.equals(document.getSourceId())) {
				throw new IllegalArgumentException("Document does not belong to source");
			}
			activeDocuments.put(sourceKey(tenantId, knowledgeId, sourceId), documentId);
		}

		@Override
		public void deleteDocumentsBySource(String tenantId, String knowledgeId, String sourceId) {
			for (TinyflowDocument document : new ArrayList<TinyflowDocument>(documents.values())) {
				if (tenantId.equals(document.getTenantId())
						&& knowledgeId.equals(document.getKnowledgeId())
						&& sourceId.equals(document.getSourceId())) {
					documents.remove(documentKey(tenantId, knowledgeId, document.getDocumentId()));
				}
			}
			activeDocuments.remove(sourceKey(tenantId, knowledgeId, sourceId));
		}

		@Override
		public void saveChunks(List<TinyflowChunk> values) {
			for (TinyflowChunk chunk : values) {
				chunks.put(chunkKey(chunk.getTenantId(), chunk.getKnowledgeId(), chunk.getChunkId()), chunk);
			}
		}

		@Override
		public TinyflowChunk findChunk(String tenantId, String chunkId) {
			for (Map.Entry<String, TinyflowChunk> entry : chunks.entrySet()) {
				if (entry.getKey().startsWith(tenantId + "|") && chunkId.equals(entry.getValue().getChunkId())) {
					return entry.getValue();
				}
			}
			return null;
		}

		@Override
		public List<TinyflowChunk> findChunksByDocument(String tenantId, String documentId) {
			List<TinyflowChunk> result = new ArrayList<TinyflowChunk>();
			for (TinyflowChunk chunk : chunks.values()) {
				if (tenantId.equals(chunk.getTenantId()) && documentId.equals(chunk.getDocumentId())) {
					result.add(chunk);
				}
			}
			return result;
		}

		@Override
		public List<TinyflowChunk> findChunksByVectorIds(String tenantId, String knowledgeId, List<String> vectorIds) {
			List<TinyflowChunk> result = new ArrayList<TinyflowChunk>();
			for (TinyflowChunk chunk : chunks.values()) {
				if (tenantId.equals(chunk.getTenantId())
						&& knowledgeId.equals(chunk.getKnowledgeId())
						&& !Collections.disjoint(chunk.getVectorIds(), vectorIds)) {
					result.add(chunk);
				}
			}
			return result;
		}

		@Override
		public void updateChunk(String tenantId, String knowledgeId, String chunkId,
				boolean enabled, String content, Map<String, Object> metadata) {
			String key = chunkKey(tenantId, knowledgeId, chunkId);
			TinyflowChunk original = chunks.get(key);
			if (original == null) {
				throw new IllegalArgumentException("Chunk not found");
			}
			Map<String, Object> scopedMetadata = new LinkedHashMap<String, Object>(metadata);
			scopedMetadata.put("tenantId", tenantId);
			scopedMetadata.put("knowledgeId", knowledgeId);
			TinyflowChunk updated = TinyflowChunk.builder()
					.tenantId(original.getTenantId())
					.knowledgeId(original.getKnowledgeId())
					.sourceId(original.getSourceId())
					.sourceType(original.getSourceType())
					.documentId(original.getDocumentId())
					.chunkId(original.getChunkId())
					.content(content)
					.sourceUri(original.getSourceUri())
					.sourceVersion(original.getSourceVersion())
					.location(original.getLocation())
					.position(original.getPosition())
					.parentChunkId(original.getParentChunkId())
					.tokenCount(original.getTokenCount())
					.metadata(scopedMetadata)
					.enabled(enabled)
					.vectorIds(original.getVectorIds())
					.build();
			chunks.put(key, updated);
		}

		@Override
		public void saveTask(TinyflowIngestionTask task) {
			tasks.add(task);
		}

		@Override
		public List<TinyflowIngestionTask> findTasks(String tenantId, String knowledgeId) {
			List<TinyflowIngestionTask> result = new ArrayList<TinyflowIngestionTask>();
			for (TinyflowIngestionTask task : tasks) {
				if (tenantId.equals(task.getTenantId()) && knowledgeId.equals(task.getKnowledgeId())) {
					result.add(task);
				}
			}
			return result;
		}

		@Override
		public void saveVectorCompensation(VectorCompensation compensation) {
			compensations.add(compensation);
		}

		@Override
		public List<VectorCompensation> findVectorCompensations(String tenantId, String knowledgeId) {
			List<VectorCompensation> result = new ArrayList<VectorCompensation>();
			for (VectorCompensation compensation : compensations) {
				if (tenantId.equals(compensation.getTenantId())
						&& knowledgeId.equals(compensation.getKnowledgeId())) {
					result.add(compensation);
				}
			}
			return result;
		}

		@Override
		public void deleteVectorCompensation(String tenantId, String knowledgeId, String compensationId) {
			for (VectorCompensation compensation : new ArrayList<VectorCompensation>(compensations)) {
				if (tenantId.equals(compensation.getTenantId())
						&& knowledgeId.equals(compensation.getKnowledgeId())
						&& compensationId.equals(compensation.getCompensationId())) {
					compensations.remove(compensation);
				}
			}
		}

		private static String key(String tenantId, String knowledgeId) {
			return tenantId + "|" + knowledgeId;
		}

		private static String sourceKey(String tenantId, String knowledgeId, String sourceId) {
			return key(tenantId, knowledgeId) + "|" + sourceId;
		}

		private static String documentKey(String tenantId, String knowledgeId, String documentId) {
			return key(tenantId, knowledgeId) + "|" + documentId;
		}

		private static String chunkKey(String tenantId, String knowledgeId, String chunkId) {
			return key(tenantId, knowledgeId) + "|" + chunkId;
		}

	}
}
