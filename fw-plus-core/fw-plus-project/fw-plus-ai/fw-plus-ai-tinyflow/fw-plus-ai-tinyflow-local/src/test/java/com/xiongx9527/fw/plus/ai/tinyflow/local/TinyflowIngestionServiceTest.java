package com.xiongx9527.fw.plus.ai.tinyflow.local;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowChunk;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowChunkStrategy;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDocument;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDocumentImport;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDocumentParser;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowIngestionTask;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeDefinition;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeMatch;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowObjectStorage;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowParsedDocument;
import com.xiongx9527.fw.plus.ai.tinyflow.local.chunk.NormalChunkStrategy;
import com.xiongx9527.fw.plus.ai.tinyflow.local.index.TinyflowVectorCompensator;
import com.xiongx9527.fw.plus.ai.tinyflow.local.parser.CompositeTinyflowDocumentParser;
import com.xiongx9527.fw.plus.ai.tinyflow.local.retrieval.TinyflowLocalRetriever;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowEmbeddingModel;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowModelDescriptor;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowModelProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStore;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStoreDescriptor;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStoreProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

public class TinyflowIngestionServiceTest {

	private InMemoryRepository repository;
	private InMemoryObjectStorage storage;
	private RecordingIndex index;
	private DefaultTinyflowIngestionService service;

	@Before
	public void setUp() {
		repository = new InMemoryRepository();
		storage = new InMemoryObjectStorage();
		index = new RecordingIndex(repository);
		service = service(Runnable::run, CompositeTinyflowDocumentParser.defaults());
	}

	@Test
	public void shouldSkipSameSourceAndChecksum() {
		storage.values.put("object-1", "same fixture".getBytes(StandardCharsets.UTF_8));

		TinyflowIngestionTask first = service.importDocument(fileImport("source-1", "sha256-a", null, "object-1"));
		TinyflowIngestionTask second = service.importDocument(fileImport("source-1", "sha256-a", null, "object-1"));

		assertEquals(first.getDocumentId(), second.getDocumentId());
		assertEquals("COMPLETED", second.getStatus());
		assertEquals(1, index.embeddingCalls.get());
		assertEquals(1, index.indexCalls.get());
	}

	@Test
	public void shouldSwitchAndDeleteSupersededDocumentVersionAfterSuccessfulImport() {
		service.importDocument(fileImport("source-1", "sha256-old", "old content", null));
		TinyflowDocument v1 = repository.findActiveDocument("tenant-a", "kb-1", "source-1");
		List<String> oldVectorIds = repository.findChunksByDocument("tenant-a", "kb-1", v1.getDocumentId())
				.get(0).getVectorIds();

		TinyflowIngestionTask task = service.importDocument(
				fileImport("source-1", "sha256-new", "new content", null));
		TinyflowDocument v2 = repository.findActiveDocument("tenant-a", "kb-1", "source-1");

		assertEquals("COMPLETED", latest(task.getTaskId()).getStatus());
		assertEquals(v1.getSourceId(), v2.getSourceId());
		assertNotEquals(v1.getDocumentId(), v2.getDocumentId());
		assertEquals(Collections.singletonList(v2),
				repository.findDocumentVersions("tenant-a", "kb-1", "source-1"));
		assertTrue(repository.findChunksByDocument("tenant-a", "kb-1", v1.getDocumentId()).isEmpty());
		assertEquals(oldVectorIds, index.lastDeletedVectorIds);
		assertTrue(repository.compensations.isEmpty());
	}

	@Test
	public void shouldKeepNewVersionActiveAndRetainCleanupWhenOldVectorDeletionFails() {
		service.importDocument(fileImport("source-cleanup", "sha256-old", "old content", null));
		TinyflowDocument oldVersion = repository.findActiveDocument(
				"tenant-a", "kb-1", "source-cleanup");
		List<String> oldVectorIds = repository.findChunksByDocument(
				"tenant-a", "kb-1", oldVersion.getDocumentId()).get(0).getVectorIds();
		index.failDelete = true;

		TinyflowIngestionTask task = service.importDocument(
				fileImport("source-cleanup", "sha256-new", "new content", null));
		TinyflowDocument active = repository.findActiveDocument("tenant-a", "kb-1", "source-cleanup");

		assertEquals("COMPLETED", latest(task.getTaskId()).getStatus());
		assertNotEquals(oldVersion.getDocumentId(), active.getDocumentId());
		assertEquals(2, repository.findDocumentVersions("tenant-a", "kb-1", "source-cleanup").size());
		assertEquals(1, repository.compensations.size());
		TinyflowKnowledgeRepository.VectorCompensation compensation = repository.compensations.get(0);
		assertEquals("DELETE_OLD_VERSION", compensation.getOperation());
		assertEquals(TinyflowKnowledgeRepository.VectorCompensation.VECTOR_PENDING, compensation.getStage());
		assertEquals(oldVersion.getDocumentId(), compensation.getDocumentId());
		assertEquals(oldVectorIds, compensation.getVectorIds());

		index.failDelete = false;
		new TinyflowVectorCompensator(repository, index).retry("tenant-a", "kb-1");

		assertEquals(Collections.singletonList(active),
				repository.findDocumentVersions("tenant-a", "kb-1", "source-cleanup"));
		assertTrue(repository.findChunksByDocument(
				"tenant-a", "kb-1", oldVersion.getDocumentId()).isEmpty());
		assertTrue(repository.compensations.isEmpty());
	}

	@Test
	public void shouldResumeResourceCleanupAfterObjectWasAlreadyDeleted() {
		storage.values.put("object-old", "old content".getBytes(StandardCharsets.UTF_8));
		service.importDocument(fileImport("source-replay", "sha256-old", null, "object-old"));
		repository.failDeleteDocumentVersionOnce = true;
		storage.failWhenMissing = true;

		service.importDocument(fileImport("source-replay", "sha256-new", "new content", null));

		assertFalse(storage.exists("tenant-a", "object-old"));
		assertEquals(1, repository.compensations.size());
		new TinyflowVectorCompensator(repository, index, storage).retry("tenant-a", "kb-1");
		assertTrue(repository.findDocumentVersions("tenant-a", "kb-1", "source-replay").stream()
				.noneMatch(document -> "sha256-old".equals(document.getChecksum())));
		assertTrue(repository.compensations.isEmpty());
	}

