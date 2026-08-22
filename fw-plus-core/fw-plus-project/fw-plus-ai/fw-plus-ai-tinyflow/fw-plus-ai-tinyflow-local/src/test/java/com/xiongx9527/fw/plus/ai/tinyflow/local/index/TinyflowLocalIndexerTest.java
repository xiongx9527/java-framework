package com.xiongx9527.fw.plus.ai.tinyflow.local.index;

import com.xiongx9527.fw.plus.ai.tinyflow.TinyflowException;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowChunk;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDocument;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowIngestionTask;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeDefinition;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeMatch;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowEmbeddingModel;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowModelDescriptor;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowModelProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorDocument;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorMatch;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStore;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStoreDescriptor;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStoreProvider;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class TinyflowLocalIndexerTest {

	@Test
	public void shouldRecordAndRemoveOrphanVectorWhenChunkSaveFails() {
		MemoryRepository repository = new MemoryRepository();
		RecordingStore store = new RecordingStore();
		TinyflowLocalIndexer indexer = indexer(repository, store, true, true, 10);
		repository.failNextChunkSave = true;

		assertThrows(TinyflowException.class, () -> indexer.index(document("doc-new"), chunks("doc-new", 1)));
		assertEquals(Collections.singletonList("vec-chunk-1"), repository.pendingVectorIds("tenant-a", "kb-1"));

		new TinyflowVectorCompensator(repository, indexer).retry("tenant-a", "kb-1");
		assertEquals(Collections.singletonList("vec-chunk-1"), store.removed);
		assertTrue(repository.pendingVectorIds("tenant-a", "kb-1").isEmpty());
	}

	@Test
	public void shouldAttachAllSourceMetadataAndFailForMissingProviders() {
		MemoryRepository repository = new MemoryRepository();
		RecordingStore store = new RecordingStore();
		TinyflowLocalIndexer indexer = indexer(repository, store, true, true, 10);

		indexer.index(document("doc-1"), chunks("doc-1", 1));

		TinyflowVectorDocument written = store.added.get(0);
		assertEquals("chunk-1", written.getId());
		assertEquals("kb-1", written.getMetadata().get("knowledge_id"));
		assertEquals("source-1", written.getMetadata().get("source_id"));
		assertEquals("file", written.getMetadata().get("source_type"));
		assertEquals("doc-1", written.getMetadata().get("document_id"));
		assertEquals("chunk-1", written.getMetadata().get("chunk_id"));
		assertThrows(TinyflowException.class, () -> indexer(repository, store, false, true, 10)
				.index(document("doc-model"), chunks("doc-model", 1)));
		assertThrows(TinyflowException.class, () -> indexer(repository, store, true, false, 10)
				.index(document("doc-store"), chunks("doc-store", 1)));
	}

	@Test
	public void shouldExposeWrittenFirstBatchForCompensationWhenLaterBatchFails() throws Exception {
		MemoryRepository repository = new MemoryRepository();
		RecordingStore store = new RecordingStore();
		store.failOnDocumentCall = 2;
		TinyflowLocalIndexer indexer = indexer(repository, store, true, true, 1);

		TinyflowException error = assertThrows(TinyflowException.class,
				() -> indexer.index(document("doc-batch"), chunks("doc-batch", 2)));

		assertTrue(error.getCause() instanceof Exception);
		assertEquals(Collections.singletonList("vec-chunk-1"), repository.pendingVectorIds("tenant-a", "kb-1"));
	}

	@Test
	public void shouldTrackEveryVectorWrittenBeforeFailureInsideOneBatch() {
		MemoryRepository repository = new MemoryRepository();
		RecordingStore store = new RecordingStore();
		store.failOnDocumentCall = 3;
		TinyflowLocalIndexer indexer = indexer(repository, store, true, true, 10);

		assertThrows(TinyflowException.class,
				() -> indexer.index(document("doc-partial"), chunks("doc-partial", 3)));

		assertEquals(Arrays.asList("vec-chunk-1", "vec-chunk-2"),
				repository.pendingVectorIds("tenant-a", "kb-1"));
	}

	@Test
	public void shouldUseKnowledgeVectorStoreForIndexAndDelete() throws Exception {
		MemoryRepository repository = new MemoryRepository();
		RecordingStore defaultStore = new RecordingStore();
		RecordingStore knowledgeStore = new RecordingStore();
		RoutingStoreProvider stores = new RoutingStoreProvider(defaultStore, knowledgeStore);
		repository.saveKnowledge(knowledge("store-b"), 0L);
		TinyflowLocalIndexer indexer = new TinyflowLocalIndexer(repository, new FixedModelProvider(true),
				stores, "model-a", "store-a", 10);

		indexer.index(document("doc-route"), chunks("doc-route", 1));
		indexer.delete("tenant-a", "kb-1", Collections.singletonList("vec-chunk-1"));

		assertTrue(defaultStore.added.isEmpty());
		assertTrue(defaultStore.removed.isEmpty());
		assertEquals(1, knowledgeStore.added.size());
		assertEquals(Collections.singletonList("vec-chunk-1"), knowledgeStore.removed);
	}

	@Test
	public void shouldResolveEmbeddingModelForEachKnowledge() {
		MemoryRepository repository = new MemoryRepository();
		RecordingStore store = new RecordingStore();
		RecordingModelProvider models = new RecordingModelProvider();
		repository.saveKnowledge(knowledge("kb-1", "embedding-a", "store-a"), 0L);
		repository.saveKnowledge(knowledge("kb-2", "embedding-b", "store-a"), 0L);
		TinyflowLocalIndexer indexer = new TinyflowLocalIndexer(repository, models,
				new FixedStoreProvider(store, true), "fallback-model", "store-a", 10);

		indexer.index(document("kb-1", "doc-a"), chunks("kb-1", "doc-a", 1));
		indexer.index(document("kb-2", "doc-b"), chunks("kb-2", "doc-b", 1));

		assertEquals(Arrays.asList("embedding-a", "embedding-b"), models.requestedKeys);
	}

	@Test
	public void shouldTreatDuplicateAndMissingVectorIdsAsSuccessfullyDeleted() {
		RecordingStore store = new RecordingStore();
		List<String> vectorIds = Arrays.asList("vec-existing", "vec-missing", "vec-existing");

		store.removeAll(vectorIds);
		store.removeAll(vectorIds);

		assertEquals(Arrays.asList("vec-existing", "vec-missing"), store.removed);
		assertEquals(2, store.removeCalls);
	}

	static TinyflowLocalIndexer indexer(MemoryRepository repository, RecordingStore store,
			boolean hasModel, boolean hasStore, int batchSize) {
		return new TinyflowLocalIndexer(repository, new FixedModelProvider(hasModel),
				new FixedStoreProvider(store, hasStore), "model-a", "store-a", batchSize);
	}

	static TinyflowDocument document(String documentId) {
		return document("kb-1", documentId);
	}

	static TinyflowDocument document(String knowledgeId, String documentId) {
		return TinyflowDocument.builder().tenantId("tenant-a").knowledgeId(knowledgeId)
				.sourceId("source-1").sourceType("file").documentId(documentId).checksum("checksum-" + documentId)
				.name("fixture.txt").content("fixture").build();
	}

	static List<TinyflowChunk> chunks(String documentId, int count) {
		return chunks("kb-1", documentId, count);
	}

	static List<TinyflowChunk> chunks(String knowledgeId, String documentId, int count) {
		List<TinyflowChunk> values = new ArrayList<TinyflowChunk>();
		for (int index = 1; index <= count; index++) {
			values.add(TinyflowChunk.builder().tenantId("tenant-a").knowledgeId(knowledgeId)
					.sourceId("source-1").sourceType("file").documentId(documentId).chunkId("chunk-" + index)
					.content("chunk content " + index).build());
		}
		return values;
	}

	static TinyflowKnowledgeDefinition knowledge(String storeKey) {
		return knowledge("kb-1", "model-a", storeKey);
	}

	static TinyflowKnowledgeDefinition knowledge(String knowledgeId, String embeddingModelKey, String storeKey) {
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		metadata.put("embeddingModelKey", embeddingModelKey);
		metadata.put(TinyflowKnowledgeMaintenanceService.VECTOR_STORE_KEY, storeKey);
		return TinyflowKnowledgeDefinition.builder().providerKey("fw-plus-local").tenantId("tenant-a")
				.knowledgeId(knowledgeId).name("Knowledge").metadata(metadata).build();
	}

	static final class FixedModelProvider implements TinyflowModelProvider {
		private final boolean available;
		FixedModelProvider(boolean available) { this.available = available; }
		@Override public List<TinyflowModelDescriptor> listChatModels() { return Collections.emptyList(); }
		@Override public List<TinyflowModelDescriptor> listEmbeddingModels() { return Collections.emptyList(); }
		@Override public dev.langchain4j.model.chat.ChatModel getChatModel(String key) { return null; }
		@Override public TinyflowEmbeddingModel getEmbeddingModel(String key) {
			return available ? text -> new float[] { text.length() } : null;
		}
	}

	static final class RecordingModelProvider implements TinyflowModelProvider {
		final List<String> requestedKeys = new ArrayList<String>();
		@Override public List<TinyflowModelDescriptor> listChatModels() { return Collections.emptyList(); }
		@Override public List<TinyflowModelDescriptor> listEmbeddingModels() { return Collections.emptyList(); }
		@Override public dev.langchain4j.model.chat.ChatModel getChatModel(String key) { return null; }
		@Override public TinyflowEmbeddingModel getEmbeddingModel(String key) {
			requestedKeys.add(key);
			return text -> new float[] { text.length() };
		}
	}

	static final class FixedStoreProvider implements TinyflowVectorStoreProvider {
		private final RecordingStore store;
		private final boolean available;
		FixedStoreProvider(RecordingStore store, boolean available) { this.store = store; this.available = available; }
		@Override public List<TinyflowVectorStoreDescriptor> list() {
			return Collections.singletonList(new TinyflowVectorStoreDescriptor("store-a", "Store A", "memory", "test", false));
		}
		@Override public TinyflowVectorStore get(String key) { return available ? store : null; }
	}

	static final class RoutingStoreProvider implements TinyflowVectorStoreProvider {
		private final Map<String, RecordingStore> stores = new LinkedHashMap<String, RecordingStore>();
		RoutingStoreProvider(RecordingStore defaultStore, RecordingStore knowledgeStore) {
			stores.put("store-a", defaultStore);
			stores.put("store-b", knowledgeStore);
		}
		@Override public List<TinyflowVectorStoreDescriptor> list() {
			return Arrays.asList(
					new TinyflowVectorStoreDescriptor("store-a", "Store A", "memory", "test", false),
					new TinyflowVectorStoreDescriptor("store-b", "Store B", "memory", "test", false));
		}
		@Override public TinyflowVectorStore get(String key) { return stores.get(key); }
	}

	static final class RecordingStore implements TinyflowVectorStore {
		final List<TinyflowVectorDocument> added = new ArrayList<TinyflowVectorDocument>();
		final List<String> removed = new ArrayList<String>();
		int addCalls;
		int failOnAddCall;
		int documentCalls;
		int failOnDocumentCall;
		int removeCalls;
		boolean failRemove;
		boolean failOnRepeatedRemove;
		@Override public List<String> addAll(List<TinyflowVectorDocument> documents) {
			addCalls++;
			if (failOnAddCall == addCalls) { throw new IllegalStateException("vector write failed"); }
			List<String> ids = new ArrayList<String>();
			for (TinyflowVectorDocument document : documents) { ids.add(add(document)); }
			return ids;
		}
		public String add(TinyflowVectorDocument document) {
			documentCalls++;
			if (failOnDocumentCall == documentCalls) { throw new IllegalStateException("vector write failed"); }
			added.add(document);
			return "vec-" + document.getId();
		}
		@Override public void removeAll(List<String> ids) {
			removeCalls++;
			if (failRemove) { throw new IllegalStateException("vector remove failed"); }
			if (failOnRepeatedRemove && !Collections.disjoint(removed, ids)) {
				throw new IllegalStateException("vector was already removed");
			}
			for (String id : ids) {
				if (!removed.contains(id)) { removed.add(id); }
			}
		}
		@Override public List<TinyflowVectorMatch> search(float[] vector, int maxResults, double minScore) {
			return Collections.emptyList();
		}
	}

	static final class MemoryRepository implements TinyflowKnowledgeRepository {
		final Map<String, TinyflowKnowledgeDefinition> knowledge = new LinkedHashMap<String, TinyflowKnowledgeDefinition>();
		final Map<String, Long> versions = new LinkedHashMap<String, Long>();
		final Map<String, TinyflowDocument> documents = new LinkedHashMap<String, TinyflowDocument>();
		final Map<String, String> active = new LinkedHashMap<String, String>();
		final Map<String, TinyflowChunk> storedChunks = new LinkedHashMap<String, TinyflowChunk>();
		final List<TinyflowIngestionTask> tasks = new ArrayList<TinyflowIngestionTask>();
		final List<VectorCompensation> compensations = new ArrayList<VectorCompensation>();
		boolean failNextChunkSave;
		boolean failActivation;
		boolean failNextDocumentVersionDelete;
		boolean failNextSourceDelete;
		String failTaskStatusOnce;
		String failCompensationStartAt;
		boolean failNextStageAdvance;

		List<String> pendingVectorIds(String tenantId, String knowledgeId) {
			List<String> values = new ArrayList<String>();
			for (VectorCompensation compensation : findVectorCompensations(tenantId, knowledgeId)) {
				values.addAll(compensation.getVectorIds());
			}
			return values;
		}

		@Override public TinyflowKnowledgeDefinition findKnowledge(String tenantId, String knowledgeId) { return knowledge.get(key(tenantId, knowledgeId)); }
		@Override public long findKnowledgeVersion(String tenantId, String knowledgeId) { Long value = versions.get(key(tenantId, knowledgeId)); return value == null ? 0L : value; }
		@Override public void saveKnowledge(TinyflowKnowledgeDefinition definition, long expectedVersion) {
			long actual = findKnowledgeVersion(definition.getTenantId(), definition.getKnowledgeId());
			if (actual != expectedVersion) { throw new OptimisticLockException(expectedVersion, actual); }
			knowledge.put(key(definition.getTenantId(), definition.getKnowledgeId()), definition);
			versions.put(key(definition.getTenantId(), definition.getKnowledgeId()), actual + 1L);
		}
		@Override public synchronized void saveKnowledgeWithVectorStoreOwnership(TinyflowKnowledgeDefinition definition,
				long expectedVersion, String vectorStoreKey, boolean metadataFilterSupported) {
			for (TinyflowKnowledgeDefinition existing : knowledge.values()) {
				Object existingStore = existing.getMetadata().get(TinyflowKnowledgeMaintenanceService.VECTOR_STORE_KEY);
				if (!metadataFilterSupported && vectorStoreKey.equals(String.valueOf(existingStore))
						&& !(definition.getTenantId().equals(existing.getTenantId())
						&& definition.getKnowledgeId().equals(existing.getKnowledgeId()))) {
					throw new VectorStoreOwnershipException(vectorStoreKey);
				}
			}
			saveKnowledge(definition, expectedVersion);
		}
		@Override public void deleteKnowledge(String tenantId, String knowledgeId, long expectedVersion) { knowledge.remove(key(tenantId, knowledgeId)); versions.remove(key(tenantId, knowledgeId)); }
		@Override public TinyflowDocument findBySourceAndChecksum(String tenantId, String knowledgeId, String sourceId, String checksum) {
			for (TinyflowDocument document : findDocumentVersions(tenantId, knowledgeId, sourceId)) { if (checksum.equals(document.getChecksum())) { return document; } }
			return null;
		}
		@Override public TinyflowDocument findActiveDocument(String tenantId, String knowledgeId, String sourceId) {
			String id = active.get(sourceKey(tenantId, knowledgeId, sourceId));
			return id == null ? null : documents.get(documentKey(tenantId, knowledgeId, id));
		}
		@Override public void saveDocument(TinyflowDocument document) { documents.put(documentKey(document.getTenantId(), document.getKnowledgeId(), document.getDocumentId()), document); }
		@Override public List<TinyflowDocument> findDocumentVersions(String tenantId, String knowledgeId, String sourceId) {
			List<TinyflowDocument> values = new ArrayList<TinyflowDocument>();
			for (TinyflowDocument document : documents.values()) { if (tenantId.equals(document.getTenantId()) && knowledgeId.equals(document.getKnowledgeId()) && sourceId.equals(document.getSourceId())) { values.add(document); } }
			return values;
		}
		@Override public List<TinyflowDocument> findDocumentsByKnowledge(String tenantId, String knowledgeId) {
			List<TinyflowDocument> values = new ArrayList<TinyflowDocument>();
			for (TinyflowDocument document : documents.values()) {
				if (tenantId.equals(document.getTenantId()) && knowledgeId.equals(document.getKnowledgeId())) {
					values.add(document);
				}
			}
			return values;
		}
		@Override public void activateDocumentVersion(String tenantId, String knowledgeId, String sourceId, String documentId) {
			if (failActivation) { failActivation = false; throw new IllegalStateException("activation failed"); }
			active.put(sourceKey(tenantId, knowledgeId, sourceId), documentId);
		}
		@Override public void deleteDocumentsBySource(String tenantId, String knowledgeId, String sourceId) {
			if (failNextSourceDelete) { failNextSourceDelete = false; throw new IllegalStateException("source deletion failed"); }
			for (TinyflowDocument document : new ArrayList<TinyflowDocument>(findDocumentVersions(tenantId, knowledgeId, sourceId))) { documents.remove(documentKey(tenantId, knowledgeId, document.getDocumentId())); }
			active.remove(sourceKey(tenantId, knowledgeId, sourceId));
			for (TinyflowChunk chunk : new ArrayList<TinyflowChunk>(storedChunks.values())) { if (tenantId.equals(chunk.getTenantId()) && knowledgeId.equals(chunk.getKnowledgeId()) && sourceId.equals(chunk.getSourceId())) { storedChunks.remove(chunkKey(tenantId, knowledgeId, chunk.getChunkId())); } }
		}
		@Override public void deleteDocumentVersion(String tenantId, String knowledgeId, String sourceId, String documentId) {
			if (failNextDocumentVersionDelete) { failNextDocumentVersionDelete = false; throw new IllegalStateException("document version deletion failed"); }
			TinyflowDocument document = documents.get(documentKey(tenantId, knowledgeId, documentId));
			if (document == null) { return; }
			if (!sourceId.equals(document.getSourceId())) { throw new IllegalArgumentException("Document does not belong to source"); }
			if (documentId.equals(active.get(sourceKey(tenantId, knowledgeId, sourceId)))) { throw new IllegalStateException("Active document version cannot be deleted"); }
			documents.remove(documentKey(tenantId, knowledgeId, documentId));
			for (TinyflowChunk chunk : new ArrayList<TinyflowChunk>(storedChunks.values())) { if (tenantId.equals(chunk.getTenantId()) && knowledgeId.equals(chunk.getKnowledgeId()) && documentId.equals(chunk.getDocumentId())) { storedChunks.remove(chunkKey(tenantId, knowledgeId, chunk.getChunkId())); } }
		}
		@Override public void saveChunks(List<TinyflowChunk> chunks) {
			if (failNextChunkSave) { failNextChunkSave = false; throw new IllegalStateException("chunk persistence failed"); }
			for (TinyflowChunk chunk : chunks) { storedChunks.put(chunkKey(chunk.getTenantId(), chunk.getKnowledgeId(), chunk.getChunkId()), chunk); }
		}
		@Override public TinyflowChunk findChunk(String tenantId, String knowledgeId, String chunkId) { return storedChunks.get(chunkKey(tenantId, knowledgeId, chunkId)); }
		@Override public List<TinyflowChunk> findChunksByDocument(String tenantId, String knowledgeId, String documentId) {
			List<TinyflowChunk> values = new ArrayList<TinyflowChunk>();
			for (TinyflowChunk chunk : storedChunks.values()) { if (tenantId.equals(chunk.getTenantId()) && knowledgeId.equals(chunk.getKnowledgeId()) && documentId.equals(chunk.getDocumentId())) { values.add(chunk); } }
			return values;
		}
		@Override public List<TinyflowChunk> findChunksByVectorIds(String tenantId, String knowledgeId, List<String> vectorIds) { return Collections.emptyList(); }
		@Override public List<TinyflowKnowledgeMatch> searchChunksByKeyword(String tenantId, String knowledgeId, String query, int limit) { return Collections.emptyList(); }
		@Override public void updateChunk(String tenantId, String knowledgeId, String chunkId,
				boolean enabled, String content, Map<String, Object> metadata) {
			TinyflowChunk current = findChunk(tenantId, knowledgeId, chunkId);
			if (current == null) { throw new IllegalArgumentException("Chunk not found"); }
			storedChunks.put(chunkKey(tenantId, knowledgeId, chunkId), copyChunk(
					current, enabled, content, metadata, current.getVectorIds()));
		}
		@Override public void replaceChunkWithVectorCompensation(TinyflowChunk chunk,
				VectorCompensation compensation) {
			Map<String, TinyflowChunk> chunkSnapshot = new LinkedHashMap<String, TinyflowChunk>(storedChunks);
			List<VectorCompensation> compensationSnapshot = new ArrayList<VectorCompensation>(compensations);
			try {
				TinyflowChunk current = findChunk(chunk.getTenantId(), chunk.getKnowledgeId(), chunk.getChunkId());
				if (current == null || !current.getVectorIds().equals(compensation.getVectorIds())) {
					throw new IllegalStateException("Chunk vector mapping changed before replacement");
				}
				saveChunks(Collections.singletonList(chunk));
				saveVectorCompensation(compensation);
			} catch (RuntimeException exception) {
				storedChunks.clear(); storedChunks.putAll(chunkSnapshot);
				compensations.clear(); compensations.addAll(compensationSnapshot);
				throw exception;
			}
		}
		@Override public void saveTask(TinyflowIngestionTask task) {
			if ("task".equals(failCompensationStartAt) && "DELETE_PENDING".equals(task.getStatus())) {
				failCompensationStartAt = null;
				throw new IllegalStateException("pending task persistence failed");
			}
			if (task.getStatus().equals(failTaskStatusOnce)) { failTaskStatusOnce = null; throw new IllegalStateException("task persistence failed"); }
			tasks.add(task);
		}
		@Override public void saveTaskWithVectorCompensation(TinyflowIngestionTask task,
				VectorCompensation compensation) {
			List<TinyflowIngestionTask> taskSnapshot = new ArrayList<TinyflowIngestionTask>(tasks);
			List<VectorCompensation> compensationSnapshot = new ArrayList<VectorCompensation>(compensations);
			try {
				saveTask(task);
				saveVectorCompensation(compensation);
			} catch (RuntimeException exception) {
				tasks.clear(); tasks.addAll(taskSnapshot);
				compensations.clear(); compensations.addAll(compensationSnapshot);
				throw exception;
			}
		}
		@Override public void completeIngestion(TinyflowDocument document, List<TinyflowChunk> chunks, TinyflowIngestionTask task) {
			completeIngestionWithVectorCompensations(
					document, chunks, task, Collections.<VectorCompensation>emptyList());
		}
		@Override public void completeIngestionWithVectorCompensations(TinyflowDocument document,
				List<TinyflowChunk> chunks, TinyflowIngestionTask task,
				List<VectorCompensation> cleanupCompensations) {
			Map<String, TinyflowDocument> documentSnapshot = new LinkedHashMap<String, TinyflowDocument>(documents);
			Map<String, String> activeSnapshot = new LinkedHashMap<String, String>(active);
			Map<String, TinyflowChunk> chunkSnapshot = new LinkedHashMap<String, TinyflowChunk>(storedChunks);
			List<TinyflowIngestionTask> taskSnapshot = new ArrayList<TinyflowIngestionTask>(tasks);
			List<VectorCompensation> compensationSnapshot = new ArrayList<VectorCompensation>(compensations);
			try {
				saveChunks(chunks); saveDocument(document);
				activateDocumentVersion(document.getTenantId(), document.getKnowledgeId(),
						document.getSourceId(), document.getDocumentId());
				saveTask(task);
				for (VectorCompensation compensation : cleanupCompensations) { saveVectorCompensation(compensation); }
			} catch (RuntimeException exception) {
				documents.clear(); documents.putAll(documentSnapshot);
				active.clear(); active.putAll(activeSnapshot);
				storedChunks.clear(); storedChunks.putAll(chunkSnapshot);
				tasks.clear(); tasks.addAll(taskSnapshot);
				compensations.clear(); compensations.addAll(compensationSnapshot);
				throw exception;
			}
		}
		@Override public List<TinyflowIngestionTask> findTasks(String tenantId, String knowledgeId) { return new ArrayList<TinyflowIngestionTask>(tasks); }
		@Override public void saveVectorCompensation(VectorCompensation compensation) {
			if ("compensation".equals(failCompensationStartAt)) {
				failCompensationStartAt = null;
				throw new IllegalStateException("compensation persistence failed");
			}
			compensations.add(compensation);
		}
		@Override public List<VectorCompensation> findVectorCompensations(String tenantId, String knowledgeId) {
			List<VectorCompensation> values = new ArrayList<VectorCompensation>();
			for (VectorCompensation compensation : compensations) { if (tenantId.equals(compensation.getTenantId()) && knowledgeId.equals(compensation.getKnowledgeId())) { values.add(compensation); } }
			return values;
		}
		@Override public void advanceVectorCompensationStage(String tenantId, String knowledgeId,
				String compensationId, String expectedStage, String nextStage) {
			if (failNextStageAdvance) {
				failNextStageAdvance = false;
				throw new IllegalStateException("stage persistence failed");
			}
			for (int index = 0; index < compensations.size(); index++) {
				VectorCompensation compensation = compensations.get(index);
				if (tenantId.equals(compensation.getTenantId()) && knowledgeId.equals(compensation.getKnowledgeId())
						&& compensationId.equals(compensation.getCompensationId())) {
					if (nextStage.equals(compensation.getStage())) { return; }
					if (!expectedStage.equals(compensation.getStage())) {
						throw new CompensationStageException(expectedStage, compensation.getStage());
					}
					compensations.set(index, compensation.withStage(nextStage));
					return;
				}
			}
			throw new CompensationStageException(expectedStage, null);
		}
		@Override public void deleteVectorCompensation(String tenantId, String knowledgeId, String compensationId) {
			for (VectorCompensation compensation : new ArrayList<VectorCompensation>(compensations)) { if (compensationId.equals(compensation.getCompensationId())) { compensations.remove(compensation); } }
		}
		private TinyflowChunk copyChunk(TinyflowChunk source, boolean enabled, String content,
				Map<String, Object> metadata, List<String> vectorIds) {
			return TinyflowChunk.builder().tenantId(source.getTenantId()).knowledgeId(source.getKnowledgeId())
					.sourceId(source.getSourceId()).sourceType(source.getSourceType()).documentId(source.getDocumentId())
					.chunkId(source.getChunkId()).content(content).sourceUri(source.getSourceUri())
					.sourceVersion(source.getSourceVersion()).location(source.getLocation()).position(source.getPosition())
					.parentChunkId(source.getParentChunkId()).tokenCount(source.getTokenCount()).metadata(metadata)
					.enabled(enabled).vectorIds(vectorIds).build();
		}
		private static String key(String tenantId, String knowledgeId) { return tenantId + "|" + knowledgeId; }
		private static String sourceKey(String tenantId, String knowledgeId, String sourceId) { return key(tenantId, knowledgeId) + "|" + sourceId; }
		private static String documentKey(String tenantId, String knowledgeId, String documentId) { return key(tenantId, knowledgeId) + "|" + documentId; }
		private static String chunkKey(String tenantId, String knowledgeId, String chunkId) { return key(tenantId, knowledgeId) + "|" + chunkId; }
	}
}
