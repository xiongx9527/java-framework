package com.xiongx9527.fw.plus.ai.tinyflow.knowledge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

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
	protected void failNextCompensationStartAt(TinyflowKnowledgeRepository target, String step) {
		((InMemoryTinyflowKnowledgeRepository) target).failCompensationStartAt = step;
	}

	@Override
	protected RepositoryConcurrencyProbe concurrencyProbe(TinyflowKnowledgeRepository target) {
		return (RepositoryConcurrencyProbe) target;
	}

	public static final class InMemoryTinyflowKnowledgeRepository
			implements TinyflowKnowledgeRepository, RepositoryConcurrencyProbe {

		private final Map<String, TinyflowKnowledgeDefinition> knowledge =
				new LinkedHashMap<String, TinyflowKnowledgeDefinition>();
		private final Map<String, Long> versions = new LinkedHashMap<String, Long>();
		private final Map<String, VectorStoreOwnership> vectorStoreOwnerships =
				new LinkedHashMap<String, VectorStoreOwnership>();
		private final Map<String, TinyflowDocument> documents = new LinkedHashMap<String, TinyflowDocument>();
		private final Map<String, String> activeDocuments = new LinkedHashMap<String, String>();
		private final Map<String, TinyflowChunk> chunks = new LinkedHashMap<String, TinyflowChunk>();
		private final List<TinyflowIngestionTask> tasks = new ArrayList<TinyflowIngestionTask>();
		private final List<VectorCompensation> compensations = new ArrayList<VectorCompensation>();
		private final ReentrantLock repositoryLock = new ReentrantLock(true);
		private volatile String failCompletionAt;
		private volatile String failCompensationStartAt;
		private volatile String pauseCompletionAfter;
		private final CountDownLatch completionPaused = new CountDownLatch(1);
		private final CountDownLatch releaseCompletion = new CountDownLatch(1);
		private volatile CountDownLatch readerEntered;
		private volatile CountDownLatch writerEntered;
		private volatile Thread readerThread;
		private volatile Thread writerThread;
		private volatile int readerEntryCount;
		private volatile int writerEntryCount;
		private volatile boolean failNextStageAdvance;

		public void failNextStageAdvance() {
			failNextStageAdvance = true;
		}

		@Override
		public TinyflowKnowledgeDefinition findKnowledge(String tenantId, String knowledgeId) {
			return read(() -> knowledge.get(key(tenantId, knowledgeId)));
		}

		@Override
		public long findKnowledgeVersion(String tenantId, String knowledgeId) {
			return read(() -> {
				Long version = versions.get(key(tenantId, knowledgeId));
				return version == null ? 0L : version;
			});
		}

		@Override
		public void saveKnowledge(TinyflowKnowledgeDefinition definition, long expectedVersion) {
			write(() -> {
				String key = key(definition.getTenantId(), definition.getKnowledgeId());
				long actualVersion = findKnowledgeVersion(definition.getTenantId(), definition.getKnowledgeId());
				if (actualVersion != expectedVersion) {
					throw new OptimisticLockException(expectedVersion, actualVersion);
				}
				knowledge.put(key, definition);
				versions.put(key, actualVersion + 1L);
			});
		}

		@Override
		public void saveKnowledgeWithVectorStoreOwnership(TinyflowKnowledgeDefinition definition,
				long expectedVersion, String vectorStoreKey, boolean metadataFilterSupported) {
			write(() -> {
				String ownerKey = key(definition.getTenantId(), definition.getKnowledgeId());
				for (Map.Entry<String, VectorStoreOwnership> entry : vectorStoreOwnerships.entrySet()) {
					VectorStoreOwnership ownership = entry.getValue();
					if (!ownerKey.equals(entry.getKey()) && vectorStoreKey.equals(ownership.vectorStoreKey)
							&& (!metadataFilterSupported || !ownership.metadataFilterSupported)) {
						throw new VectorStoreOwnershipException(vectorStoreKey);
					}
				}
				saveKnowledge(definition, expectedVersion);
				vectorStoreOwnerships.put(ownerKey,
						new VectorStoreOwnership(vectorStoreKey, metadataFilterSupported));
			});
		}

		@Override
		public void deleteKnowledge(String tenantId, String knowledgeId, long expectedVersion) {
			write(() -> {
				long actualVersion = findKnowledgeVersion(tenantId, knowledgeId);
				if (actualVersion != expectedVersion) {
					throw new OptimisticLockException(expectedVersion, actualVersion);
				}
				String key = key(tenantId, knowledgeId);
					knowledge.remove(key);
					versions.remove(key);
					vectorStoreOwnerships.remove(key);
				});
		}

		@Override
		public TinyflowDocument findBySourceAndChecksum(String tenantId, String knowledgeId,
				String sourceId, String checksum) {
			return read(() -> {
				for (TinyflowDocument document : findDocumentVersions(tenantId, knowledgeId, sourceId)) {
					if (checksum.equals(document.getChecksum())) {
						return document;
					}
				}
				return null;
			});
		}

		@Override
		public TinyflowDocument findActiveDocument(String tenantId, String knowledgeId, String sourceId) {
			return read(() -> {
				String documentId = activeDocuments.get(sourceKey(tenantId, knowledgeId, sourceId));
				return documentId == null ? null : documents.get(documentKey(tenantId, knowledgeId, documentId));
			});
		}

		@Override
		public void saveDocument(TinyflowDocument document) {
			write(() -> documents.put(
					documentKey(document.getTenantId(), document.getKnowledgeId(), document.getDocumentId()), document));
		}

		@Override
		public List<TinyflowDocument> findDocumentVersions(String tenantId, String knowledgeId, String sourceId) {
			return read(() -> {
				List<TinyflowDocument> result = new ArrayList<TinyflowDocument>();
				for (TinyflowDocument document : documents.values()) {
					if (tenantId.equals(document.getTenantId())
							&& knowledgeId.equals(document.getKnowledgeId())
							&& sourceId.equals(document.getSourceId())) {
						result.add(document);
					}
				}
				return result;
			});
		}

		@Override
		public void activateDocumentVersion(String tenantId, String knowledgeId,
				String sourceId, String documentId) {
			write(() -> {
				TinyflowDocument document = documents.get(documentKey(tenantId, knowledgeId, documentId));
				if (document == null || !sourceId.equals(document.getSourceId())) {
					throw new IllegalArgumentException("Document does not belong to source");
				}
				activeDocuments.put(sourceKey(tenantId, knowledgeId, sourceId), documentId);
			});
		}

		@Override
		public void deleteDocumentsBySource(String tenantId, String knowledgeId, String sourceId) {
			write(() -> {
				for (TinyflowDocument document : new ArrayList<TinyflowDocument>(documents.values())) {
					if (tenantId.equals(document.getTenantId())
							&& knowledgeId.equals(document.getKnowledgeId())
							&& sourceId.equals(document.getSourceId())) {
						documents.remove(documentKey(tenantId, knowledgeId, document.getDocumentId()));
					}
				}
				activeDocuments.remove(sourceKey(tenantId, knowledgeId, sourceId));
				for (TinyflowChunk chunk : new ArrayList<TinyflowChunk>(chunks.values())) {
					if (tenantId.equals(chunk.getTenantId())
							&& knowledgeId.equals(chunk.getKnowledgeId())
							&& sourceId.equals(chunk.getSourceId())) {
						chunks.remove(chunkKey(tenantId, knowledgeId, chunk.getChunkId()));
					}
				}
			});
		}

		@Override
		public void deleteDocumentVersion(String tenantId, String knowledgeId,
				String sourceId, String documentId) {
			write(() -> {
				String documentKey = documentKey(tenantId, knowledgeId, documentId);
				TinyflowDocument document = documents.get(documentKey);
				if (document == null) {
					return;
				}
				if (!sourceId.equals(document.getSourceId())) {
					throw new IllegalArgumentException("Document does not belong to source");
				}
				if (documentId.equals(activeDocuments.get(sourceKey(tenantId, knowledgeId, sourceId)))) {
					throw new IllegalStateException("Active document version cannot be deleted");
				}
				documents.remove(documentKey);
				for (TinyflowChunk chunk : new ArrayList<TinyflowChunk>(chunks.values())) {
					if (tenantId.equals(chunk.getTenantId()) && knowledgeId.equals(chunk.getKnowledgeId())
							&& documentId.equals(chunk.getDocumentId())) {
						chunks.remove(chunkKey(tenantId, knowledgeId, chunk.getChunkId()));
					}
				}
			});
		}

		@Override
		public void saveChunks(List<TinyflowChunk> values) {
			write(() -> {
				for (TinyflowChunk chunk : values) {
					chunks.put(chunkKey(chunk.getTenantId(), chunk.getKnowledgeId(), chunk.getChunkId()), chunk);
				}
			});
		}

		@Override
		public TinyflowChunk findChunk(String tenantId, String knowledgeId, String chunkId) {
			return read(() -> chunks.get(chunkKey(tenantId, knowledgeId, chunkId)));
		}

		@Override
		public List<TinyflowChunk> findChunksByDocument(String tenantId, String knowledgeId, String documentId) {
			return read(() -> {
				List<TinyflowChunk> result = new ArrayList<TinyflowChunk>();
				for (TinyflowChunk chunk : chunks.values()) {
					if (tenantId.equals(chunk.getTenantId())
							&& knowledgeId.equals(chunk.getKnowledgeId())
							&& documentId.equals(chunk.getDocumentId())) {
						result.add(chunk);
					}
				}
				return result;
			});
		}

		@Override
		public List<TinyflowChunk> findChunksByVectorIds(String tenantId, String knowledgeId,
				List<String> vectorIds) {
			return read(() -> {
				List<TinyflowChunk> result = new ArrayList<TinyflowChunk>();
				for (TinyflowChunk chunk : chunks.values()) {
					if (tenantId.equals(chunk.getTenantId())
							&& knowledgeId.equals(chunk.getKnowledgeId())
							&& !Collections.disjoint(chunk.getVectorIds(), vectorIds)) {
						result.add(chunk);
					}
				}
				return result;
			});
		}

		@Override
		public void updateChunk(String tenantId, String knowledgeId, String chunkId,
				boolean enabled, String content, Map<String, Object> metadata) {
			write(() -> {
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
			});
		}

		@Override
		public void saveTask(TinyflowIngestionTask task) {
			write(() -> tasks.add(task));
		}

		@Override
		public void saveTaskWithVectorCompensation(TinyflowIngestionTask task,
				VectorCompensation compensation) {
			write(() -> {
				List<TinyflowIngestionTask> taskSnapshot = new ArrayList<TinyflowIngestionTask>(tasks);
				List<VectorCompensation> compensationSnapshot = new ArrayList<VectorCompensation>(compensations);
				try {
					saveTask(task);
					afterCompensationStartStep("task");
					saveVectorCompensation(compensation);
					afterCompensationStartStep("compensation");
				} catch (RuntimeException exception) {
					tasks.clear();
					tasks.addAll(taskSnapshot);
					compensations.clear();
					compensations.addAll(compensationSnapshot);
					throw exception;
				}
			});
		}

		@Override
		public void completeIngestion(TinyflowDocument completedDocument, List<TinyflowChunk> values,
				TinyflowIngestionTask completedTask) {
			completeIngestionWithVectorCompensations(
					completedDocument, values, completedTask, Collections.<VectorCompensation>emptyList());
		}

		@Override
		public void completeIngestionWithVectorCompensations(TinyflowDocument completedDocument,
				List<TinyflowChunk> values, TinyflowIngestionTask completedTask,
				List<VectorCompensation> cleanupCompensations) {
			write(() -> {
				Map<String, TinyflowDocument> documentSnapshot =
						new LinkedHashMap<String, TinyflowDocument>(documents);
				Map<String, String> activeSnapshot = new LinkedHashMap<String, String>(activeDocuments);
				Map<String, TinyflowChunk> chunkSnapshot = new LinkedHashMap<String, TinyflowChunk>(chunks);
				List<TinyflowIngestionTask> taskSnapshot = new ArrayList<TinyflowIngestionTask>(tasks);
				List<VectorCompensation> compensationSnapshot = new ArrayList<VectorCompensation>(compensations);
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
					for (VectorCompensation compensation : cleanupCompensations) {
						saveVectorCompensation(compensation);
					}
					afterCompletionStep("compensation");
				} catch (RuntimeException exception) {
					documents.clear();
					documents.putAll(documentSnapshot);
					activeDocuments.clear();
					activeDocuments.putAll(activeSnapshot);
					chunks.clear();
					chunks.putAll(chunkSnapshot);
					tasks.clear();
					tasks.addAll(taskSnapshot);
					compensations.clear();
					compensations.addAll(compensationSnapshot);
					throw exception;
				}
			});
		}

		@Override
		public List<TinyflowIngestionTask> findTasks(String tenantId, String knowledgeId) {
			return read(() -> {
				List<TinyflowIngestionTask> result = new ArrayList<TinyflowIngestionTask>();
				for (TinyflowIngestionTask task : tasks) {
					if (tenantId.equals(task.getTenantId()) && knowledgeId.equals(task.getKnowledgeId())) {
						result.add(task);
					}
				}
				return result;
			});
		}

		@Override
		public void saveVectorCompensation(VectorCompensation compensation) {
			write(() -> compensations.add(compensation));
		}

		@Override
		public List<VectorCompensation> findVectorCompensations(String tenantId, String knowledgeId) {
			return read(() -> {
				List<VectorCompensation> result = new ArrayList<VectorCompensation>();
				for (VectorCompensation compensation : compensations) {
					if (tenantId.equals(compensation.getTenantId())
							&& knowledgeId.equals(compensation.getKnowledgeId())) {
						result.add(compensation);
					}
				}
				return result;
			});
		}

		@Override
		public void advanceVectorCompensationStage(String tenantId, String knowledgeId,
				String compensationId, String expectedStage, String nextStage) {
			write(() -> {
				if (failNextStageAdvance) {
					failNextStageAdvance = false;
					throw new IllegalStateException("stage persistence failed");
				}
				for (int index = 0; index < compensations.size(); index++) {
					VectorCompensation compensation = compensations.get(index);
					if (tenantId.equals(compensation.getTenantId())
							&& knowledgeId.equals(compensation.getKnowledgeId())
							&& compensationId.equals(compensation.getCompensationId())) {
						if (nextStage.equals(compensation.getStage())) {
							return;
						}
						if (!expectedStage.equals(compensation.getStage())) {
							throw new CompensationStageException(expectedStage, compensation.getStage());
						}
						compensations.set(index, compensation.withStage(nextStage));
						return;
					}
				}
				throw new CompensationStageException(expectedStage, null);
			});
		}

		@Override
		public void deleteVectorCompensation(String tenantId, String knowledgeId, String compensationId) {
			write(() -> {
				for (VectorCompensation compensation : new ArrayList<VectorCompensation>(compensations)) {
					if (tenantId.equals(compensation.getTenantId())
							&& knowledgeId.equals(compensation.getKnowledgeId())
							&& compensationId.equals(compensation.getCompensationId())) {
						compensations.remove(compensation);
					}
				}
			});
		}

		@Override
		public void pauseNextCompletionAfter(String step) {
			pauseCompletionAfter = step;
		}

		@Override
		public void awaitCompletionPaused() throws InterruptedException {
			await(completionPaused, "Completion did not reach the configured pause");
		}

		@Override
		public void releaseCompletion() {
			releaseCompletion.countDown();
		}

		@Override
		public void expectReaderEntry() {
			readerThread = null;
			readerEntryCount = 0;
			readerEntered = new CountDownLatch(1);
		}

		@Override
		public void awaitReaderEntered() throws InterruptedException {
			await(readerEntered, "Reader did not enter the repository read path");
		}

		@Override
		public void awaitReaderBlocked() {
			awaitBlocked(readerThread, "Reader", true);
		}

		@Override
		public void expectWriterEntry() {
			writerThread = null;
			writerEntryCount = 0;
			writerEntered = new CountDownLatch(1);
		}

		@Override
		public void awaitWriterEntered() throws InterruptedException {
			await(writerEntered, "Writer did not enter the repository write path");
		}

		@Override
		public void awaitWriterBlocked() {
			awaitBlocked(writerThread, "Writer", false);
		}

		private <T> T read(RepositoryOperation<T> operation) {
			recordReaderEntry();
			repositoryLock.lock();
			try {
				return operation.execute();
			} finally {
				repositoryLock.unlock();
			}
		}

		private void write(RepositoryMutation mutation) {
			recordWriterEntry();
			repositoryLock.lock();
			try {
				mutation.execute();
			} finally {
				repositoryLock.unlock();
			}
		}

		private void recordReaderEntry() {
			CountDownLatch entered = readerEntered;
			if (entered != null) {
				if (readerThread == null) {
					readerThread = Thread.currentThread();
					readerEntryCount = 1;
					entered.countDown();
				} else if (readerThread == Thread.currentThread()) {
					readerEntryCount++;
				}
			}
		}

		private void recordWriterEntry() {
			CountDownLatch entered = writerEntered;
			if (entered != null) {
				if (writerThread == null) {
					writerThread = Thread.currentThread();
					writerEntryCount = 1;
					entered.countDown();
				} else if (writerThread == Thread.currentThread()) {
					writerEntryCount++;
				}
			}
		}

		private void awaitBlocked(Thread thread, String actor, boolean reader) {
			if (thread == null) {
				throw new AssertionError(actor + " entry was not recorded");
			}
			if (entryCount(reader) != 1) {
				throw new AssertionError(actor + " reached another repository path before blocking");
			}
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5L);
			while (!repositoryLock.hasQueuedThread(thread)) {
				if (entryCount(reader) != 1) {
					throw new AssertionError(actor + " reached another repository path before blocking");
				}
				if (!thread.isAlive()) {
					throw new AssertionError(actor + " returned before blocking on the repository lock");
				}
				if (System.nanoTime() >= deadline) {
					throw new AssertionError(actor + " did not block on the repository lock");
				}
				Thread.yield();
			}
			if (entryCount(reader) != 1) {
				throw new AssertionError(actor + " reached another repository path before blocking");
			}
		}

		private int entryCount(boolean reader) {
			return reader ? readerEntryCount : writerEntryCount;
		}

		private void await(CountDownLatch latch, String failureMessage) throws InterruptedException {
			if (latch == null || !latch.await(5, TimeUnit.SECONDS)) {
				throw new AssertionError(failureMessage);
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

		private void afterCompensationStartStep(String step) {
			if (step.equals(failCompensationStartAt)) {
				failCompensationStartAt = null;
				throw new IllegalStateException("Compensation start failed after " + step);
			}
		}

		private interface RepositoryOperation<T> {
			T execute();
		}

		private interface RepositoryMutation {
			void execute();
		}

		private static final class VectorStoreOwnership {
			private final String vectorStoreKey;
			private final boolean metadataFilterSupported;

			private VectorStoreOwnership(String vectorStoreKey, boolean metadataFilterSupported) {
				this.vectorStoreKey = vectorStoreKey;
				this.metadataFilterSupported = metadataFilterSupported;
			}
		}
	}
}