	@Test
	public void shouldRetryLegacyResourcePendingObjectBeforeRepositoryCleanup() {
		storage.values.put("object-legacy", "legacy content".getBytes(StandardCharsets.UTF_8));
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		metadata.put("s3Key", "s3-main");
		repository.saveDocument(TinyflowDocument.builder().tenantId("tenant-a").knowledgeId("kb-1")
				.sourceId("source-legacy").sourceType("file").documentId("document-legacy")
				.checksum("sha256-legacy").sourceUri("object-legacy").metadata(metadata).build());
		repository.saveVectorCompensation(new TinyflowKnowledgeRepository.VectorCompensation(
				"compensation-legacy", "tenant-a", "kb-1", "source-legacy", "document-legacy",
				Collections.<String>emptyList(), TinyflowVectorCompensator.DELETE_FAILED_IMPORT,
				TinyflowKnowledgeRepository.VectorCompensation.RESOURCE_PENDING));
		repository.failDeleteDocumentVersionOnce = true;

		new TinyflowVectorCompensator(repository, index, storage).retry("tenant-a", "kb-1");

		assertFalse(storage.exists("tenant-a", "object-legacy"));
		assertEquals(1, repository.findDocumentVersions("tenant-a", "kb-1", "source-legacy").size());
		assertEquals(1, repository.compensations.size());

		new TinyflowVectorCompensator(repository, index, storage).retry("tenant-a", "kb-1");

		assertTrue(repository.findDocumentVersions("tenant-a", "kb-1", "source-legacy").isEmpty());
		assertTrue(repository.compensations.isEmpty());
	}

	@Test
	public void shouldKeepDisabledDocumentDisabledThroughIngestionCopies() {
		TinyflowIngestionTask task = service.importDocument(
				fileImport("source-disabled", "sha256-disabled", "disabled content", null, false));

		TinyflowDocument active = repository.findActiveDocument("tenant-a", "kb-1", "source-disabled");
		assertEquals("COMPLETED", latest(task.getTaskId()).getStatus());
		assertFalse(active.isEnabled());
	}

	@Test
	public void shouldPersistCompleteStateSequenceCountsAndChunks() {
		TinyflowIngestionTask pending = service.importDocument(
				fileImport("source-1", "sha256-a", "first line\nsecond line", null));

		assertEquals(Arrays.asList("PENDING", "PARSING", "CHUNKING", "EMBEDDING", "INDEXING", "COMPLETED"),
				statuses(pending.getTaskId()));
		TinyflowIngestionTask completed = latest(pending.getTaskId());
		assertEquals(2, completed.getMetadata().get("parsedSectionCount"));
		assertTrue(((Integer) completed.getMetadata().get("chunkCount")) > 0);
		assertEquals(0, completed.getMetadata().get("retryCount"));
		List<TinyflowChunk> chunks = repository.findChunksByDocument(
				"tenant-a", "kb-1", completed.getDocumentId());
		assertFalse(chunks.isEmpty());
		for (TinyflowChunk chunk : chunks) {
			assertEquals("tenant-a", chunk.getTenantId());
			assertEquals("kb-1", chunk.getKnowledgeId());
			assertEquals("source-1", chunk.getSourceId());
			assertEquals("file", chunk.getSourceType());
			assertEquals(completed.getDocumentId(), chunk.getDocumentId());
			assertNotNull(chunk.getChunkId());
			assertFalse(chunk.getVectorIds().isEmpty());
		}
	}

	@Test
	public void shouldCancelThroughCancellingAndCancelledWithoutIndexing() {
		ManualExecutor executor = new ManualExecutor();
		DefaultTinyflowIngestionService queued = service(executor, CompositeTinyflowDocumentParser.defaults());
		TinyflowIngestionTask pending = queued.importDocument(
				fileImport("source-cancel", "sha256-cancel", "cancel me", null));

		TinyflowIngestionTask cancelling = queued.cancel("tenant-a", "kb-1", pending.getTaskId());
		assertEquals("CANCELLING", cancelling.getStatus());
		executor.runNext();

		assertEquals(Arrays.asList("PENDING", "CANCELLING", "CANCELLED"), statuses(pending.getTaskId()));
		assertEquals(0, index.embeddingCalls.get());
		assertNull(repository.findActiveDocument("tenant-a", "kb-1", "source-cancel"));
	}

	@Test
	public void shouldPersistFailedStageAndSanitizedError() {
		TinyflowDocumentParser failing = new TinyflowDocumentParser() {
			@Override
			public boolean supports(String mediaType, String fileName) {
				return true;
			}

			@Override
			public TinyflowParsedDocument parse(InputStream input, ParseContext context) throws IOException {
				throw new IOException("password=secret-token at /private/input.txt");
			}
		};
		DefaultTinyflowIngestionService failingService = service(Runnable::run,
				new CompositeTinyflowDocumentParser(Collections.singletonList(failing)));

		TinyflowIngestionTask pending = failingService.importDocument(
				fileImport("source-fail", "sha256-fail", "bad", null));
		TinyflowIngestionTask failed = latest(pending.getTaskId());

		assertEquals(Arrays.asList("PENDING", "PARSING", "FAILED"), statuses(pending.getTaskId()));
		assertEquals("PARSING", failed.getMetadata().get("failedStage"));
		assertEquals(0, failed.getMetadata().get("retryCount"));
		assertFalse(failed.getMessage().contains("secret-token"));
		assertFalse(failed.getMessage().contains("/private"));
	}

	@Test
	public void shouldDeleteUploadedObjectAndFailedVersionWhenAsyncIngestionFails() {
		storage.values.put("object-fail", "bad".getBytes(StandardCharsets.UTF_8));
		TinyflowDocumentParser failing = new TinyflowDocumentParser() {
			@Override public boolean supports(String mediaType, String fileName) { return true; }
			@Override public TinyflowParsedDocument parse(InputStream input, ParseContext context) throws IOException {
				throw new IOException("parse failed");
			}
		};
		DefaultTinyflowIngestionService failingService = service(Runnable::run,
				new CompositeTinyflowDocumentParser(Collections.singletonList(failing)));

		TinyflowIngestionTask task = failingService.importDocument(
				fileImport("source-object-fail", "sha256-object-fail", null, "object-fail"));

		assertEquals("FAILED", latest(task.getTaskId()).getStatus());
		assertFalse(storage.exists("tenant-a", "object-fail"));
		assertTrue(repository.findDocumentVersions(
				"tenant-a", "kb-1", "source-object-fail").isEmpty());
		assertTrue(repository.compensations.isEmpty());
	}

