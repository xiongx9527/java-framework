package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowChunkMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowDifyConnectionMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowDocumentMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowIngestionTaskMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowKnowledgeKeyMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowKnowledgeMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowChunk;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDocument;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDocumentImport;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowIngestionTask;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeCapabilities;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeDefinition;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeProviderRegistry;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRequest;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeResult;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowObjectStorage;
import com.xiongx9527.fw.plus.ai.tinyflow.local.index.TinyflowKnowledgeMaintenanceService;
import com.xiongx9527.fw.plus.ai.tinyflow.local.index.TinyflowLocalIndexer;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowEmbeddingModel;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowModelDescriptor;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowModelProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStore;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStoreDescriptor;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStoreProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorDocument;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorMatch;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.ssssssss.magicapi.core.resource.FileResource;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.core.service.impl.DefaultMagicResourceService;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletResponse;
import org.ssssssss.magicapi.core.servlet.MagicRequestContextHolder;
import org.ssssssss.magicapi.utils.WebUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class DefaultMagicTinyflowKnowledgeOperationsTest {

	private MagicRequestContextHolder previous;

	@Before
	public void installContext() {
		previous = WebUtils.magicRequestContextHolder;
		WebUtils.magicRequestContextHolder = new MagicRequestContextHolder() {
			@Override public MagicHttpServletRequest getRequest() { return null; }
			@Override public MagicHttpServletResponse getResponse() { return null; }
		};
	}

	@After
	public void restoreContext() { WebUtils.magicRequestContextHolder = previous; }

	@Test
	public void shouldPersistVersionedKnowledgeAndReturnBoundedPages() throws Exception {
		Fixture fixture = fixture();
		TinyflowKnowledgeDefinition definition = definition("Knowledge");
		assertEquals(1L, fixture.operations.create(definition).getVersion());
		assertEquals(1, fixture.operations.listKnowledge("tenant-a", 1, 20).getItems().size());

		TinyflowKnowledgeDefinition updated = definition("Updated");
		assertEquals(2L, fixture.operations.update(updated, 1L).getVersion());
		assertEquals("Updated", fixture.operations.find("tenant-a", "kb-1").getDefinition().getName());
		fixture.operations.delete("tenant-a", "kb-1", 2L);
		assertNull(fixture.operations.find("tenant-a", "kb-1"));
	}

	@Test
	public void shouldDeleteEverySourceBeforeRemovingKnowledgeDefinition() throws Exception {
		Fixture fixture = fixture();
		fixture.operations.create(definition("Knowledge"));
		seedSource(fixture.repository, "source-1", "doc-1", "chunk-1", "vec-1");
		seedSource(fixture.repository, "source-2", "doc-2", "chunk-2", "vec-2");

		fixture.operations.delete("tenant-a", "kb-1", 1L);

		assertNull(fixture.operations.find("tenant-a", "kb-1"));
		assertTrue(fixture.repository.findDocumentVersions("tenant-a", "kb-1", "source-1").isEmpty());
		assertTrue(fixture.repository.findDocumentVersions("tenant-a", "kb-1", "source-2").isEmpty());
		assertTrue(fixture.repository.findChunksByDocument("tenant-a", "kb-1", "doc-1").isEmpty());
		assertTrue(fixture.repository.findChunksByDocument("tenant-a", "kb-1", "doc-2").isEmpty());
		assertEquals(2, fixture.vectorStore.removed.size());
		assertTrue(fixture.vectorStore.removed.containsAll(Arrays.asList("vec-1", "vec-2")));
	}

	@Test
	public void shouldKeepKnowledgeDefinitionWhenSourceCleanupIsPending() throws Exception {
		Fixture fixture = fixture();
		fixture.operations.create(definition("Knowledge"));
		seedSource(fixture.repository, "source-1", "doc-1", "chunk-1", "vec-1");
		fixture.vectorStore.failRemove = true;

		assertThrows(RuntimeException.class,
				() -> fixture.operations.delete("tenant-a", "kb-1", 1L));

		assertNotNull(fixture.operations.find("tenant-a", "kb-1"));
		assertFalse(fixture.repository.findDocumentVersions("tenant-a", "kb-1", "source-1").isEmpty());
		assertEquals(1, fixture.repository.findVectorCompensations("tenant-a", "kb-1").size());
	}

	@Test
	public void shouldReindexChunkContentWithTemporaryVectorDocumentId() throws Exception {
		Fixture fixture = fixture();
		fixture.operations.create(definition("Knowledge"));
		seedSource(fixture.repository, "source-1", "doc-1", "chunk-1", "vec-old");

		TinyflowChunk updated = fixture.operations.updateChunk("tenant-a", "kb-1", "chunk-1",
				true, "updated content", Collections.<String, Object>singletonMap("tag", "updated"));

		assertEquals("updated content", updated.getContent());
		assertEquals(Collections.singletonList("vec-new-1"), updated.getVectorIds());
		assertEquals(Collections.singletonList("vec-old"), fixture.vectorStore.removed);
		assertEquals(1, fixture.vectorStore.added.size());
		assertFalse("chunk-1".equals(fixture.vectorStore.added.get(0).getId()));
		assertEquals("chunk-1", fixture.vectorStore.added.get(0).getMetadata().get("chunk_id"));
	}

	@Test
	public void shouldReindexChunkWhenOnlyMetadataChanges() throws Exception {
		Fixture fixture = fixture();
		fixture.operations.create(definition("Knowledge"));
		seedSource(fixture.repository, "source-1", "doc-1", "chunk-1", "vec-old");

		TinyflowChunk updated = fixture.operations.updateChunk("tenant-a", "kb-1", "chunk-1",
				true, "content", Collections.<String, Object>singletonMap("tag", "updated"));

		assertEquals(Collections.singletonList("vec-new-1"), updated.getVectorIds());
		assertEquals("updated", updated.getMetadata().get("tag"));
		assertEquals(Collections.singletonList("vec-old"), fixture.vectorStore.removed);
	}

	@Test
	public void shouldUpdateEnabledWithoutReindexingChunk() throws Exception {
		Fixture fixture = fixture();
		fixture.operations.create(definition("Knowledge"));
		seedSource(fixture.repository, "source-1", "doc-1", "chunk-1", "vec-old");

		TinyflowChunk updated = fixture.operations.updateChunk("tenant-a", "kb-1", "chunk-1",
				false, "content", Collections.<String, Object>emptyMap());

		assertFalse(updated.isEnabled());
		assertEquals(Collections.singletonList("vec-old"), updated.getVectorIds());
		assertTrue(fixture.vectorStore.added.isEmpty());
		assertTrue(fixture.vectorStore.removed.isEmpty());
	}

	@Test
	public void shouldKeepOldChunkAndVectorWhenAtomicReplacementFails() throws Exception {
		Fixture fixture = fixture();
		fixture.operations.create(definition("Knowledge"));
		seedSource(fixture.repository, "source-1", "doc-1", "chunk-1", "vec-old");
		fixture.repository.failNextCompletionAt("chunk-replacement");

		assertThrows(RuntimeException.class, () -> fixture.operations.updateChunk(
				"tenant-a", "kb-1", "chunk-1", true, "updated content",
				Collections.<String, Object>singletonMap("tag", "updated")));

		TinyflowChunk unchanged = fixture.repository.findChunk("tenant-a", "kb-1", "chunk-1");
		assertEquals("content", unchanged.getContent());
		assertEquals(Collections.singletonList("vec-old"), unchanged.getVectorIds());
		assertEquals(Collections.singletonList("vec-new-1"), fixture.vectorStore.removed);
	}

	@Test
	public void shouldReportPendingWhenReplacedVectorCleanupFails() throws Exception {
		Fixture fixture = fixture();
		fixture.operations.create(definition("Knowledge"));
		seedSource(fixture.repository, "source-1", "doc-1", "chunk-1", "vec-old");
		fixture.vectorStore.failRemove = true;

		RuntimeException error = assertThrows(RuntimeException.class, () -> fixture.operations.updateChunk(
				"tenant-a", "kb-1", "chunk-1", true, "updated content",
				Collections.<String, Object>singletonMap("tag", "updated")));

		assertTrue(error.getMessage().contains("pending"));
		TinyflowChunk updated = fixture.repository.findChunk("tenant-a", "kb-1", "chunk-1");
		assertEquals(Collections.singletonList("vec-new-1"), updated.getVectorIds());
		assertEquals(1, fixture.repository.findVectorCompensations("tenant-a", "kb-1").size());
		assertEquals("DELETE_REPLACED_CHUNK_VECTORS", fixture.repository
				.findVectorCompensations("tenant-a", "kb-1").get(0).getOperation());
	}

	@Test
	public void shouldRejectStaleChunkVectorReplacement() throws Exception {
		Fixture fixture = fixture();
		fixture.operations.create(definition("Knowledge"));
		seedSource(fixture.repository, "source-1", "doc-1", "chunk-1", "vec-current");
		TinyflowChunk replacement = TinyflowChunk.builder().tenantId("tenant-a").knowledgeId("kb-1")
				.sourceId("source-1").sourceType("text").documentId("doc-1").chunkId("chunk-1")
				.content("stale replacement").vectorIds(Collections.singletonList("vec-new")).build();
		TinyflowKnowledgeRepository.VectorCompensation stale = new TinyflowKnowledgeRepository.VectorCompensation(
				"comp-stale", "tenant-a", "kb-1", "source-1", "doc-1",
				Collections.singletonList("vec-stale"), "DELETE_REPLACED_CHUNK_VECTORS");

		assertThrows(RuntimeException.class,
				() -> fixture.repository.replaceChunkWithVectorCompensation(replacement, stale));

		TinyflowChunk unchanged = fixture.repository.findChunk("tenant-a", "kb-1", "chunk-1");
		assertEquals("content", unchanged.getContent());
		assertEquals(Collections.singletonList("vec-current"), unchanged.getVectorIds());
		assertTrue(fixture.repository.findVectorCompensations("tenant-a", "kb-1").isEmpty());
	}

	@Test
	public void shouldStoreFileAndUrlButKeepTextInlineAndRetryFailedTask() throws Exception {
		Fixture fixture = fixture();
		fixture.operations.create(definition("Knowledge"));

		fixture.operations.importDocument(command("source-text", "text", "note.txt", "text/plain", "text"));
		assertEquals("text", fixture.gateway.imports.get(0).getDocument().getContent());
		assertEquals(0, fixture.storage.puts);

		fixture.operations.importDocument(command("source-file", "file", "manual.md", "text/markdown", "file"));
		assertEquals("object-ref-1", fixture.gateway.imports.get(1).getDocument().getSourceUri());
		assertNull(fixture.gateway.imports.get(1).getDocument().getContent());
		assertEquals("s3-main", fixture.storage.lastMetadata.get("s3Key"));

		fixture.operations.importDocument(command("source-url", "url", "remote.md", "text/markdown", "url"));
		assertEquals(2, fixture.storage.puts);

		TinyflowDocument failedDocument = TinyflowDocument.builder().tenantId("tenant-a").knowledgeId("kb-1")
				.sourceId("source-retry").sourceType("file").documentId("doc-failed").checksum("retry-sum")
				.name("retry.md").sourceUri("object-ref-retry")
				.metadata(Collections.<String, Object>singletonMap("mediaType", "text/markdown")).build();
		fixture.repository.saveDocument(failedDocument);
		Map<String, Object> taskMetadata = new LinkedHashMap<String, Object>();
		taskMetadata.put("sourceId", "source-retry"); taskMetadata.put("checksum", "retry-sum");
		fixture.repository.saveTask(TinyflowIngestionTask.builder().taskId("task-failed").providerKey("fw-plus-local")
				.tenantId("tenant-a").knowledgeId("kb-1").documentId("doc-failed").status("FAILED")
				.metadata(taskMetadata).build());
		fixture.operations.retry("tenant-a", "kb-1", "task-failed");
		assertEquals("source-retry", fixture.gateway.imports.get(3).getDocument().getSourceId());
		assertEquals("CANCELLED", fixture.operations.cancel("tenant-a", "kb-1", "task-live").getStatus());
	}

	@Test
	public void shouldPageChunksTasksAndRouteRecallOnlyToLocalProvider() throws Exception {
		Fixture fixture = fixture();
		fixture.operations.create(definition("Knowledge"));
		fixture.repository.saveChunks(Collections.singletonList(TinyflowChunk.builder().tenantId("tenant-a")
				.knowledgeId("kb-1").sourceId("source-1").sourceType("text").documentId("doc-1")
				.chunkId("chunk-1").content("content").build()));
		fixture.repository.saveTask(TinyflowIngestionTask.builder().taskId("task-1").providerKey("fw-plus-local")
				.tenantId("tenant-a").knowledgeId("kb-1").documentId("doc-1").status("COMPLETED").build());

		assertEquals(1, fixture.operations.listChunks("tenant-a", "kb-1", "doc-1", 1, 10).getItems().size());
		assertEquals(1, fixture.operations.listTasks("tenant-a", "kb-1", 1, 10).getItems().size());
		fixture.operations.retrieve(TinyflowKnowledgeRequest.builder().providerKey("fw-plus-local")
				.tenantId("tenant-a").knowledgeId("kb-1").query("query").topK(5).build());
		assertEquals("tenant-a", fixture.provider.request.getTenantId());
	}

	private Fixture fixture() throws Exception {
		MagicApiTinyflowKnowledgeRepository repository = new MagicApiTinyflowKnowledgeRepository(
				resources(Files.createTempDirectory("tinyflow-operations").toFile()));
		RecordingStorage storage = new RecordingStorage();
		RecordingGateway gateway = new RecordingGateway();
		RecordingProvider provider = new RecordingProvider();
		TinyflowKnowledgeProviderRegistry registry = new TinyflowKnowledgeProviderRegistry(
				Collections.<TinyflowKnowledgeProvider>singletonList(provider));
		RecordingVectorStore vectorStore = new RecordingVectorStore();
		TinyflowVectorStoreProvider vectors = new TinyflowVectorStoreProvider() {
			@Override public List<TinyflowVectorStoreDescriptor> list() {
				return Collections.singletonList(new TinyflowVectorStoreDescriptor(
						"vector-main", "Vector", "vector", "test", true));
			}
			@Override public TinyflowVectorStore get(String key) { return vectorStore; }
		};
		TinyflowModelProvider models = new TinyflowModelProvider() {
			@Override public List<TinyflowModelDescriptor> listChatModels() { return Collections.emptyList(); }
			@Override public List<TinyflowModelDescriptor> listEmbeddingModels() { return Collections.emptyList(); }
			@Override public dev.langchain4j.model.chat.ChatLanguageModel getChatModel(String key) { return null; }
			@Override public TinyflowEmbeddingModel getEmbeddingModel(String key) {
				return text -> new float[] { text.length() };
			}
		};
		TinyflowLocalIndexer indexer = new TinyflowLocalIndexer(
				repository, models, vectors, "embedding-main", "vector-main", 64);
		TinyflowKnowledgeMaintenanceService maintenance =
				new TinyflowKnowledgeMaintenanceService(repository, indexer, vectors);
		return new Fixture(repository, storage, gateway, provider, vectorStore,
				new DefaultMagicTinyflowKnowledgeOperations(
						repository, storage, gateway, registry, vectors, maintenance));
	}

	private void seedSource(MagicApiTinyflowKnowledgeRepository repository, String sourceId,
			String documentId, String chunkId, String vectorId) {
		repository.saveDocument(TinyflowDocument.builder().tenantId("tenant-a").knowledgeId("kb-1")
				.sourceId(sourceId).sourceType("text").documentId(documentId).checksum("checksum-" + documentId)
				.name(documentId + ".txt").content("content").build());
		repository.saveChunks(Collections.singletonList(TinyflowChunk.builder().tenantId("tenant-a")
				.knowledgeId("kb-1").sourceId(sourceId).sourceType("text").documentId(documentId)
				.chunkId(chunkId).content("content").vectorIds(Collections.singletonList(vectorId)).build()));
		repository.activateDocumentVersion("tenant-a", "kb-1", sourceId, documentId);
	}

	private MagicTinyflowKnowledgeOperations.ImportCommand command(String sourceId, String sourceType,
			String name, String mediaType, String content) {
		return new MagicTinyflowKnowledgeOperations.ImportCommand("tenant-a", "kb-1", sourceId, sourceType,
				name, mediaType, content.getBytes(StandardCharsets.UTF_8),
				"url".equals(sourceType) ? "https://public.test/remote.md" : null,
				Collections.<String, Object>singletonMap("chunkStrategy", "normal"));
	}

	private TinyflowKnowledgeDefinition definition(String name) {
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		metadata.put("embeddingModelKey", "embedding-main"); metadata.put("vectorStoreKey", "vector-main");
		metadata.put("s3Key", "s3-main");
		return TinyflowKnowledgeDefinition.builder().providerKey("fw-plus-local").tenantId("tenant-a")
				.knowledgeId("kb-1").name(name).metadata(metadata).enabled(true).build();
	}

	private MagicResourceService resources(File root) {
		List<MagicResourceStorage<? extends org.ssssssss.magicapi.core.model.MagicEntity>> storages = Arrays
				.<MagicResourceStorage<? extends org.ssssssss.magicapi.core.model.MagicEntity>>asList(
						new TinyflowKnowledgeMagicResourceStorage(), new TinyflowDocumentMagicResourceStorage(),
						new TinyflowChunkMagicResourceStorage(), new TinyflowIngestionTaskMagicResourceStorage(),
						new TinyflowKnowledgeKeyMagicResourceStorage(), new TinyflowDifyConnectionMagicResourceStorage());
		DefaultMagicResourceService service = new DefaultMagicResourceService(
				new FileResource(root, false, root.getAbsolutePath()), storages, new ApplicationEventPublisher() {
					@Override public void publishEvent(Object event) { }
				});
		service.refresh();
		return service;
	}

	private static final class Fixture {
		private final MagicApiTinyflowKnowledgeRepository repository;
		private final RecordingStorage storage;
		private final RecordingGateway gateway;
		private final RecordingProvider provider;
		private final RecordingVectorStore vectorStore;
		private final DefaultMagicTinyflowKnowledgeOperations operations;
		private Fixture(MagicApiTinyflowKnowledgeRepository repository, RecordingStorage storage,
				RecordingGateway gateway, RecordingProvider provider, RecordingVectorStore vectorStore,
				DefaultMagicTinyflowKnowledgeOperations operations) {
			this.repository = repository; this.storage = storage; this.gateway = gateway;
			this.provider = provider; this.vectorStore = vectorStore; this.operations = operations;
		}
	}

	private static final class RecordingVectorStore implements TinyflowVectorStore {
		private final List<TinyflowVectorDocument> added = new ArrayList<TinyflowVectorDocument>();
		private final List<String> removed = new ArrayList<String>();
		private boolean failRemove;
		@Override public List<String> addAll(List<TinyflowVectorDocument> documents) {
			List<String> ids = new ArrayList<String>();
			for (TinyflowVectorDocument document : documents) {
				added.add(document);
				ids.add("vec-new-" + added.size());
			}
			return ids;
		}
		@Override public void removeAll(List<String> ids) {
			if (failRemove) throw new IllegalStateException("vector delete failed");
			removed.addAll(ids);
		}
		@Override public List<TinyflowVectorMatch> search(
				float[] vector, int maxResults, double minScore) { return Collections.emptyList(); }
	}

	private static final class RecordingGateway implements DefaultMagicTinyflowKnowledgeOperations.IngestionGateway {
		private final List<TinyflowDocumentImport> imports = new ArrayList<TinyflowDocumentImport>();
		@Override public TinyflowIngestionTask importDocument(TinyflowDocumentImport request) {
			imports.add(request);
			return TinyflowIngestionTask.builder().taskId("task-" + imports.size()).providerKey("fw-plus-local")
					.tenantId(request.getTenantId()).knowledgeId(request.getKnowledgeId())
					.documentId(request.getDocument().getDocumentId()).status("PENDING").build();
		}
		@Override public TinyflowIngestionTask cancel(String tenantId, String knowledgeId, String taskId) {
			return TinyflowIngestionTask.builder().taskId(taskId).providerKey("fw-plus-local").tenantId(tenantId)
					.knowledgeId(knowledgeId).documentId("doc-live").status("CANCELLED").build();
		}
	}

	private static final class RecordingStorage implements TinyflowObjectStorage {
		private int puts;
		private Map<String, Object> lastMetadata;
		@Override public StoredObject put(String tenantId, String objectKey, InputStream input, String mediaType,
				Map<String, Object> metadata) {
			puts++; lastMetadata = metadata;
			return new StoredObject(tenantId, "object-ref-" + puts, mediaType, 4L, "storage-sum-" + puts, metadata);
		}
		@Override public InputStream get(String tenantId, String objectKey) {
			return new ByteArrayInputStream(new byte[0]);
		}
		@Override public boolean exists(String tenantId, String objectKey) { return true; }
		@Override public void delete(String tenantId, String objectKey) { }
	}

	private static final class RecordingProvider implements TinyflowKnowledgeProvider {
		private TinyflowKnowledgeRequest request;
		@Override public String getKey() { return "fw-plus-local"; }
		@Override public TinyflowKnowledgeCapabilities capabilities() { return TinyflowKnowledgeCapabilities.managed(); }
		@Override public TinyflowKnowledgeResult retrieve(TinyflowKnowledgeRequest request) {
			this.request = request; return TinyflowKnowledgeResult.empty();
		}
	}
}
