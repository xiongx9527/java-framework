package com.xiongx9527.fw.plus.ai.tinyflow.local.index;

import com.xiongx9527.fw.plus.ai.tinyflow.TinyflowException;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowChunk;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowIngestionTask;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeDefinition;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class TinyflowKnowledgeMaintenanceServiceTest {

	@Test
	public void shouldKeepOldVersionSearchableUntilNewIndexCompletes() {
		TinyflowLocalIndexerTest.MemoryRepository repository = new TinyflowLocalIndexerTest.MemoryRepository();
		TinyflowLocalIndexerTest.RecordingStore store = new TinyflowLocalIndexerTest.RecordingStore();
		TinyflowLocalIndexer indexer = TinyflowLocalIndexerTest.indexer(repository, store, true, true, 1);
		TinyflowKnowledgeMaintenanceService maintenance = new TinyflowKnowledgeMaintenanceService(repository, indexer,
				new TinyflowLocalIndexerTest.FixedStoreProvider(store, true));
		seedActive(repository);
		store.failOnDocumentCall = 2;

		assertThrows(TinyflowException.class, () -> maintenance.rebuild("tenant-a", "kb-1", "source-1"));

		assertEquals("doc-v1", repository.findActiveDocument("tenant-a", "kb-1", "source-1").getDocumentId());
		assertFalse(repository.pendingVectorIds("tenant-a", "kb-1").isEmpty());
	}

	@Test
	public void shouldCompensateNewVectorsWhenAtomicActivationFails() {
		TinyflowLocalIndexerTest.MemoryRepository repository = new TinyflowLocalIndexerTest.MemoryRepository();
		TinyflowLocalIndexerTest.RecordingStore store = new TinyflowLocalIndexerTest.RecordingStore();
		TinyflowLocalIndexer indexer = TinyflowLocalIndexerTest.indexer(repository, store, true, true, 10);
		TinyflowKnowledgeMaintenanceService maintenance = new TinyflowKnowledgeMaintenanceService(repository, indexer,
				new TinyflowLocalIndexerTest.FixedStoreProvider(store, true));
		seedActive(repository);
		repository.failActivation = true;

		assertThrows(TinyflowException.class, () -> maintenance.rebuild("tenant-a", "kb-1", "source-1"));

		assertEquals("doc-v1", repository.findActiveDocument("tenant-a", "kb-1", "source-1").getDocumentId());
		assertFalse(repository.pendingVectorIds("tenant-a", "kb-1").isEmpty());
	}

	@Test
	public void shouldRemoveEveryOldVersionAfterSuccessfulRebuild() {
		TinyflowLocalIndexerTest.MemoryRepository repository = new TinyflowLocalIndexerTest.MemoryRepository();
		TinyflowLocalIndexerTest.RecordingStore store = new TinyflowLocalIndexerTest.RecordingStore();
		TinyflowLocalIndexer indexer = TinyflowLocalIndexerTest.indexer(repository, store, true, true, 10);
		TinyflowKnowledgeMaintenanceService maintenance = new TinyflowKnowledgeMaintenanceService(repository, indexer,
				new TinyflowLocalIndexerTest.FixedStoreProvider(store, true));
		seedActive(repository);
		repository.saveDocument(TinyflowLocalIndexerTest.document("doc-v0"));
		repository.saveChunks(Collections.singletonList(oldVersionChunk()));

		maintenance.rebuild("tenant-a", "kb-1", "source-1");

		String activeDocumentId = repository.findActiveDocument(
				"tenant-a", "kb-1", "source-1").getDocumentId();
		assertFalse("doc-v1".equals(activeDocumentId));
		assertEquals(1, repository.findDocumentVersions("tenant-a", "kb-1", "source-1").size());
		assertTrue(repository.findChunksByDocument("tenant-a", "kb-1", "doc-v1").isEmpty());
		assertTrue(repository.findChunksByDocument("tenant-a", "kb-1", "doc-v0").isEmpty());
		assertTrue(store.removed.containsAll(Arrays.asList("vec-old-1", "vec-old-2", "vec-old-v0")));
	}

	@Test
	public void shouldPersistOldVersionCleanupAndConvergeAfterRetry() {
		TinyflowLocalIndexerTest.MemoryRepository repository = new TinyflowLocalIndexerTest.MemoryRepository();
		TinyflowLocalIndexerTest.RecordingStore store = new TinyflowLocalIndexerTest.RecordingStore();
		TinyflowLocalIndexer indexer = TinyflowLocalIndexerTest.indexer(repository, store, true, true, 10);
		TinyflowKnowledgeMaintenanceService maintenance = new TinyflowKnowledgeMaintenanceService(repository, indexer,
				new TinyflowLocalIndexerTest.FixedStoreProvider(store, true));
		seedActive(repository);
		store.failRemove = true;

		maintenance.rebuild("tenant-a", "kb-1", "source-1");

		assertFalse("doc-v1".equals(repository.findActiveDocument(
				"tenant-a", "kb-1", "source-1").getDocumentId()));
		assertEquals("DELETE_OLD_VERSION", repository.findVectorCompensations(
				"tenant-a", "kb-1").get(0).getOperation());
		assertFalse(repository.findChunksByDocument("tenant-a", "kb-1", "doc-v1").isEmpty());

		store.failRemove = false;
		new TinyflowVectorCompensator(repository, indexer).retry("tenant-a", "kb-1");

		assertTrue(repository.findChunksByDocument("tenant-a", "kb-1", "doc-v1").isEmpty());
		assertTrue(repository.findVectorCompensations("tenant-a", "kb-1").isEmpty());
	}

	@Test
	public void shouldFinishDeletePendingResourcesAndTaskAfterRetry() {
		TinyflowLocalIndexerTest.MemoryRepository repository = new TinyflowLocalIndexerTest.MemoryRepository();
		TinyflowLocalIndexerTest.RecordingStore store = new TinyflowLocalIndexerTest.RecordingStore();
		TinyflowLocalIndexer indexer = TinyflowLocalIndexerTest.indexer(repository, store, true, true, 10);
		TinyflowKnowledgeMaintenanceService maintenance = new TinyflowKnowledgeMaintenanceService(repository, indexer,
				new TinyflowLocalIndexerTest.FixedStoreProvider(store, true));
		seedActive(repository);
		store.failRemove = true;
		assertThrows(TinyflowException.class,
				() -> maintenance.deleteSource("tenant-a", "kb-1", "source-1"));

		store.failRemove = false;
		new TinyflowVectorCompensator(repository, indexer).retry("tenant-a", "kb-1");

		assertTrue(repository.findDocumentVersions("tenant-a", "kb-1", "source-1").isEmpty());
		assertEquals("DELETED", lastTask(repository).getStatus());
		assertTrue(repository.findVectorCompensations("tenant-a", "kb-1").isEmpty());
	}

	@Test
	public void shouldRejectVectorStoreReuseWithoutKnowledgeFilterCapability() {
		TinyflowLocalIndexerTest.MemoryRepository repository = new TinyflowLocalIndexerTest.MemoryRepository();
		TinyflowLocalIndexerTest.RecordingStore store = new TinyflowLocalIndexerTest.RecordingStore();
		TinyflowKnowledgeMaintenanceService maintenance = new TinyflowKnowledgeMaintenanceService(repository,
				TinyflowLocalIndexerTest.indexer(repository, store, true, true, 10),
				new TinyflowLocalIndexerTest.FixedStoreProvider(store, true));
		repository.saveKnowledge(knowledge("kb-1", "store-a"), 0L);

		assertThrows(TinyflowException.class, () -> maintenance.create(knowledge("kb-2", "store-a")));
	}

	@Test
	public void shouldLeaveSourceActiveAndTrackDeletePendingWhenVectorRemovalFails() {
		TinyflowLocalIndexerTest.MemoryRepository repository = new TinyflowLocalIndexerTest.MemoryRepository();
		TinyflowLocalIndexerTest.RecordingStore store = new TinyflowLocalIndexerTest.RecordingStore();
		TinyflowKnowledgeMaintenanceService maintenance = new TinyflowKnowledgeMaintenanceService(repository,
				TinyflowLocalIndexerTest.indexer(repository, store, true, true, 10),
				new TinyflowLocalIndexerTest.FixedStoreProvider(store, true));
		seedActive(repository);
		store.failRemove = true;

		assertThrows(TinyflowException.class, () -> maintenance.deleteSource("tenant-a", "kb-1", "source-1"));

		assertEquals("doc-v1", repository.findActiveDocument("tenant-a", "kb-1", "source-1").getDocumentId());
		assertFalse(repository.pendingVectorIds("tenant-a", "kb-1").isEmpty());
		assertEquals("DELETE_PENDING", lastTask(repository).getStatus());
	}

	@Test
	public void shouldRemoveVectorsForEveryDocumentVersionBeforeDeletingSource() {
		TinyflowLocalIndexerTest.MemoryRepository repository = new TinyflowLocalIndexerTest.MemoryRepository();
		TinyflowLocalIndexerTest.RecordingStore store = new TinyflowLocalIndexerTest.RecordingStore();
		TinyflowKnowledgeMaintenanceService maintenance = new TinyflowKnowledgeMaintenanceService(repository,
				TinyflowLocalIndexerTest.indexer(repository, store, true, true, 10),
				new TinyflowLocalIndexerTest.FixedStoreProvider(store, true));
		seedActive(repository);
		TinyflowChunk oldChunk = withVector(TinyflowLocalIndexerTest.chunks("doc-v0", 1).get(0), "vec-old-v0");
		repository.saveDocument(TinyflowLocalIndexerTest.document("doc-v0"));
		repository.saveChunks(Collections.singletonList(oldChunk));

		maintenance.deleteSource("tenant-a", "kb-1", "source-1");

		assertTrue(store.removed.contains("vec-old-v0"));
		assertTrue(repository.findDocumentVersions("tenant-a", "kb-1", "source-1").isEmpty());
		assertEquals("DELETED", lastTask(repository).getStatus());
	}

	private static void seedActive(TinyflowLocalIndexerTest.MemoryRepository repository) {
		repository.saveKnowledge(knowledge("kb-1", "store-a"), 0L);
		List<TinyflowChunk> chunks = TinyflowLocalIndexerTest.chunks("doc-v1", 2);
		List<TinyflowChunk> indexed = Arrays.asList(withVector(chunks.get(0), "vec-old-1"), withVector(chunks.get(1), "vec-old-2"));
		repository.saveDocument(TinyflowLocalIndexerTest.document("doc-v1"));
		repository.saveChunks(indexed);
		repository.activateDocumentVersion("tenant-a", "kb-1", "source-1", "doc-v1");
	}

	private static TinyflowChunk withVector(TinyflowChunk chunk, String vectorId) {
		return TinyflowChunk.builder().tenantId(chunk.getTenantId()).knowledgeId(chunk.getKnowledgeId())
				.sourceId(chunk.getSourceId()).sourceType(chunk.getSourceType()).documentId(chunk.getDocumentId())
				.chunkId(chunk.getChunkId()).content(chunk.getContent()).metadata(chunk.getMetadata())
				.vectorIds(Collections.singletonList(vectorId)).build();
	}

	private static TinyflowChunk oldVersionChunk() {
		return TinyflowChunk.builder().tenantId("tenant-a").knowledgeId("kb-1")
				.sourceId("source-1").sourceType("file").documentId("doc-v0")
				.chunkId("chunk-v0").content("old version")
				.vectorIds(Collections.singletonList("vec-old-v0")).build();
	}

	private static TinyflowKnowledgeDefinition knowledge(String knowledgeId, String storeKey) {
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		metadata.put("vectorStoreKey", storeKey);
		return TinyflowKnowledgeDefinition.builder().providerKey("fw-plus-local").tenantId("tenant-a")
				.knowledgeId(knowledgeId).name(knowledgeId).metadata(metadata).build();
	}

	private static TinyflowIngestionTask lastTask(TinyflowLocalIndexerTest.MemoryRepository repository) {
		List<TinyflowIngestionTask> tasks = repository.findTasks("tenant-a", "kb-1");
		return tasks.get(tasks.size() - 1);
	}
}