	@Test
	public void shouldRetainFailedObjectCleanupAndCompleteItOnRetry() {
		storage.values.put("object-cleanup-retry", "bad".getBytes(StandardCharsets.UTF_8));
		storage.failDelete = true;
		TinyflowDocumentParser failing = new TinyflowDocumentParser() {
			@Override public boolean supports(String mediaType, String fileName) { return true; }
			@Override public TinyflowParsedDocument parse(InputStream input, ParseContext context) throws IOException {
				throw new IOException("parse failed");
			}
		};
		DefaultTinyflowIngestionService failingService = service(Runnable::run,
				new CompositeTinyflowDocumentParser(Collections.singletonList(failing)));

		failingService.importDocument(fileImport(
				"source-cleanup-retry", "sha256-cleanup-retry", null, "object-cleanup-retry"));

		assertTrue(storage.exists("tenant-a", "object-cleanup-retry"));
		assertEquals(1, repository.findDocumentVersions(
				"tenant-a", "kb-1", "source-cleanup-retry").size());
		assertEquals(1, repository.compensations.size());
		assertEquals(TinyflowVectorCompensator.DELETE_FAILED_IMPORT,
				repository.compensations.get(0).getOperation());
		assertEquals(TinyflowKnowledgeRepository.VectorCompensation.OBJECT_PENDING,
				repository.compensations.get(0).getStage());

		storage.failDelete = false;
		new TinyflowVectorCompensator(repository, index, storage).retry("tenant-a", "kb-1");

		assertFalse(storage.exists("tenant-a", "object-cleanup-retry"));
		assertTrue(repository.findDocumentVersions(
				"tenant-a", "kb-1", "source-cleanup-retry").isEmpty());
		assertTrue(repository.compensations.isEmpty());
	}

	@Test
	public void shouldDeleteUploadedObjectWhenQueuedIngestionIsCancelled() {
		storage.values.put("object-cancel", "cancel".getBytes(StandardCharsets.UTF_8));
		ManualExecutor executor = new ManualExecutor();
		DefaultTinyflowIngestionService queued = service(executor, CompositeTinyflowDocumentParser.defaults());
		TinyflowIngestionTask pending = queued.importDocument(
				fileImport("source-object-cancel", "sha256-object-cancel", null, "object-cancel"));

		queued.cancel("tenant-a", "kb-1", pending.getTaskId());
		executor.runNext();

		assertFalse(storage.exists("tenant-a", "object-cancel"));
		assertTrue(repository.findDocumentVersions(
				"tenant-a", "kb-1", "source-object-cancel").isEmpty());
		assertTrue(repository.compensations.isEmpty());
	}

	@Test
	public void shouldRetrySameChecksumAfterFailedVersion() {
		TinyflowDocumentParser failing = new TinyflowDocumentParser() {
			@Override
			public boolean supports(String mediaType, String fileName) {
				return true;
			}

			@Override
			public TinyflowParsedDocument parse(InputStream input, ParseContext context) throws IOException {
				throw new IOException("temporary failure");
			}
		};
		DefaultTinyflowIngestionService failingService = service(Runnable::run,
				new CompositeTinyflowDocumentParser(Collections.singletonList(failing)));
		TinyflowIngestionTask failedAttempt = failingService.importDocument(
				fileImport("source-retry", "sha256-retry", "retry content", null));

		TinyflowIngestionTask retry = service.importDocument(
				fileImport("source-retry", "sha256-retry", "retry content", null));

		assertNotEquals(failedAttempt.getDocumentId(), retry.getDocumentId());
		assertEquals("COMPLETED", latest(retry.getTaskId()).getStatus());
		assertEquals(1, index.embeddingCalls.get());
	}

	@Test
	public void shouldPersistRetryCountAcrossServiceInstancesAndThenSkipThirdImport() {
		TinyflowDocumentParser failing = new TinyflowDocumentParser() {
			@Override
			public boolean supports(String mediaType, String fileName) { return true; }

			@Override
			public TinyflowParsedDocument parse(InputStream input, ParseContext context) throws IOException {
				throw new IOException("temporary failure");
			}
		};
		DefaultTinyflowIngestionService firstService = service(Runnable::run,
				new CompositeTinyflowDocumentParser(Collections.singletonList(failing)));
		firstService.importDocument(fileImport("source-restart", "sha256-restart", "retry content", null));

		DefaultTinyflowIngestionService restarted = service(Runnable::run, CompositeTinyflowDocumentParser.defaults());
		TinyflowIngestionTask second = restarted.importDocument(
				fileImport("source-restart", "sha256-restart", "retry content", null));
		TinyflowIngestionTask third = restarted.importDocument(
				fileImport("source-restart", "sha256-restart", "retry content", null));

		assertEquals(1, latest(second.getTaskId()).getMetadata().get("retryCount"));
		assertEquals(second.getDocumentId(), third.getDocumentId());
		assertEquals("COMPLETED", third.getStatus());
		assertEquals(1, index.embeddingCalls.get());
	}

