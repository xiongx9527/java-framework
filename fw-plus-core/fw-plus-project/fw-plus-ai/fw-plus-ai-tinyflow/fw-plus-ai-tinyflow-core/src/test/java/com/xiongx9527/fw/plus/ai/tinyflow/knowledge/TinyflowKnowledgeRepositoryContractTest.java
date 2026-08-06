package com.xiongx9527.fw.plus.ai.tinyflow.knowledge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TinyflowKnowledgeRepositoryContractTest
		extends AbstractTinyflowKnowledgeRepositoryContractTest {

	@Override
	protected TinyflowKnowledgeRepository createRepository() {
		return new InMemoryTinyflowKnowledgeRepository();
	}

	@Override
	protected void failNextCompletionAt(TinyflowKnowledgeRepository target, String step) {
		((InMemoryTinyflowKnowledgeRepository) target).failCompletionAt = step;
	}

	@Override
	protected void pauseNextCompletionAfter(TinyflowKnowledgeRepository target, String step) {
		((InMemoryTinyflowKnowledgeRepository) target).pauseCompletionAfter = step;
	}

	@Override
	protected void awaitCompletionPaused(TinyflowKnowledgeRepository target) throws InterruptedException {
		if (!((InMemoryTinyflowKnowledgeRepository) target).completionPaused.await(5, TimeUnit.SECONDS)) {
			throw new AssertionError("Completion did not reach the configured pause");
		}
	}

	@Override
	protected void releaseCompletion(TinyflowKnowledgeRepository target) {
		((InMemoryTinyflowKnowledgeRepository) target).releaseCompletion.countDown();
	}

	private static final class InMemoryTinyflowKnowledgeRepository implements TinyflowKnowledgeRepository {

		private final Map<String, TinyflowKnowledgeDefinition> knowledge = new LinkedHashMap<String, TinyflowKnowledgeDefinition>();
		private final Map<String, Long> versions = new LinkedHashMap<String, Long>();
		private final Map<String, TinyflowDocument> documents = new LinkedHashMap<String, TinyflowDocument>();
		private final Map<String, String> activeDocuments = new LinkedHashMap<String, String>();
		private final Map<String, TinyflowChunk> chunks = new LinkedHashMap<String, TinyflowChunk>();
		private final List<TinyflowIngestionTask> tasks = new ArrayList<TinyflowIngestionTask>();
		private final List<VectorCompensation> compensations = new ArrayList<VectorCompensation>();
		private String failCompletionAt;
		private String pauseCompletionAfter;
		private final CountDownLatch completionPaused = new CountDownLatch(1);
		private final CountDownLatch releaseCompletion = new CountDownLatch(1);

		@Override
		public synchronized TinyflowKnowledgeDefinition findKnowledge(String tenantId, String knowledgeId) {
			return knowledge.get(key(tenantId, knowledgeId));
		}

		@Override
		public synchronized long findKnowledgeVersion(String tenantId, String knowledgeId) {
			Long version = versions.get(key(tenantId, knowledgeId));
			return version == null ? 0L : version;
		}

		@Override
		public synchronized void saveKnowledge(TinyflowKnowledgeDefinition definition, long expectedVersion) {
			String key = key(definition.getTenantId(), definition.getKnowledgeId());
			long actualVersion = findKnowledgeVersion(definition.getTenantId(), definition.getKnowledgeId());
			if (actualVersion != expectedVersion) {
				throw new OptimisticLockException(expectedVersion, actualVersion);
			}
			knowledge.put(key, definition);
			versions.put(key, actualVersion + 1L);
		}

		@Override
		public synchronized void deleteKnowledge(String tenantId, String knowledgeId, long expectedVersion) {
			long actualVersion = findKnowledgeVersion(tenantId, knowledgeId);
			if (actualVersion != expectedVersion) {
				throw new OptimisticLockException(expectedVersion, actualVersion);
			}
			String key = key(tenantId, knowledgeId);
			knowledge.remove(key);
			versions.remove(key);
		}

		@Override
		public synchronized TinyflowDocument findBySourceAndChecksum(String tenantId, String knowledgeId,
				String sourceId, String checksum) {
			for (TinyflowDocument document : findDocumentVersions(tenantId, knowledgeId, sourceId)) {
					if (checksum.equals(document.getChecksum())) {
					return document;
				}
			}
			return null;
		}

		@Override
		public synchronized TinyflowDocument findActiveDocument(String tenantId, String knowledgeId, String sourceId) {
			String documentId = activeDocuments.get(sourceKey(tenantId, knowledgeId, sourceId));
			return documentId == null ? null : documents.get(documentKey(tenantId, knowledgeId, documentId));
		}

		@Override
		public synchronized void saveDocument(TinyflowDocument document) {
			documents.put(documentKey(document.getTenantId(), document.getKnowledgeId(), document.getDocumentId()), document);
		}

		@Override
		public synchronized List<TinyflowDocument> findDocumentVersions(String tenantId, String knowledgeId,
				String sourceId) {
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
		public synchronized void activateDocumentVersion(String tenantId, String knowledgeId,
				String sourceId, String documentId) {
			TinyflowDocument document = documents.get(documentKey(tenantId, knowledgeId, documentId));
			if (document == null || !sourceId.equals(document.getSourceId())) {
				throw new IllegalArgumentException("Document does not belong to source");
			}
			activeDocuments.put(sourceKey(tenantId, knowledgeId, sourceId), documentId);
		}

		@Override
		public synchronized void deleteDocumentsBySource(String tenantId, String knowledgeId, String sourceId) {
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
		public synchronized void saveChunks(List<TinyflowChunk> values) {
			for (TinyflowChunk chunk : values) {
				chunks.put(chunkKey(chunk.getTenantId(), chunk.getKnowledgeId(), chunk.getChunkId()), chunk);
			}
		}

		@Override
		public synchronized TinyflowChunk findChunk(String tenantId, String knowledgeId, String chunkId) {
			return chunks.get(chunkKey(tenantId, knowledgeId, chunkId));
		}

		@Override
		public synchronized List<TinyflowChunk> findChunksByDocument(String tenantId, String knowledgeId,
				String documentId) {
			List<TinyflowChunk> result = new ArrayList<TinyflowChunk>();
			for (TinyflowChunk chunk : chunks.values()) {
				if (tenantId.equals(chunk.getTenantId())
						&& knowledgeId.equals(chunk.getKnowledgeId())
						&& documentId.equals(chunk.getDocumentId())) {
					result.add(chunk);
				}
			}
			return result;
		}

		@Override
		public synchronized List<TinyflowChunk> findChunksByVectorIds(String tenantId, String knowledgeId,
				List<String> vectorIds) {
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
		public synchronized void updateChunk(String tenantId, String knowledgeId, String chunkId,
				boolean enabled, String content, Map<String, Object> metadata) {
			String key = chunkKey(tenantId, knowledgeId, chunkId);
			TinyflowChunk original = chunks.get(key);
			if (original == null) {
				throw new IllegalArgumentException("Chunk not found");
			}
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
					.metadata(metadata)
					.enabled(enabled)
					.vectorIds(original.getVectorIds())
					.build();
			chunks.put(key, updated);
		}

		@Override
		public synchronized void saveTask(TinyflowIngestionTask task) {
			tasks.add(task);
		}

		@Override
		public synchronized void completeIngestion(TinyflowDocument completedDocument, List<TinyflowChunk> values,
				TinyflowIngestionTask completedTask) {
			Map<String, TinyflowDocument> documentSnapshot =
					new LinkedHashMap<String, TinyflowDocument>(documents);
			Map<String, String> activeSnapshot = new LinkedHashMap<String, String>(activeDocuments);
			Map<String, TinyflowChunk> chunkSnapshot = new LinkedHashMap<String, TinyflowChunk>(chunks);
			List<TinyflowIngestionTask> taskSnapshot = new ArrayList<TinyflowIngestionTask>(tasks);
			try {
				saveChunks(values);
				afterCompletionStep("chunks");
				saveDocument(completedDocument);
				afterCompletionStep("document");
				activateDocumentVersion(completedDocument.getTenantId(), completedDocument.getKnowledgeId(),
						completedDocument.getSourceId(), completedDocument.getDocumentId());
				afterCompletionStep("active");
				saveTask(completedTask);
				afterCompletionStep("task");
			} catch (RuntimeException exception) {
				documents.clear();
				documents.putAll(documentSnapshot);
				activeDocuments.clear();
				activeDocuments.putAll(activeSnapshot);
				chunks.clear();
				chunks.putAll(chunkSnapshot);
				tasks.clear();
				tasks.addAll(taskSnapshot);
				throw exception;
			}
		}

		@Override
		public synchronized List<TinyflowIngestionTask> findTasks(String tenantId, String knowledgeId) {
			List<TinyflowIngestionTask> result = new ArrayList<TinyflowIngestionTask>();
			for (TinyflowIngestionTask task : tasks) {
				if (tenantId.equals(task.getTenantId()) && knowledgeId.equals(task.getKnowledgeId())) {
					result.add(task);
				}
			}
			return result;
		}

		@Override
		public synchronized void saveVectorCompensation(VectorCompensation compensation) {
			compensations.add(compensation);
		}

		@Override
		public synchronized List<VectorCompensation> findVectorCompensations(String tenantId, String knowledgeId) {
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
		public synchronized void deleteVectorCompensation(String tenantId, String knowledgeId, String compensationId) {
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

		private void afterCompletionStep(String step) {
			if (step.equals(failCompletionAt)) {
				failCompletionAt = null;
				throw new IllegalStateException("Completion failed after " + step);
			}
			if (step.equals(pauseCompletionAfter)) {
				completionPaused.countDown();
				try {
					if (!releaseCompletion.await(5, TimeUnit.SECONDS)) {
						throw new IllegalStateException("Timed out waiting to release completion");
					}
				} catch (InterruptedException exception) {
					Thread.currentThread().interrupt();
					throw new IllegalStateException("Interrupted while pausing completion", exception);
				}
			}
		}

	}
}
