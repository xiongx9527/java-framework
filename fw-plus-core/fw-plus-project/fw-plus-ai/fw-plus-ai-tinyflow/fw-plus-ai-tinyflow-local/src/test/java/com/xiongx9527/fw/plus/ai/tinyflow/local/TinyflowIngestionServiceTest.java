package com.xiongx9527.fw.plus.ai.tinyflow.local;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowChunk;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowChunkStrategy;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDocument;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDocumentImport;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDocumentParser;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowIngestionTask;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeDefinition;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowObjectStorage;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowParsedDocument;
import com.xiongx9527.fw.plus.ai.tinyflow.local.chunk.NormalChunkStrategy;
import com.xiongx9527.fw.plus.ai.tinyflow.local.parser.CompositeTinyflowDocumentParser;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TinyflowIngestionServiceTest {

	private InMemoryRepository repository;
	private InMemoryObjectStorage storage;
	private RecordingIndex index;
	private DefaultTinyflowIngestionService service;

	@Before
	public void setUp() {
		repository = new InMemoryRepository();
		storage = new InMemoryObjectStorage();
		index = new RecordingIndex();
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
	public void shouldPreserveSourceIdAndCreateNewDocumentVersion() {
		service.importDocument(fileImport("source-1", "sha256-old", "old content", null));
		TinyflowDocument v1 = repository.findActiveDocument("tenant-a", "kb-1", "source-1");
		service.importDocument(fileImport("source-1", "sha256-new", "new content", null));
		TinyflowDocument v2 = repository.findActiveDocument("tenant-a", "kb-1", "source-1");

		assertEquals(v1.getSourceId(), v2.getSourceId());
		assertNotEquals(v1.getDocumentId(), v2.getDocumentId());
		assertEquals(2, repository.findDocumentVersions("tenant-a", "kb-1", "source-1").size());
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
	public void shouldExposeFixedProviderKeyAndCrudKnowledgeDefinitions() {
		FwPlusLocalKnowledgeProvider provider = new FwPlusLocalKnowledgeProvider(repository, service);
		TinyflowKnowledgeDefinition created = provider.create(definition("Initial", true));
		TinyflowKnowledgeDefinition updated = provider.update(definition("Updated", true));
		provider.delete("tenant-a", "kb-1");

		assertEquals("fw-plus-local", provider.getKey());
		assertTrue(provider.capabilities().supportsManagement());
		assertEquals("Initial", created.getName());
		assertEquals("Updated", updated.getName());
		assertFalse(repository.findKnowledge("tenant-a", "kb-1").isEnabled());
	}

	private DefaultTinyflowIngestionService service(Executor executor, TinyflowDocumentParser parser) {
		Map<String, TinyflowChunkStrategy> strategies = new LinkedHashMap<String, TinyflowChunkStrategy>();
		strategies.put("normal", new NormalChunkStrategy());
		return new DefaultTinyflowIngestionService(repository, storage, parser, strategies, index, executor);
	}

	private TinyflowDocumentImport fileImport(String sourceId, String checksum, String content, String objectKey) {
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		metadata.put("checksum", checksum);
		metadata.put("mediaType", "text/plain");
		TinyflowDocument document = TinyflowDocument.builder().tenantId("tenant-a").knowledgeId("kb-1")
				.sourceId(sourceId).sourceType("file").documentId("request-document")
				.name("fixture.txt").content(content).sourceUri(objectKey).metadata(metadata).build();
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
		for (TinyflowIngestionTask task : repository.tasks) {
			if (taskId.equals(task.getTaskId())) {
				statuses.add(task.getStatus());
			}
		}
		return statuses;
	}

	private TinyflowIngestionTask latest(String taskId) {
		TinyflowIngestionTask latest = null;
		for (TinyflowIngestionTask task : repository.tasks) {
			if (taskId.equals(task.getTaskId())) {
				latest = task;
			}
		}
		return latest;
	}

	private static final class RecordingIndex implements DefaultTinyflowIngestionService.IngestionIndex {
		private final AtomicInteger embeddingCalls = new AtomicInteger();
		private final AtomicInteger indexCalls = new AtomicInteger();

		@Override
		public void embed(String tenantId, String knowledgeId, String documentId, List<TinyflowChunk> chunks) {
			embeddingCalls.incrementAndGet();
		}

		@Override
		public void index(String tenantId, String knowledgeId, String documentId, List<TinyflowChunk> chunks) {
			indexCalls.incrementAndGet();
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
			values.remove(objectKey);
		}
	}

	private static final class InMemoryRepository implements TinyflowKnowledgeRepository {
		private final Map<String, TinyflowKnowledgeDefinition> knowledge = new LinkedHashMap<String, TinyflowKnowledgeDefinition>();
		private final Map<String, Long> knowledgeVersions = new LinkedHashMap<String, Long>();
		private final Map<String, TinyflowDocument> documents = new LinkedHashMap<String, TinyflowDocument>();
		private final Map<String, String> active = new LinkedHashMap<String, String>();
		private final Map<String, TinyflowChunk> chunks = new LinkedHashMap<String, TinyflowChunk>();
		private final List<TinyflowIngestionTask> tasks = new ArrayList<TinyflowIngestionTask>();

		@Override
		public TinyflowKnowledgeDefinition findKnowledge(String tenantId, String knowledgeId) {
			return knowledge.get(key(tenantId, knowledgeId));
		}

		@Override
		public long findKnowledgeVersion(String tenantId, String knowledgeId) {
			Long version = knowledgeVersions.get(key(tenantId, knowledgeId));
			return version == null ? 0L : version;
		}

		@Override
		public void saveKnowledge(TinyflowKnowledgeDefinition definition, long expectedVersion) {
			String key = key(definition.getTenantId(), definition.getKnowledgeId());
			long actual = findKnowledgeVersion(definition.getTenantId(), definition.getKnowledgeId());
			if (actual != expectedVersion) {
				throw new OptimisticLockException(expectedVersion, actual);
			}
			knowledge.put(key, definition);
			knowledgeVersions.put(key, actual + 1L);
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
			String id = active.get(sourceKey(tenantId, knowledgeId, sourceId));
			return id == null ? null : documents.get(documentKey(tenantId, knowledgeId, id));
		}

		@Override
		public void saveDocument(TinyflowDocument document) {
			documents.put(documentKey(document.getTenantId(), document.getKnowledgeId(), document.getDocumentId()), document);
		}

		@Override
		public List<TinyflowDocument> findDocumentVersions(String tenantId, String knowledgeId, String sourceId) {
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
		public void activateDocumentVersion(String tenantId, String knowledgeId, String sourceId, String documentId) {
			TinyflowDocument document = documents.get(documentKey(tenantId, knowledgeId, documentId));
			if (document == null || !sourceId.equals(document.getSourceId())) {
				throw new IllegalArgumentException("Document does not belong to source");
			}
			active.put(sourceKey(tenantId, knowledgeId, sourceId), documentId);
		}

		@Override
		public void deleteDocumentsBySource(String tenantId, String knowledgeId, String sourceId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void saveChunks(List<TinyflowChunk> values) {
			for (TinyflowChunk chunk : values) {
				chunks.put(chunkKey(chunk.getTenantId(), chunk.getKnowledgeId(), chunk.getChunkId()), chunk);
			}
		}

		@Override
		public TinyflowChunk findChunk(String tenantId, String knowledgeId, String chunkId) {
			return chunks.get(chunkKey(tenantId, knowledgeId, chunkId));
		}

		@Override
		public List<TinyflowChunk> findChunksByDocument(String tenantId, String knowledgeId, String documentId) {
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
		public List<TinyflowChunk> findChunksByVectorIds(String tenantId, String knowledgeId, List<String> vectorIds) {
			return Collections.emptyList();
		}

		@Override
		public void updateChunk(String tenantId, String knowledgeId, String chunkId, boolean enabled,
				String content, Map<String, Object> metadata) {
			throw new UnsupportedOperationException();
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
		}

		@Override
		public List<VectorCompensation> findVectorCompensations(String tenantId, String knowledgeId) {
			return Collections.emptyList();
		}

		@Override
		public void deleteVectorCompensation(String tenantId, String knowledgeId, String compensationId) {
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