	@Test
	public void shouldAtomicallyGateConcurrentImportsForSameSourceAndChecksum() throws Exception {
		final TinyflowDocumentParser delegate = CompositeTinyflowDocumentParser.defaults();
		final CountDownLatch parsingEntered = new CountDownLatch(1);
		final CountDownLatch releaseParsing = new CountDownLatch(1);
		final AtomicBoolean first = new AtomicBoolean(true);
		TinyflowDocumentParser blocking = new TinyflowDocumentParser() {
			@Override
			public boolean supports(String mediaType, String fileName) { return true; }

			@Override
			public TinyflowParsedDocument parse(InputStream input, ParseContext context) throws IOException {
				if (first.compareAndSet(true, false)) {
					parsingEntered.countDown();
					await(releaseParsing);
				}
				return delegate.parse(input, context);
			}
		};
		Executor asynchronous = command -> new Thread(command, "tinyflow-ingestion-test").start();
		DefaultTinyflowIngestionService concurrent = service(asynchronous,
				new CompositeTinyflowDocumentParser(Collections.singletonList(blocking)));

		TinyflowIngestionTask firstTask = concurrent.importDocument(
				fileImport("source-concurrent", "sha256-concurrent", "content", null));
		assertTrue(parsingEntered.await(5, TimeUnit.SECONDS));
		TinyflowIngestionTask secondTask = concurrent.importDocument(
				fileImport("source-concurrent", "sha256-concurrent", "content", null));
		releaseParsing.countDown();

		assertEquals(firstTask.getDocumentId(), secondTask.getDocumentId());
		awaitTerminal(firstTask.getTaskId());
		assertEquals(1, index.embeddingCalls.get());
	}

	@Test
	public void shouldReturnCompletedWhenCancellationRacesWithAtomicCompletion() throws Exception {
		repository.blockCompletion = true;
		Executor asynchronous = command -> new Thread(command, "tinyflow-cancel-test").start();
		DefaultTinyflowIngestionService concurrent = service(asynchronous, CompositeTinyflowDocumentParser.defaults());

		TinyflowIngestionTask pending = concurrent.importDocument(
				fileImport("source-cancel-race", "sha256-cancel-race", "content", null));
		assertTrue(repository.completionEntered.await(5, TimeUnit.SECONDS));
		AtomicReference<TinyflowIngestionTask> cancelResult = new AtomicReference<TinyflowIngestionTask>();
		CountDownLatch cancelReturned = new CountDownLatch(1);
		Thread cancelThread = new Thread(() -> {
			cancelResult.set(concurrent.cancel("tenant-a", "kb-1", pending.getTaskId()));
			cancelReturned.countDown();
		}, "tinyflow-cancel-race-test");
		cancelThread.start();
		assertFalse(cancelReturned.await(100, TimeUnit.MILLISECONDS));
		repository.releaseCompletion.countDown();
		assertTrue(cancelReturned.await(5, TimeUnit.SECONDS));

		TinyflowIngestionTask terminal = awaitTerminal(pending.getTaskId());
		assertEquals("COMPLETED", terminal.getStatus());
		assertEquals("COMPLETED", cancelResult.get().getStatus());
		assertFalse(statuses(pending.getTaskId()).contains("CANCELLING"));
		assertEquals(0, index.deleteCalls.get());
		assertTrue(repository.compensations.isEmpty());
	}

	@Test
	public void shouldPersistCompensationBeforeDeletingVectorsWhenChunkPersistenceFails() {
		repository.failSaveChunks = true;

		TinyflowIngestionTask pending = service.importDocument(
				fileImport("source-persist-fail", "sha256-persist-fail", "content", null));

		assertEquals("FAILED", latest(pending.getTaskId()).getStatus());
		assertEquals(1, index.deleteCalls.get());
		assertTrue(index.compensationPresentAtDelete);
		assertFalse(index.lastDeletedVectorIds.isEmpty());
		assertTrue(repository.compensations.isEmpty());
	}

	@Test
	public void shouldRetainVectorCompensationWhenVectorDeletionFails() {
		repository.failSaveChunks = true;
		index.failDelete = true;

		TinyflowIngestionTask pending = service.importDocument(
				fileImport("source-delete-fail", "sha256-delete-fail", "content", null));

		assertEquals("FAILED", latest(pending.getTaskId()).getStatus());
		assertEquals(1, index.deleteCalls.get());
		assertEquals(1, repository.compensations.size());
		assertFalse(repository.compensations.get(0).getVectorIds().isEmpty());
	}

	@Test
	public void shouldCompensateReturnedVectorsWhenIndexMappingIsIncomplete() {
		index.incompleteIndexResult = true;

		TinyflowIngestionTask pending = service.importDocument(
				fileImport("source-index-invalid", "sha256-index-invalid", "first\nsecond", null));

		assertEquals("FAILED", latest(pending.getTaskId()).getStatus());
		assertEquals(1, index.deleteCalls.get());
		assertEquals(Collections.singletonList("vector-orphan"), index.lastDeletedVectorIds);
		assertTrue(repository.compensations.isEmpty());
	}

	@Test
	public void shouldCompensatePartialIndexFailureBeforeFailingTask() {
		index.partialFailure = true;

		TinyflowIngestionTask pending = service.importDocument(
				fileImport("source-index-partial", "sha256-index-partial", "content", null));

		assertEquals("FAILED", latest(pending.getTaskId()).getStatus());
		assertEquals(1, index.deleteCalls.get());
		assertTrue(index.compensationPresentAtDelete);
		assertFalse(index.lastDeletedVectorIds.isEmpty());
		assertTrue(repository.compensations.isEmpty());
	}

	@Test
	public void shouldRetainPartialIndexCompensationWhenVectorDeletionFails() {
		index.partialFailure = true;
		index.failDelete = true;

		TinyflowIngestionTask pending = service.importDocument(
				fileImport("source-index-partial-delete", "sha256-index-partial-delete", "content", null));

		assertEquals("FAILED", latest(pending.getTaskId()).getStatus());
		assertEquals(1, repository.compensations.size());
		assertFalse(repository.compensations.get(0).getVectorIds().isEmpty());
	}

	@Test
	public void shouldRejectExtraIndexKeyWithoutDeletingItsVector() {
		index.includeExtraKey = true;

		TinyflowIngestionTask pending = service.importDocument(
				fileImport("source-index-extra", "sha256-index-extra", "content", null));

		assertEquals("FAILED", latest(pending.getTaskId()).getStatus());
		assertEquals(1, index.deleteCalls.get());
		assertFalse(index.lastDeletedVectorIds.contains("vector-existing-document"));
	}

