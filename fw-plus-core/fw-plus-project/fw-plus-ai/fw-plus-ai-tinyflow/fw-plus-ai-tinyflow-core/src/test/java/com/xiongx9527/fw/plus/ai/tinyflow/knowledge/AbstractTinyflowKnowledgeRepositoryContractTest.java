package com.xiongx9527.fw.plus.ai.tinyflow.knowledge;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public abstract class AbstractTinyflowKnowledgeRepositoryContractTest {

	protected TinyflowKnowledgeRepository repository;

	protected abstract TinyflowKnowledgeRepository createRepository();

	protected abstract void failNextCompletionAt(TinyflowKnowledgeRepository target, String step);

	protected abstract void failNextCompensationStartAt(TinyflowKnowledgeRepository target, String step);

	protected abstract RepositoryConcurrencyProbe concurrencyProbe(TinyflowKnowledgeRepository target);

	protected interface RepositoryConcurrencyProbe {
		void pauseNextCompletionAfter(String step);
		void awaitCompletionPaused() throws InterruptedException;
		void releaseCompletion();
		void expectReaderEntry();
		void awaitReaderEntered() throws InterruptedException;
		void awaitReaderBlocked();
		void expectWriterEntry();
		void awaitWriterEntered() throws InterruptedException;
		void awaitWriterBlocked();
	}

	@Before
	public void setUpRepository() {
		repository = createRepository();
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
	public void shouldKeepPreviousActiveVersionWhenSwitchTargetIsInvalid() {
		repository.saveDocument(document("tenant-a", "kb-1", "source-1", "doc-active", "sum-active"));
		repository.saveDocument(document("tenant-a", "kb-1", "source-2", "doc-source", "sum-source"));
		repository.saveDocument(document("tenant-a", "kb-2", "source-1", "doc-knowledge", "sum-knowledge"));
		repository.saveDocument(document("tenant-b", "kb-1", "source-1", "doc-tenant", "sum-tenant"));
		repository.activateDocumentVersion("tenant-a", "kb-1", "source-1", "doc-active");

		assertSwitchFailsAndActiveRemains("tenant-a", "kb-1", "source-1", "missing");
		assertSwitchFailsAndActiveRemains("tenant-a", "kb-1", "source-1", "doc-source");
		assertSwitchFailsAndActiveRemains("tenant-a", "kb-1", "source-1", "doc-knowledge");
		assertSwitchFailsAndActiveRemains("tenant-a", "kb-1", "source-1", "doc-tenant");
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
	public void shouldDeleteKnowledgeWithExpectedVersionAndRejectStaleDelete() {
		TinyflowKnowledgeDefinition initial = knowledge("tenant-a", "kb-delete");
		repository.saveKnowledge(initial, 0L);
		long staleVersion = repository.findKnowledgeVersion("tenant-a", "kb-delete");
		TinyflowKnowledgeDefinition concurrent = TinyflowKnowledgeDefinition.builder()
				.providerKey("local").tenantId("tenant-a").knowledgeId("kb-delete")
				.name("Concurrent update").description("must survive stale delete").build();
		repository.saveKnowledge(concurrent, staleVersion);

		assertThrows(TinyflowKnowledgeRepository.OptimisticLockException.class,
				() -> repository.deleteKnowledge("tenant-a", "kb-delete", staleVersion));
		assertEquals("Concurrent update", repository.findKnowledge("tenant-a", "kb-delete").getName());
		long currentVersion = repository.findKnowledgeVersion("tenant-a", "kb-delete");
		repository.deleteKnowledge("tenant-a", "kb-delete", currentVersion);

		assertNull(repository.findKnowledge("tenant-a", "kb-delete"));
		assertEquals(0L, repository.findKnowledgeVersion("tenant-a", "kb-delete"));
	}

	@Test
	public void shouldRejectExclusiveVectorStoreOwnershipAcrossTenants() {
		repository.saveKnowledgeWithVectorStoreOwnership(
				knowledge("tenant-a", "kb-owner"), 0L, "store-exclusive", false);

		assertThrows(TinyflowKnowledgeRepository.VectorStoreOwnershipException.class,
				() -> repository.saveKnowledgeWithVectorStoreOwnership(
						knowledge("tenant-b", "kb-contender"), 0L, "store-exclusive", false));
		assertNotNull(repository.findKnowledge("tenant-a", "kb-owner"));
		assertNull(repository.findKnowledge("tenant-b", "kb-contender"));
	}

	@Test
	public void shouldAtomicallyGrantExclusiveVectorStoreToOnlyOneConcurrentOwner() throws Exception {
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch finished = new CountDownLatch(2);
		AtomicInteger successes = new AtomicInteger();
		AtomicInteger conflicts = new AtomicInteger();
		AtomicReference<Throwable> unexpected = new AtomicReference<Throwable>();
		Runnable tenantA = ownershipAttempt(start, finished, successes, conflicts, unexpected,
				"tenant-a", "kb-a", "store-race");
		Runnable tenantB = ownershipAttempt(start, finished, successes, conflicts, unexpected,
				"tenant-b", "kb-b", "store-race");
		new Thread(tenantA, "tinyflow-store-owner-a").start();
		new Thread(tenantB, "tinyflow-store-owner-b").start();

		start.countDown();
		assertTrue(finished.await(5, TimeUnit.SECONDS));

		assertNull(unexpected.get());
		assertEquals(1, successes.get());
		assertEquals(1, conflicts.get());
		assertTrue((repository.findKnowledge("tenant-a", "kb-a") != null)
				^ (repository.findKnowledge("tenant-b", "kb-b") != null));
	}

	private Runnable ownershipAttempt(CountDownLatch start, CountDownLatch finished,
			AtomicInteger successes, AtomicInteger conflicts, AtomicReference<Throwable> unexpected,
			String tenantId, String knowledgeId, String storeKey) {
		return () -> {
			try {
				start.await();
				repository.saveKnowledgeWithVectorStoreOwnership(
						knowledge(tenantId, knowledgeId), 0L, storeKey, false);
				successes.incrementAndGet();
			} catch (TinyflowKnowledgeRepository.VectorStoreOwnershipException expected) {
				conflicts.incrementAndGet();
			} catch (Throwable throwable) {
				unexpected.compareAndSet(null, throwable);
			} finally {
				finished.countDown();
			}
		};
	}

	@Test
	public void shouldDeleteOnlyRequestedSourceVersionsWithinTenantAndKnowledge() {
		repository.saveDocument(document("tenant-a", "kb-1", "source-1", "doc-a1", "sum-a1"));
		repository.saveDocument(document("tenant-a", "kb-1", "source-1", "doc-a2", "sum-a2"));
		repository.saveDocument(document("tenant-a", "kb-1", "source-2", "doc-a3", "sum-a3"));
		repository.saveDocument(document("tenant-b", "kb-1", "source-1", "doc-b1", "sum-b1"));
		repository.saveChunks(Arrays.asList(
				chunk("tenant-a", "kb-1", "doc-a1", "chunk-a1", "a1", true, "vec-a1"),
				chunk("tenant-a", "kb-1", "doc-a2", "chunk-a2", "a2", true, "vec-a2"),
				chunk("tenant-b", "kb-1", "doc-b1", "chunk-b1", "b1", true, "vec-b1")));

		assertEquals(2, repository.findDocumentVersions("tenant-a", "kb-1", "source-1").size());
		repository.deleteDocumentsBySource("tenant-a", "kb-1", "source-1");
		assertTrue(repository.findDocumentVersions("tenant-a", "kb-1", "source-1").isEmpty());
		assertTrue(repository.findChunksByDocument("tenant-a", "kb-1", "doc-a1").isEmpty());
		assertTrue(repository.findChunksByDocument("tenant-a", "kb-1", "doc-a2").isEmpty());
		assertEquals(1, repository.findDocumentVersions("tenant-a", "kb-1", "source-2").size());
		assertEquals(1, repository.findDocumentVersions("tenant-b", "kb-1", "source-1").size());
		assertEquals(1, repository.findChunksByDocument("tenant-b", "kb-1", "doc-b1").size());
	}

	@Test
	public void shouldDeleteOnlySpecifiedInactiveDocumentVersionAndItsChunks() {
		repository.saveDocument(document("tenant-a", "kb-1", "source-1", "doc-old", "sum-old"));
		repository.saveDocument(document("tenant-a", "kb-1", "source-1", "doc-active", "sum-active"));
		repository.saveChunks(Arrays.asList(
				chunk("tenant-a", "kb-1", "doc-old", "chunk-old", "old", true, "vec-old"),
				chunk("tenant-a", "kb-1", "doc-active", "chunk-active", "active", true, "vec-active")));
		repository.activateDocumentVersion("tenant-a", "kb-1", "source-1", "doc-active");

		repository.deleteDocumentVersion("tenant-a", "kb-1", "source-1", "doc-old");
		repository.deleteDocumentVersion("tenant-a", "kb-1", "source-1", "doc-old");

		assertTrue(repository.findChunksByDocument("tenant-a", "kb-1", "doc-old").isEmpty());
		assertNull(repository.findBySourceAndChecksum("tenant-a", "kb-1", "source-1", "sum-old"));
		assertEquals("doc-active", repository.findActiveDocument(
				"tenant-a", "kb-1", "source-1").getDocumentId());
		assertEquals(1, repository.findChunksByDocument("tenant-a", "kb-1", "doc-active").size());
	}

	@Test
	public void shouldIsolateChunkAndDocumentIdsAcrossKnowledgeWithinTenant() {
		repository.saveChunks(Arrays.asList(
				chunk("tenant-a", "kb-1", "doc-shared", "chunk-shared", "knowledge-one", true, "vec-1"),
				chunk("tenant-a", "kb-2", "doc-shared", "chunk-shared", "knowledge-two", true, "vec-2")));

		assertEquals("knowledge-one", repository.findChunk("tenant-a", "kb-1", "chunk-shared").getContent());
		assertEquals("knowledge-two", repository.findChunk("tenant-a", "kb-2", "chunk-shared").getContent());
		assertEquals("knowledge-one",
				repository.findChunksByDocument("tenant-a", "kb-1", "doc-shared").get(0).getContent());
		assertEquals("knowledge-two",
				repository.findChunksByDocument("tenant-a", "kb-2", "doc-shared").get(0).getContent());
	}

	@Test
	public void shouldPersistAndResolveVectorIdsWithinTenantAndKnowledge() {
		repository.saveChunks(Arrays.asList(
				chunk("tenant-a", "kb-1", "doc-1", "chunk-1", "old", true, "vec-1", "vec-2"),
				chunk("tenant-b", "kb-1", "doc-2", "chunk-2", "other", true, "vec-1")));

		assertEquals(Arrays.asList("vec-1", "vec-2"),
				repository.findChunk("tenant-a", "kb-1", "chunk-1").getVectorIds());
		List<TinyflowChunk> matches = repository.findChunksByVectorIds(
				"tenant-a", "kb-1", Arrays.asList("vec-2", "vec-1"));
		assertEquals(1, matches.size());
		assertEquals("chunk-1", matches.get(0).getChunkId());
	}

	@Test
	public void shouldUpdateChunkWithoutLosingSourceIdentityOrChangingMetadataShape() {
		repository.saveChunks(Collections.singletonList(
				chunk("tenant-a", "kb-1", "doc-1", "chunk-1", "old", true, "vec-1")));
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		metadata.put("page", 2);
		repository.updateChunk("tenant-a", "kb-1", "chunk-1", false, "new", metadata);

		TinyflowChunk updated = repository.findChunk("tenant-a", "kb-1", "chunk-1");
		assertEquals("tenant-a", updated.getTenantId());
		assertEquals("kb-1", updated.getKnowledgeId());
		assertEquals("source-1", updated.getSourceId());
		assertEquals("file", updated.getSourceType());
		assertEquals("doc-1", updated.getDocumentId());
		assertEquals("new", updated.getContent());
		assertFalse(updated.isEnabled());
		assertEquals(Collections.<String, Object>singletonMap("page", 2), updated.getMetadata());
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
	public void shouldAtomicallyAdvanceVectorCompensationStage() {
		TinyflowKnowledgeRepository.VectorCompensation compensation =
				new TinyflowKnowledgeRepository.VectorCompensation(
						"comp-stage", "tenant-a", "kb-1", "source-1", "doc-1",
						Collections.singletonList("vec-1"), "DELETE_PENDING");
		repository.saveVectorCompensation(compensation);

		assertEquals(TinyflowKnowledgeRepository.VectorCompensation.VECTOR_PENDING,
				repository.findVectorCompensations("tenant-a", "kb-1").get(0).getStage());
		repository.advanceVectorCompensationStage("tenant-a", "kb-1", "comp-stage",
				TinyflowKnowledgeRepository.VectorCompensation.VECTOR_PENDING,
				TinyflowKnowledgeRepository.VectorCompensation.RESOURCE_PENDING);
		repository.advanceVectorCompensationStage("tenant-a", "kb-1", "comp-stage",
				TinyflowKnowledgeRepository.VectorCompensation.VECTOR_PENDING,
				TinyflowKnowledgeRepository.VectorCompensation.RESOURCE_PENDING);

		assertEquals(TinyflowKnowledgeRepository.VectorCompensation.RESOURCE_PENDING,
				repository.findVectorCompensations("tenant-a", "kb-1").get(0).getStage());
		assertThrows(TinyflowKnowledgeRepository.CompensationStageException.class,
				() -> repository.advanceVectorCompensationStage("tenant-a", "kb-1", "comp-stage",
						TinyflowKnowledgeRepository.VectorCompensation.VECTOR_PENDING,
						TinyflowKnowledgeRepository.VectorCompensation.TASK_PENDING));
	}

	@Test
	public void shouldAtomicallyPersistPendingTaskAndVectorCompensation() {
		for (String step : Arrays.asList("task", "compensation")) {
			repository = createRepository();
			TinyflowIngestionTask pendingTask = task("task-delete", "tenant-a", "kb-1");
			TinyflowKnowledgeRepository.VectorCompensation compensation =
					new TinyflowKnowledgeRepository.VectorCompensation(
							"comp-delete", "tenant-a", "kb-1", "source-1", "doc-1",
							Collections.singletonList("vec-1"), "DELETE_PENDING");
			failNextCompensationStartAt(repository, step);

			assertThrows(IllegalStateException.class,
					() -> repository.saveTaskWithVectorCompensation(pendingTask, compensation));
			assertTrue(repository.findTasks("tenant-a", "kb-1").isEmpty());
			assertTrue(repository.findVectorCompensations("tenant-a", "kb-1").isEmpty());
		}

		repository = createRepository();
		TinyflowIngestionTask pendingTask = task("task-delete", "tenant-a", "kb-1");
		TinyflowKnowledgeRepository.VectorCompensation compensation =
				new TinyflowKnowledgeRepository.VectorCompensation(
						"comp-delete", "tenant-a", "kb-1", "source-1", "doc-1",
						Collections.singletonList("vec-1"), "DELETE_PENDING");
		repository.saveTaskWithVectorCompensation(pendingTask, compensation);

		assertEquals("task-delete", repository.findTasks("tenant-a", "kb-1").get(0).getTaskId());
		assertEquals("comp-delete", repository.findVectorCompensations(
				"tenant-a", "kb-1").get(0).getCompensationId());
	}

	@Test
	public void shouldListTasksWithinTenantAndKnowledge() {
		repository.saveTask(task("task-a", "tenant-a", "kb-1"));
		repository.saveTask(task("task-b", "tenant-b", "kb-1"));
		assertEquals(1, repository.findTasks("tenant-a", "kb-1").size());
		assertEquals("task-a", repository.findTasks("tenant-a", "kb-1").get(0).getTaskId());
	}

	@Test
	public void shouldAtomicallyCompleteIngestionAndRollbackEveryFailedStep() {
		for (String step : Arrays.asList("chunks", "document", "active", "task", "compensation")) {
			repository = createRepository();
			CompletionFixture fixture = completionFixture();
			prepareCompletion(fixture);
			failNextCompletionAt(repository, step);

			assertThrows(IllegalStateException.class, () -> repository.completeIngestionWithVectorCompensations(
					fixture.completedDocument, fixture.chunks, fixture.completedTask,
					Collections.singletonList(cleanupCompensation())));
			assertEquals("doc-old", repository.findActiveDocument("tenant-a", "kb-atomic", "source-1")
					.getDocumentId());
			assertTrue(repository.findChunksByDocument("tenant-a", "kb-atomic", "doc-new").isEmpty());
			assertTrue(repository.findBySourceAndChecksum(
					"tenant-a", "kb-atomic", "source-1", "sum-new").getChunks().isEmpty());
			assertFalse(hasTaskStatus(repository.findTasks("tenant-a", "kb-atomic"), "COMPLETED"));
			assertTrue(repository.findVectorCompensations("tenant-a", "kb-atomic").isEmpty());
		}

		repository = createRepository();
		CompletionFixture fixture = completionFixture();
		prepareCompletion(fixture);
		repository.completeIngestionWithVectorCompensations(
				fixture.completedDocument, fixture.chunks, fixture.completedTask,
				Collections.singletonList(cleanupCompensation()));

		assertEquals("doc-new", repository.findActiveDocument("tenant-a", "kb-atomic", "source-1")
				.getDocumentId());
		assertEquals("chunk-new", repository.findChunksByDocument(
				"tenant-a", "kb-atomic", "doc-new").get(0).getChunkId());
		assertEquals(1, repository.findBySourceAndChecksum(
				"tenant-a", "kb-atomic", "source-1", "sum-new").getChunks().size());
		assertTrue(hasTaskStatus(repository.findTasks("tenant-a", "kb-atomic"), "COMPLETED"));
		assertEquals("comp-old", repository.findVectorCompensations(
				"tenant-a", "kb-atomic").get(0).getCompensationId());
	}

	@Test
	public void shouldNeverExposePartiallyCompletedIngestionToConcurrentReaders() throws Exception {
		CompletionFixture fixture = completionFixture();
		prepareCompletion(fixture);
		RepositoryConcurrencyProbe probe = concurrencyProbe(repository);
		probe.pauseNextCompletionAfter("chunks");
		AtomicReference<Throwable> completionFailure = new AtomicReference<Throwable>();
		CountDownLatch completionDone = new CountDownLatch(1);
		Thread completion = new Thread(() -> {
			try {
				repository.completeIngestion(fixture.completedDocument, fixture.chunks, fixture.completedTask);
			} catch (Throwable throwable) {
				completionFailure.set(throwable);
			} finally {
				completionDone.countDown();
			}
		}, "tinyflow-repository-completion");
		completion.start();

		AtomicReference<CompletionObservation> observation = new AtomicReference<CompletionObservation>();
		AtomicReference<Throwable> readerFailure = new AtomicReference<Throwable>();
		CountDownLatch readerDone = new CountDownLatch(1);
		Thread reader = new Thread(() -> {
			try {
				observation.set(observeCompletion(repository));
			} catch (Throwable throwable) {
				readerFailure.set(throwable);
			} finally {
				readerDone.countDown();
			}
		}, "tinyflow-repository-reader");
		try {
			probe.awaitCompletionPaused();
			probe.expectReaderEntry();
			reader.start();
			probe.awaitReaderEntered();
			probe.awaitReaderBlocked();
			assertEquals("Reader returned while completion was paused", 1L, readerDone.getCount());
		} finally {
			probe.releaseCompletion();
		}

		assertTrue(completionDone.await(5, TimeUnit.SECONDS));
		assertTrue(readerDone.await(5, TimeUnit.SECONDS));
		assertNull(completionFailure.get());
		assertNull(readerFailure.get());
		assertNewCompletionObservation(observation.get());
		assertNewCompletionObservation(observeCompletion(repository));
	}

	@Test
	public void shouldRestoreExactRepositorySnapshotAfterEveryFailedCompletionStep() {
		for (String step : Arrays.asList("chunks", "document", "active", "task")) {
			repository = createRepository();
			CompletionFixture fixture = completionFixture();
			prepareCompletion(fixture);
			prepareRollbackSnapshotData();
			RepositorySnapshot before = repositorySnapshot(repository);
			failNextCompletionAt(repository, step);

			assertThrows(IllegalStateException.class, () -> repository.completeIngestion(
					fixture.completedDocument, fixture.chunks, fixture.completedTask));

			assertRepositorySnapshotEquals(before, repositorySnapshot(repository));
		}
	}

	@Test
	public void shouldPreserveConcurrentWriterAfterFailedCompletionRollback() throws Exception {
		CompletionFixture fixture = completionFixture();
		prepareCompletion(fixture);
		RepositoryConcurrencyProbe probe = concurrencyProbe(repository);
		probe.pauseNextCompletionAfter("chunks");
		failNextCompletionAt(repository, "document");
		AtomicReference<Throwable> completionFailure = new AtomicReference<Throwable>();
		CountDownLatch completionDone = new CountDownLatch(1);
		Thread completion = new Thread(() -> {
			try {
				repository.completeIngestion(fixture.completedDocument, fixture.chunks, fixture.completedTask);
			} catch (Throwable throwable) {
				completionFailure.set(throwable);
			} finally {
				completionDone.countDown();
			}
		}, "tinyflow-repository-failing-completion");
		completion.start();

		TinyflowChunk writerChunk = chunk(
				"tenant-a", "kb-atomic", "doc-writer", "chunk-writer", "writer", true, "vec-writer");
		TinyflowIngestionTask writerTask = TinyflowIngestionTask.builder().taskId("task-writer")
				.providerKey("local").tenantId("tenant-a").knowledgeId("kb-atomic")
				.documentId("doc-writer").status("PENDING").build();
		AtomicReference<Throwable> writerFailure = new AtomicReference<Throwable>();
		CountDownLatch writerDone = new CountDownLatch(1);
		Thread writer = new Thread(() -> {
			try {
				repository.saveChunks(Collections.singletonList(writerChunk));
				repository.saveTask(writerTask);
			} catch (Throwable throwable) {
				writerFailure.set(throwable);
			} finally {
				writerDone.countDown();
			}
		}, "tinyflow-repository-writer");
		try {
			probe.awaitCompletionPaused();
			probe.expectWriterEntry();
			writer.start();
			probe.awaitWriterEntered();
			probe.awaitWriterBlocked();
			assertEquals("Writer returned while rollback was pending", 1L, writerDone.getCount());
		} finally {
			probe.releaseCompletion();
		}

		assertTrue(completionDone.await(5, TimeUnit.SECONDS));
		assertTrue(writerDone.await(5, TimeUnit.SECONDS));
		assertTrue(completionFailure.get() instanceof IllegalStateException);
		assertNull(writerFailure.get());
		TinyflowChunk persistedWriterChunk = repository.findChunk(
				"tenant-a", "kb-atomic", "chunk-writer");
		assertNotNull(persistedWriterChunk);
		assertEquals("writer", persistedWriterChunk.getContent());
		assertTrue(hasTaskId(repository.findTasks("tenant-a", "kb-atomic"), "task-writer"));
	}

	private CompletionObservation observeCompletion(TinyflowKnowledgeRepository target) {
		boolean hasNewChunk = !target.findChunksByDocument(
				"tenant-a", "kb-atomic", "doc-new").isEmpty();
		TinyflowDocument document = target.findBySourceAndChecksum(
				"tenant-a", "kb-atomic", "source-1", "sum-new");
		TinyflowDocument active = target.findActiveDocument("tenant-a", "kb-atomic", "source-1");
		boolean hasCompletedTask = hasTaskStatus(target.findTasks("tenant-a", "kb-atomic"), "COMPLETED");
		return new CompletionObservation(hasNewChunk, document.getChunks().size(),
				active.getDocumentId(), hasCompletedTask);
	}

	private void assertNewCompletionObservation(CompletionObservation observation) {
		assertTrue(observation.hasNewChunk);
		assertEquals(1, observation.documentChunkCount);
		assertEquals("doc-new", observation.activeDocumentId);
		assertTrue(observation.hasCompletedTask);
	}

	private RepositorySnapshot repositorySnapshot(TinyflowKnowledgeRepository target) {
		Map<String, List<Object>> knowledge = new LinkedHashMap<String, List<Object>>();
		Map<String, Long> knowledgeVersions = new LinkedHashMap<String, Long>();
		Map<String, String> activeDocuments = new LinkedHashMap<String, String>();
		Map<String, List<Object>> documents = new LinkedHashMap<String, List<Object>>();
		Map<String, List<Object>> chunks = new LinkedHashMap<String, List<Object>>();
		Map<List<Object>, Integer> tasks = new LinkedHashMap<List<Object>, Integer>();
		for (RepositoryScope scope : snapshotScopes()) {
			String knowledgeKey = scope.tenantId + "|" + scope.knowledgeId;
			if (!knowledge.containsKey(knowledgeKey)) {
				TinyflowKnowledgeDefinition definition = target.findKnowledge(scope.tenantId, scope.knowledgeId);
				knowledge.put(knowledgeKey, definition == null ? null : knowledgeSignature(definition));
				knowledgeVersions.put(knowledgeKey,
						target.findKnowledgeVersion(scope.tenantId, scope.knowledgeId));
				for (TinyflowIngestionTask task : target.findTasks(scope.tenantId, scope.knowledgeId)) {
					increment(tasks, taskSignature(task));
				}
			}
			TinyflowDocument active = target.findActiveDocument(
					scope.tenantId, scope.knowledgeId, scope.sourceId);
			activeDocuments.put(scope.key(), active == null ? null : active.getDocumentId());
			for (TinyflowDocument document : target.findDocumentVersions(
					scope.tenantId, scope.knowledgeId, scope.sourceId)) {
				String documentKey = knowledgeKey + "|" + document.getDocumentId();
				documents.put(documentKey, documentSignature(document));
				for (TinyflowChunk chunk : target.findChunksByDocument(
						scope.tenantId, scope.knowledgeId, document.getDocumentId())) {
					chunks.put(documentKey + "|" + chunk.getChunkId(), chunkSignature(chunk));
				}
			}
		}
		return new RepositorySnapshot(knowledge, knowledgeVersions, activeDocuments, documents, chunks, tasks);
	}

	private List<Object> knowledgeSignature(TinyflowKnowledgeDefinition definition) {
		return Arrays.<Object>asList(definition.getProviderKey(), definition.getTenantId(),
				definition.getKnowledgeId(), definition.getName(), definition.getDescription(),
				definition.getMetadata(), definition.isEnabled());
	}

	private List<Object> documentSignature(TinyflowDocument document) {
		List<List<Object>> chunkSignatures = new ArrayList<List<Object>>();
		for (TinyflowChunk chunk : document.getChunks()) {
			chunkSignatures.add(chunkSignature(chunk));
		}
		return Arrays.<Object>asList(document.getTenantId(), document.getKnowledgeId(), document.getSourceId(),
				document.getSourceType(), document.getDocumentId(), document.getChecksum(), document.getName(),
				document.getContent(), document.getSourceUri(), document.getSourceVersion(), document.getMetadata(),
				chunkSignatures);
	}

	private List<Object> chunkSignature(TinyflowChunk chunk) {
		return Arrays.<Object>asList(chunk.getTenantId(), chunk.getKnowledgeId(), chunk.getSourceId(),
				chunk.getSourceType(), chunk.getDocumentId(), chunk.getChunkId(), chunk.getContent(),
				chunk.getSourceUri(), chunk.getSourceVersion(), chunk.getLocation(), chunk.getPosition(),
				chunk.getParentChunkId(), chunk.getTokenCount(), chunk.getMetadata(), chunk.isEnabled(),
				chunk.getVectorIds());
	}

	private List<Object> taskSignature(TinyflowIngestionTask task) {
		return Arrays.<Object>asList(task.getTaskId(), task.getProviderKey(), task.getTenantId(),
				task.getKnowledgeId(), task.getDocumentId(), task.getStatus(), task.getMessage(), task.getMetadata());
	}

	private void increment(Map<List<Object>, Integer> values, List<Object> signature) {
		Integer count = values.get(signature);
		values.put(signature, count == null ? 1 : count + 1);
	}

	private void assertRepositorySnapshotEquals(RepositorySnapshot expected, RepositorySnapshot actual) {
		assertEquals(expected.knowledge, actual.knowledge);
		assertEquals(expected.knowledgeVersions, actual.knowledgeVersions);
		assertEquals(expected.activeDocuments, actual.activeDocuments);
		assertEquals(expected.documents, actual.documents);
		assertEquals(expected.chunks, actual.chunks);
		assertEquals(expected.tasks, actual.tasks);
	}

	private boolean hasTaskId(List<TinyflowIngestionTask> tasks, String taskId) {
		for (TinyflowIngestionTask task : tasks) {
			if (taskId.equals(task.getTaskId())) {
				return true;
			}
		}
		return false;
	}

	private CompletionFixture completionFixture() {
		TinyflowChunk chunk = richChunk("tenant-a", "kb-atomic", "source-1", "doc-new", "chunk-new", "new");
		TinyflowDocument completed = TinyflowDocument.builder().tenantId("tenant-a").knowledgeId("kb-atomic")
				.sourceId("source-1").sourceType("file").documentId("doc-new").checksum("sum-new")
				.name("new").content("new-content").sourceUri("s3://atomic/new")
				.sourceVersion("source-version-new").metadata(metadata("document", "completed"))
				.chunks(Collections.singletonList(chunk)).build();
		TinyflowIngestionTask task = TinyflowIngestionTask.builder().taskId("task-new").providerKey("local")
				.tenantId("tenant-a").knowledgeId("kb-atomic").documentId("doc-new")
				.status("COMPLETED").message("completed").metadata(metadata("task", "completed")).build();
		return new CompletionFixture(completed, Collections.singletonList(chunk), task);
	}

	private TinyflowKnowledgeRepository.VectorCompensation cleanupCompensation() {
		return new TinyflowKnowledgeRepository.VectorCompensation(
				"comp-old", "tenant-a", "kb-atomic", "source-1", "doc-old",
				Collections.singletonList("vector-old"), "DELETE_OLD_VERSION");
	}

	private void prepareCompletion(CompletionFixture fixture) {
		repository.saveDocument(richDocument(
				"tenant-a", "kb-atomic", "source-1", "doc-old", "sum-old", "old"));
		repository.activateDocumentVersion("tenant-a", "kb-atomic", "source-1", "doc-old");
		repository.saveDocument(TinyflowDocument.builder().tenantId("tenant-a").knowledgeId("kb-atomic")
				.sourceId("source-1").sourceType("file-pending").documentId("doc-new").checksum("sum-new")
				.name("name-pending").content("content-pending").sourceUri("s3://pending")
				.sourceVersion("version-pending").metadata(metadata("document", "pending"))
				.chunks(Collections.<TinyflowChunk>emptyList()).build());
		repository.saveTask(TinyflowIngestionTask.builder().taskId("task-new").providerKey("local")
				.tenantId("tenant-a").knowledgeId("kb-atomic").documentId("doc-new")
				.status("PENDING").message("pending").metadata(metadata("task", "pending")).build());
	}

	private void prepareRollbackSnapshotData() {
		repository.saveKnowledge(TinyflowKnowledgeDefinition.builder().providerKey("local")
				.tenantId("tenant-a").knowledgeId("kb-atomic").name("Atomic knowledge")
				.description("must be restored exactly").metadata(metadata("scope", "atomic"))
				.enabled(false).build(), 0L);
		repository.saveKnowledge(TinyflowKnowledgeDefinition.builder().providerKey("remote")
				.tenantId("tenant-b").knowledgeId("kb-unrelated").name("Unrelated knowledge")
				.description("must remain untouched").metadata(metadata("scope", "unrelated"))
				.enabled(true).build(), 0L);
		repository.saveDocument(richDocument(
				"tenant-a", "kb-atomic", "source-extra", "doc-extra", "sum-extra", "extra"));
		repository.activateDocumentVersion("tenant-a", "kb-atomic", "source-extra", "doc-extra");
		repository.saveDocument(richDocument(
				"tenant-b", "kb-unrelated", "source-unrelated", "doc-unrelated", "sum-unrelated", "unrelated"));
		repository.activateDocumentVersion(
				"tenant-b", "kb-unrelated", "source-unrelated", "doc-unrelated");
		repository.saveChunks(Arrays.asList(
				richChunk("tenant-a", "kb-atomic", "source-1", "doc-old", "chunk-old", "old"),
				richChunk("tenant-a", "kb-atomic", "source-1", "doc-new", "chunk-pending", "pending"),
				richChunk("tenant-a", "kb-atomic", "source-extra", "doc-extra", "chunk-extra", "extra"),
				richChunk("tenant-b", "kb-unrelated", "source-unrelated",
						"doc-unrelated", "chunk-unrelated", "unrelated")));
		repository.saveTask(richTask("task-history", "tenant-a", "kb-atomic", "doc-old", "FAILED"));
		repository.saveTask(richTask("task-extra", "tenant-a", "kb-atomic", "doc-extra", "COMPLETED"));
		repository.saveTask(richTask(
				"task-unrelated", "tenant-b", "kb-unrelated", "doc-unrelated", "COMPLETED"));
	}

	private TinyflowDocument richDocument(String tenantId, String knowledgeId, String sourceId,
			String documentId, String checksum, String marker) {
		TinyflowChunk embeddedChunk = richChunk(
				tenantId, knowledgeId, sourceId, documentId, "embedded-" + marker, "embedded-" + marker);
		return TinyflowDocument.builder().tenantId(tenantId).knowledgeId(knowledgeId).sourceId(sourceId)
				.sourceType("file-" + marker).documentId(documentId).checksum(checksum).name("name-" + marker)
				.content("content-" + marker).sourceUri("s3://" + marker).sourceVersion("version-" + marker)
				.metadata(metadata("document", marker)).chunks(Collections.singletonList(embeddedChunk)).build();
	}

	private TinyflowChunk richChunk(String tenantId, String knowledgeId, String sourceId,
			String documentId, String chunkId, String marker) {
		return TinyflowChunk.builder().tenantId(tenantId).knowledgeId(knowledgeId).sourceId(sourceId)
				.sourceType("file-" + marker).documentId(documentId).chunkId(chunkId).content("content-" + marker)
				.sourceUri("s3://" + marker).sourceVersion("version-" + marker).location("page:" + marker)
				.position(marker.length()).parentChunkId("parent-" + marker).tokenCount(marker.length() + 10)
				.metadata(metadata("chunk", marker)).enabled(false)
				.vectorIds(Arrays.asList("vector-" + marker, "vector-" + marker + "-2")).build();
	}

	private TinyflowIngestionTask richTask(String taskId, String tenantId, String knowledgeId,
			String documentId, String status) {
		return TinyflowIngestionTask.builder().taskId(taskId).providerKey("local-" + taskId)
				.tenantId(tenantId).knowledgeId(knowledgeId).documentId(documentId).status(status)
				.message("message-" + taskId).metadata(metadata("task", taskId)).build();
	}

	private Map<String, Object> metadata(String key, String value) {
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		metadata.put(key, value);
		metadata.put("labels", Arrays.asList(value + "-one", value + "-two"));
		return metadata;
	}

	private List<RepositoryScope> snapshotScopes() {
		return Arrays.asList(
				new RepositoryScope("tenant-a", "kb-atomic", "source-1"),
				new RepositoryScope("tenant-a", "kb-atomic", "source-extra"),
				new RepositoryScope("tenant-b", "kb-unrelated", "source-unrelated"));
	}

	private boolean hasTaskStatus(List<TinyflowIngestionTask> tasks, String status) {
		for (TinyflowIngestionTask task : tasks) {
			if (status.equals(task.getStatus())) {
				return true;
			}
		}
		return false;
	}

	private static final class CompletionObservation {
		private final boolean hasNewChunk;
		private final int documentChunkCount;
		private final String activeDocumentId;
		private final boolean hasCompletedTask;

		private CompletionObservation(boolean hasNewChunk, int documentChunkCount,
				String activeDocumentId, boolean hasCompletedTask) {
			this.hasNewChunk = hasNewChunk;
			this.documentChunkCount = documentChunkCount;
			this.activeDocumentId = activeDocumentId;
			this.hasCompletedTask = hasCompletedTask;
		}
	}

	private static final class RepositorySnapshot {
		private final Map<String, List<Object>> knowledge;
		private final Map<String, Long> knowledgeVersions;
		private final Map<String, String> activeDocuments;
		private final Map<String, List<Object>> documents;
		private final Map<String, List<Object>> chunks;
		private final Map<List<Object>, Integer> tasks;

		private RepositorySnapshot(Map<String, List<Object>> knowledge, Map<String, Long> knowledgeVersions,
				Map<String, String> activeDocuments, Map<String, List<Object>> documents,
				Map<String, List<Object>> chunks, Map<List<Object>, Integer> tasks) {
			this.knowledge = knowledge;
			this.knowledgeVersions = knowledgeVersions;
			this.activeDocuments = activeDocuments;
			this.documents = documents;
			this.chunks = chunks;
			this.tasks = tasks;
		}
	}

	private static final class RepositoryScope {
		private final String tenantId;
		private final String knowledgeId;
		private final String sourceId;

		private RepositoryScope(String tenantId, String knowledgeId, String sourceId) {
			this.tenantId = tenantId;
			this.knowledgeId = knowledgeId;
			this.sourceId = sourceId;
		}

		private String key() {
			return tenantId + "|" + knowledgeId + "|" + sourceId;
		}
	}

	private static final class CompletionFixture {
		private final TinyflowDocument completedDocument;
		private final List<TinyflowChunk> chunks;
		private final TinyflowIngestionTask completedTask;

		private CompletionFixture(TinyflowDocument completedDocument, List<TinyflowChunk> chunks,
				TinyflowIngestionTask completedTask) {
			this.completedDocument = completedDocument;
			this.chunks = chunks;
			this.completedTask = completedTask;
		}
	}

	private void assertSwitchFailsAndActiveRemains(String tenantId, String knowledgeId,
			String sourceId, String documentId) {
		assertThrows(IllegalArgumentException.class,
				() -> repository.activateDocumentVersion(tenantId, knowledgeId, sourceId, documentId));
		assertEquals("doc-active", repository.findActiveDocument(tenantId, knowledgeId, sourceId).getDocumentId());
	}

	protected TinyflowKnowledgeDefinition knowledge(String tenantId, String knowledgeId) {
		return TinyflowKnowledgeDefinition.builder().providerKey("local").tenantId(tenantId)
				.knowledgeId(knowledgeId).name("Knowledge").build();
	}

	protected TinyflowDocument document(String tenantId, String knowledgeId, String sourceId,
			String documentId, String checksum) {
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		metadata.put("checksum", "metadata-must-not-control-identity");
		return TinyflowDocument.builder().tenantId(tenantId).knowledgeId(knowledgeId).sourceId(sourceId)
				.sourceType("file").documentId(documentId).checksum(checksum).name(documentId)
				.metadata(metadata).build();
	}

	protected TinyflowChunk chunk(String tenantId, String knowledgeId, String documentId,
			String chunkId, String content, boolean enabled, String... vectorIds) {
		return TinyflowChunk.builder().tenantId(tenantId).knowledgeId(knowledgeId).sourceId("source-1")
				.sourceType("file").documentId(documentId).chunkId(chunkId).content(content)
				.enabled(enabled).vectorIds(Arrays.asList(vectorIds)).build();
	}

	protected TinyflowIngestionTask task(String taskId, String tenantId, String knowledgeId) {
		return TinyflowIngestionTask.builder().taskId(taskId).providerKey("local").tenantId(tenantId)
				.knowledgeId(knowledgeId).documentId("doc-1").status("pending").build();
	}
}