	@Test
	public void shouldRejectCumulativeIndexResultWithoutDeletingPreviousCallVectors() {
		index.returnCumulative = true;
		TinyflowIngestionTask first = service.importDocument(
				fileImport("source-index-first", "sha256-index-first", "first", null));
		String previousVectorId = repository.findChunksByDocument("tenant-a", "kb-1", first.getDocumentId())
				.get(0).getVectorIds().get(0);

		TinyflowIngestionTask second = service.importDocument(
				fileImport("source-index-second", "sha256-index-second", "second", null));

		assertEquals("FAILED", latest(second.getTaskId()).getStatus());
		assertEquals(1, index.deleteCalls.get());
		assertFalse(index.lastDeletedVectorIds.contains(previousVectorId));
	}

	@Test
	public void shouldClearInFlightWhenPendingTaskPersistenceFailsAndAllowRetry() {
		repository.failTaskStatusOnce = "PENDING";

		assertThrows(IllegalStateException.class, () -> service.importDocument(
				fileImport("source-pending-fail", "sha256-pending-fail", "content", null)));
		TinyflowIngestionTask retry = service.importDocument(
				fileImport("source-pending-fail", "sha256-pending-fail", "content", null));

		assertNotNull(latest(retry.getTaskId()));
		assertEquals("COMPLETED", latest(retry.getTaskId()).getStatus());
		assertEquals(1, index.indexCalls.get());
	}

	@Test
	public void shouldNotPublishCancellationWhenCancellingTaskPersistenceFails() {
		ManualExecutor executor = new ManualExecutor();
		DefaultTinyflowIngestionService queued = service(executor, CompositeTinyflowDocumentParser.defaults());
		TinyflowIngestionTask pending = queued.importDocument(
				fileImport("source-cancel-save-fail", "sha256-cancel-save-fail", "content", null));
		repository.failTaskStatusOnce = "CANCELLING";

		assertThrows(IllegalStateException.class,
				() -> queued.cancel("tenant-a", "kb-1", pending.getTaskId()));
		executor.runNext();

		assertEquals("COMPLETED", latest(pending.getTaskId()).getStatus());
		assertFalse(statuses(pending.getTaskId()).contains("CANCELLED"));
	}

	@Test
	public void shouldNotHalfCommitChunksOrDocumentWhenActivationFails() {
		TinyflowIngestionTask first = service.importDocument(
				fileImport("source-complete-fail", "sha256-complete-old", "old", null));
		repository.failActivation = true;

		TinyflowIngestionTask second = service.importDocument(
				fileImport("source-complete-fail", "sha256-complete-new", "new", null));

		assertEquals("FAILED", latest(second.getTaskId()).getStatus());
		assertEquals(first.getDocumentId(), repository.findActiveDocument(
				"tenant-a", "kb-1", "source-complete-fail").getDocumentId());
		assertTrue(repository.findChunksByDocument("tenant-a", "kb-1", second.getDocumentId()).isEmpty());
		TinyflowDocument failedVersion = repository.findBySourceAndChecksum(
				"tenant-a", "kb-1", "source-complete-fail", "sha256-complete-new");
		assertTrue(failedVersion.getChunks().isEmpty());
		assertEquals(1, index.deleteCalls.get());
	}

	@Test
	public void shouldExposeFixedProviderKeyAndCrudKnowledgeDefinitions() {
		FwPlusLocalKnowledgeProvider provider = new FwPlusLocalKnowledgeProvider(repository, service);
		TinyflowKnowledgeDefinition created = provider.create(definition("Initial", true));
		TinyflowKnowledgeDefinition updated = provider.update(definition("Updated", true));
		provider.delete("tenant-a", "kb-1");

		assertEquals("fw-plus-local", provider.getKey());
		assertTrue(provider.capabilities().supportsManagement());
		assertEquals("Initial", created.getName());
		assertEquals("Updated", updated.getName());
		assertNull(repository.findKnowledge("tenant-a", "kb-1"));
		assertEquals(0L, repository.findKnowledgeVersion("tenant-a", "kb-1"));
	}

	@Test
	public void shouldReportCapabilitiesForEveryConstructorMode() {
		TinyflowLocalRetriever retriever = emptyRetriever();
		FwPlusLocalKnowledgeProvider ingestionOnly = new FwPlusLocalKnowledgeProvider(repository, service);
		FwPlusLocalKnowledgeProvider retrievalOnly = new FwPlusLocalKnowledgeProvider(repository, retriever);
		FwPlusLocalKnowledgeProvider full = new FwPlusLocalKnowledgeProvider(repository, service, retriever);

		assertFalse(ingestionOnly.capabilities().supportsRetrieval());
		assertTrue(ingestionOnly.capabilities().supportsManagement());
		assertTrue(ingestionOnly.capabilities().supportsDocumentImport());
		assertTrue(retrievalOnly.capabilities().supportsRetrieval());
		assertTrue(retrievalOnly.capabilities().supportsManagement());
		assertFalse(retrievalOnly.capabilities().supportsDocumentImport());
		assertTrue(full.capabilities().supportsRetrieval());
		assertTrue(full.capabilities().supportsManagement());
		assertTrue(full.capabilities().supportsDocumentImport());
	}

	@Test
	public void shouldRejectStaleProviderDeleteAndKeepConcurrentKnowledgeUpdate() {
		FwPlusLocalKnowledgeProvider provider = new FwPlusLocalKnowledgeProvider(repository, service);
		provider.create(definition("Initial", true));
		repository.concurrentUpdateBeforeDelete = true;

		assertThrows(TinyflowKnowledgeRepository.OptimisticLockException.class,
				() -> provider.delete("tenant-a", "kb-1"));

		assertEquals("Concurrent update", repository.findKnowledge("tenant-a", "kb-1").getName());
		assertTrue(repository.findKnowledge("tenant-a", "kb-1").isEnabled());
	}

	private DefaultTinyflowIngestionService service(Executor executor, TinyflowDocumentParser parser) {
		Map<String, TinyflowChunkStrategy> strategies = new LinkedHashMap<String, TinyflowChunkStrategy>();
		strategies.put("normal", new NormalChunkStrategy());
		return new DefaultTinyflowIngestionService(repository, storage, parser, strategies, index, executor);
	}

	private TinyflowLocalRetriever emptyRetriever() {
		TinyflowModelProvider models = new TinyflowModelProvider() {
			@Override public List<TinyflowModelDescriptor> listChatModels() { return Collections.emptyList(); }
			@Override public List<TinyflowModelDescriptor> listEmbeddingModels() { return Collections.emptyList(); }
			@Override public ChatLanguageModel getChatModel(String key) { return null; }
			@Override public TinyflowEmbeddingModel getEmbeddingModel(String key) { return null; }
		};
		TinyflowVectorStoreProvider stores = new TinyflowVectorStoreProvider() {
			@Override public List<TinyflowVectorStoreDescriptor> list() { return Collections.emptyList(); }
			@Override public TinyflowVectorStore get(String key) { return null; }
		};
		return new TinyflowLocalRetriever(repository, models, stores, null);
	}

	private TinyflowDocumentImport fileImport(String sourceId, String checksum, String content, String objectKey) {
		return fileImport(sourceId, checksum, content, objectKey, true);
	}

	private TinyflowDocumentImport fileImport(String sourceId, String checksum, String content,
			String objectKey, boolean enabled) {
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		metadata.put("checksum", checksum);
		metadata.put("mediaType", "text/plain");
		if (objectKey != null) {
			metadata.put("s3Key", "s3-main");
		}
		TinyflowDocument document = TinyflowDocument.builder().tenantId("tenant-a").knowledgeId("kb-1")
				.sourceId(sourceId).sourceType("file").documentId("request-document")
				.checksum(checksum).name("fixture.txt").content(content).sourceUri(objectKey).metadata(metadata)
				.enabled(enabled).build();
		return TinyflowDocumentImport.builder().providerKey("fw-plus-local").tenantId("tenant-a")
				.knowledgeId("kb-1").document(document)
				.options(Collections.<String, Object>singletonMap("chunkStrategy", "normal")).build();
	}

	private TinyflowKnowledgeDefinition definition(String name, boolean enabled) {
		return TinyflowKnowledgeDefinition.builder().providerKey("fw-plus-local").tenantId("tenant-a")
				.knowledgeId("kb-1").name(name).enabled(enabled).build();
	}

	private List<String> statuses(String taskId) {
		List<String> statuses = new ArrayList<String>();
		for (TinyflowIngestionTask task : repository.taskSnapshot()) {
			if (taskId.equals(task.getTaskId())) {
				statuses.add(task.getStatus());
			}
		}
		return statuses;
	}

	private TinyflowIngestionTask latest(String taskId) {
		TinyflowIngestionTask latest = null;
		for (TinyflowIngestionTask task : repository.taskSnapshot()) {
			if (taskId.equals(task.getTaskId())) {
				latest = task;
			}
		}
		return latest;
	}

	private TinyflowIngestionTask awaitTerminal(String taskId) throws InterruptedException {
		long deadline = System.currentTimeMillis() + 5000L;
		while (System.currentTimeMillis() < deadline) {
			TinyflowIngestionTask task = latest(taskId);
			if (task != null && ("COMPLETED".equals(task.getStatus()) || "FAILED".equals(task.getStatus())
					|| "CANCELLED".equals(task.getStatus()))) {
				return task;
			}
			Thread.sleep(10L);
		}
		throw new AssertionError("Task did not reach terminal state");
	}

	private static void await(CountDownLatch latch) throws IOException {
		try {
			if (!latch.await(5, TimeUnit.SECONDS)) {
				throw new IOException("Timed out waiting for test latch");
			}
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while waiting for test latch", exception);
		}
	}

	private static final class RecordingIndex implements DefaultTinyflowIngestionService.IngestionIndex {
		private final InMemoryRepository repository;
		private final AtomicInteger embeddingCalls = new AtomicInteger();
		private final AtomicInteger indexCalls = new AtomicInteger();
		private final AtomicInteger deleteCalls = new AtomicInteger();
		private final Map<String, List<String>> indexedVectorIds = new LinkedHashMap<String, List<String>>();
		private volatile List<String> lastDeletedVectorIds = Collections.emptyList();
		private volatile boolean compensationPresentAtDelete;
		private boolean failDelete;
		private boolean incompleteIndexResult;
		private boolean partialFailure;
		private boolean includeExtraKey;
		private boolean returnCumulative;

		private RecordingIndex(InMemoryRepository repository) {
			this.repository = repository;
		}

		@Override
		public void embed(String tenantId, String knowledgeId, String documentId, List<TinyflowChunk> chunks) {
			embeddingCalls.incrementAndGet();
		}

		@Override
		public Map<String, List<String>> index(String tenantId, String knowledgeId,
				String documentId, List<TinyflowChunk> chunks) throws Exception {
			indexCalls.incrementAndGet();
			Map<String, List<String>> current = new LinkedHashMap<String, List<String>>();
			for (TinyflowChunk chunk : chunks) {
				current.put(chunk.getChunkId(),
						Collections.singletonList("vector-" + chunk.getChunkId()));
			}
			indexedVectorIds.putAll(current);
			if (incompleteIndexResult) {
				String firstChunkId = current.keySet().iterator().next();
				return Collections.singletonMap(firstChunkId,
						Collections.singletonList("vector-orphan"));
			}
			Map<String, List<String>> result = returnCumulative
					? new LinkedHashMap<String, List<String>>(indexedVectorIds) : current;
			if (includeExtraKey) {
				result.put("existing-document-chunk", Collections.singletonList("vector-existing-document"));
			}
			if (partialFailure) {
				throw new DefaultTinyflowIngestionService.IngestionIndex.PartialIndexException(
						"partial index failure", result);
			}
			return result;
		}

		@Override
		public void delete(String tenantId, String knowledgeId, List<String> vectorIds) throws Exception {
			deleteCalls.incrementAndGet();
			lastDeletedVectorIds = new ArrayList<String>(vectorIds);
			compensationPresentAtDelete = !repository.compensations.isEmpty();
			if (failDelete) {
				throw new IOException("vector delete failed");
			}
		}
	}

	private static final class ManualExecutor implements Executor {
		private final List<Runnable> commands = new ArrayList<Runnable>();

		@Override
		public void execute(Runnable command) {
			commands.add(command);
		}

		private void runNext() {
			commands.remove(0).run();
		}
	}

	private static final class InMemoryObjectStorage implements TinyflowObjectStorage {
		private final Map<String, byte[]> values = new LinkedHashMap<String, byte[]>();
		private boolean failDelete;
		private boolean failWhenMissing;

		@Override
		public StoredObject put(String tenantId, String objectKey, InputStream input, String mediaType,
				Map<String, Object> metadata) {
			throw new UnsupportedOperationException();
		}

		@Override
		public InputStream get(String tenantId, String objectKey) throws IOException {
			byte[] value = values.get(objectKey);
			if (value == null) {
				throw new IOException("Object not found");
			}
			return new ByteArrayInputStream(value);
		}

		@Override
		public boolean exists(String tenantId, String objectKey) {
			return values.containsKey(objectKey);
		}

		@Override
		public void delete(String tenantId, String objectKey) {
			if (failDelete) {
				throw new IllegalStateException("Object delete failed");
			}
			if (failWhenMissing && !values.containsKey(objectKey)) {
				throw new IllegalStateException("Object is already missing");
			}
			values.remove(objectKey);
		}
	}

	private static final class InMemoryRepository implements TinyflowKnowledgeRepository {
		private final Map<String, TinyflowKnowledgeDefinition> knowledge = new LinkedHashMap<String, TinyflowKnowledgeDefinition>();
		private final Map<String, Long> knowledgeVersions = new LinkedHashMap<String, Long>();
		private final Map<String, TinyflowDocument> documents = new LinkedHashMap<String, TinyflowDocument>();
		private final Map<String, String> active = new LinkedHashMap<String, String>();
		private final Map<String, TinyflowChunk> chunks = new LinkedHashMap<String, TinyflowChunk>();
		private final List<TinyflowIngestionTask> tasks =
				Collections.synchronizedList(new ArrayList<TinyflowIngestionTask>());
		private final List<VectorCompensation> compensations =
				Collections.synchronizedList(new ArrayList<VectorCompensation>());
		private volatile boolean blockCompletion;
		private volatile boolean failSaveChunks;
		private volatile boolean concurrentUpdateBeforeDelete;
		private volatile String failTaskStatusOnce;
		private volatile boolean failActivation;
		private volatile boolean failDeleteDocumentVersionOnce;
		private final CountDownLatch completionEntered = new CountDownLatch(1);
		private final CountDownLatch releaseCompletion = new CountDownLatch(1);

		@Override
		public synchronized TinyflowKnowledgeDefinition findKnowledge(String tenantId, String knowledgeId) {
			return knowledge.get(key(tenantId, knowledgeId));
		}

		@Override
		public synchronized long findKnowledgeVersion(String tenantId, String knowledgeId) {
			Long version = knowledgeVersions.get(key(tenantId, knowledgeId));
			return version == null ? 0L : version;
		}

		@Override
		public synchronized void saveKnowledge(TinyflowKnowledgeDefinition definition, long expectedVersion) {
			String key = key(definition.getTenantId(), definition.getKnowledgeId());
			long actual = findKnowledgeVersion(definition.getTenantId(), definition.getKnowledgeId());
			if (actual != expectedVersion) {
				throw new OptimisticLockException(expectedVersion, actual);
			}
			knowledge.put(key, definition);
			knowledgeVersions.put(key, actual + 1L);
		}

		@Override
		public synchronized void saveKnowledgeWithVectorStoreOwnership(TinyflowKnowledgeDefinition definition,
				long expectedVersion, String vectorStoreKey, boolean metadataFilterSupported) {
			saveKnowledge(definition, expectedVersion);
		}

		@Override
		public synchronized void deleteKnowledge(String tenantId, String knowledgeId, long expectedVersion) {
			if (concurrentUpdateBeforeDelete) {
				concurrentUpdateBeforeDelete = false;
				TinyflowKnowledgeDefinition concurrent = TinyflowKnowledgeDefinition.builder()
						.providerKey("fw-plus-local").tenantId(tenantId).knowledgeId(knowledgeId)
						.name("Concurrent update").enabled(true).build();
				saveKnowledge(concurrent, expectedVersion);
			}
			long actual = findKnowledgeVersion(tenantId, knowledgeId);
			if (actual != expectedVersion) {
				throw new OptimisticLockException(expectedVersion, actual);
			}
			String key = key(tenantId, knowledgeId);
			knowledge.remove(key);
			knowledgeVersions.remove(key);
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
			String id = active.get(sourceKey(tenantId, knowledgeId, sourceId));
			return id == null ? null : documents.get(documentKey(tenantId, knowledgeId, id));
		}

		@Override
		public synchronized void saveDocument(TinyflowDocument document) {
			documents.put(documentKey(document.getTenantId(), document.getKnowledgeId(), document.getDocumentId()), document);
		}

		@Override
		public synchronized List<TinyflowDocument> findDocumentVersions(String tenantId, String knowledgeId, String sourceId) {
			List<TinyflowDocument> result = new ArrayList<TinyflowDocument>();
			for (TinyflowDocument document : documents.values()) {
				if (tenantId.equals(document.getTenantId()) && knowledgeId.equals(document.getKnowledgeId())
						&& sourceId.equals(document.getSourceId())) {
					result.add(document);
				}
			}
			return result;
		}

		@Override
		public synchronized void activateDocumentVersion(String tenantId, String knowledgeId, String sourceId,
				String documentId) {
			if (failActivation) {
				failActivation = false;
				throw new IllegalStateException("Activation failed");
			}
			TinyflowDocument document = documents.get(documentKey(tenantId, knowledgeId, documentId));
			if (document == null || !sourceId.equals(document.getSourceId())) {
				throw new IllegalArgumentException("Document does not belong to source");
			}
			active.put(sourceKey(tenantId, knowledgeId, sourceId), documentId);
		}

		@Override
		public synchronized void deleteDocumentsBySource(String tenantId, String knowledgeId, String sourceId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public synchronized void deleteDocumentVersion(String tenantId, String knowledgeId,
				String sourceId, String documentId) {
			if (failDeleteDocumentVersionOnce) {
				failDeleteDocumentVersionOnce = false;
				throw new IllegalStateException("Document deletion failed");
			}
			String documentKey = documentKey(tenantId, knowledgeId, documentId);
			TinyflowDocument document = documents.get(documentKey);
			if (document == null) {
				return;
			}
			if (!sourceId.equals(document.getSourceId())) {
				throw new IllegalArgumentException("Document does not belong to source");
			}
			if (documentId.equals(active.get(sourceKey(tenantId, knowledgeId, sourceId)))) {
				throw new IllegalStateException("Active document version cannot be deleted");
			}
			documents.remove(documentKey);
			for (TinyflowChunk chunk : new ArrayList<TinyflowChunk>(chunks.values())) {
				if (tenantId.equals(chunk.getTenantId()) && knowledgeId.equals(chunk.getKnowledgeId())
						&& documentId.equals(chunk.getDocumentId())) {
					chunks.remove(chunkKey(tenantId, knowledgeId, chunk.getChunkId()));
				}
			}
		}

		@Override
		public synchronized void saveChunks(List<TinyflowChunk> values) {
			if (failSaveChunks) {
				throw new IllegalStateException("Chunk persistence failed");
			}
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
				if (tenantId.equals(chunk.getTenantId()) && knowledgeId.equals(chunk.getKnowledgeId())
						&& documentId.equals(chunk.getDocumentId())) {
					result.add(chunk);
				}
			}
			return result;
		}

		@Override
		public synchronized List<TinyflowChunk> findChunksByVectorIds(String tenantId, String knowledgeId,
				List<String> vectorIds) {
			return Collections.emptyList();
		}

		@Override
		public synchronized List<TinyflowKnowledgeMatch> searchChunksByKeyword(
				String tenantId, String knowledgeId, String query, int limit) {
			return Collections.emptyList();
		}

		@Override
		public synchronized void updateChunk(String tenantId, String knowledgeId, String chunkId, boolean enabled,
				String content, Map<String, Object> metadata) {
			throw new UnsupportedOperationException();
		}

		@Override
		public synchronized void saveTask(TinyflowIngestionTask task) {
			if (task.getStatus().equals(failTaskStatusOnce)) {
				failTaskStatusOnce = null;
				throw new IllegalStateException("Task persistence failed");
			}
			tasks.add(task);
		}

		@Override
		public synchronized void saveTaskWithVectorCompensation(TinyflowIngestionTask task,
				VectorCompensation compensation) {
			List<TinyflowIngestionTask> taskSnapshot = new ArrayList<TinyflowIngestionTask>(tasks);
			List<VectorCompensation> compensationSnapshot = new ArrayList<VectorCompensation>(compensations);
			try {
				saveTask(task);
				saveVectorCompensation(compensation);
			} catch (RuntimeException exception) {
				tasks.clear();
				tasks.addAll(taskSnapshot);
				compensations.clear();
				compensations.addAll(compensationSnapshot);
				throw exception;
			}
		}

		@Override
		public synchronized void completeIngestion(TinyflowDocument completedDocument,
				List<TinyflowChunk> values, TinyflowIngestionTask completedTask) {
			if (blockCompletion) {
				completionEntered.countDown();
				try {
					if (!releaseCompletion.await(5, TimeUnit.SECONDS)) {
						throw new IllegalStateException("Timed out waiting to release completion");
					}
				} catch (InterruptedException exception) {
					Thread.currentThread().interrupt();
					throw new IllegalStateException("Interrupted", exception);
				}
			}
			Map<String, TinyflowDocument> documentSnapshot =
					new LinkedHashMap<String, TinyflowDocument>(documents);
			Map<String, String> activeSnapshot = new LinkedHashMap<String, String>(active);
			Map<String, TinyflowChunk> chunkSnapshot = new LinkedHashMap<String, TinyflowChunk>(chunks);
			List<TinyflowIngestionTask> taskSnapshot = new ArrayList<TinyflowIngestionTask>(tasks);
			try {
				saveChunks(values);
				saveDocument(completedDocument);
				activateDocumentVersion(completedDocument.getTenantId(), completedDocument.getKnowledgeId(),
						completedDocument.getSourceId(), completedDocument.getDocumentId());
				saveTask(completedTask);
			} catch (RuntimeException exception) {
				documents.clear();
				documents.putAll(documentSnapshot);
				active.clear();
				active.putAll(activeSnapshot);
				chunks.clear();
				chunks.putAll(chunkSnapshot);
				tasks.clear();
				tasks.addAll(taskSnapshot);
				throw exception;
			}
		}

		@Override
		public synchronized void completeIngestionWithVectorCompensations(TinyflowDocument completedDocument,
				List<TinyflowChunk> values, TinyflowIngestionTask completedTask,
				List<VectorCompensation> cleanupCompensations) {
			Map<String, TinyflowDocument> documentSnapshot =
					new LinkedHashMap<String, TinyflowDocument>(documents);
			Map<String, String> activeSnapshot = new LinkedHashMap<String, String>(active);
			Map<String, TinyflowChunk> chunkSnapshot = new LinkedHashMap<String, TinyflowChunk>(chunks);
			List<TinyflowIngestionTask> taskSnapshot = new ArrayList<TinyflowIngestionTask>(tasks);
			List<VectorCompensation> compensationSnapshot = new ArrayList<VectorCompensation>(compensations);
			try {
				completeIngestion(completedDocument, values, completedTask);
				for (VectorCompensation compensation : cleanupCompensations) {
					saveVectorCompensation(compensation);
				}
			} catch (RuntimeException exception) {
				documents.clear();
				documents.putAll(documentSnapshot);
				active.clear();
				active.putAll(activeSnapshot);
				chunks.clear();
				chunks.putAll(chunkSnapshot);
				tasks.clear();
				tasks.addAll(taskSnapshot);
				compensations.clear();
				compensations.addAll(compensationSnapshot);
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
		public synchronized void advanceVectorCompensationStage(String tenantId, String knowledgeId,
				String compensationId, String expectedStage, String nextStage) {
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
		}

		@Override
		public synchronized void deleteVectorCompensation(String tenantId, String knowledgeId,
				String compensationId) {
			for (VectorCompensation compensation : new ArrayList<VectorCompensation>(compensations)) {
				if (tenantId.equals(compensation.getTenantId())
						&& knowledgeId.equals(compensation.getKnowledgeId())
						&& compensationId.equals(compensation.getCompensationId())) {
					compensations.remove(compensation);
				}
			}
		}

		private synchronized List<TinyflowIngestionTask> taskSnapshot() {
			return new ArrayList<TinyflowIngestionTask>(tasks);
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
