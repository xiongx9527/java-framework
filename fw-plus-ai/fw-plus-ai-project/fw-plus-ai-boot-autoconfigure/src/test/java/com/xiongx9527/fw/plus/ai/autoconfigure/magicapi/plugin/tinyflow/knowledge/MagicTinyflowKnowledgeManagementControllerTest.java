package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge;

import com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow.dify.TinyflowKnowledgeAccessKeyService;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowChunk;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowIngestionTask;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeDefinition;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRequest;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeResult;
import org.junit.Test;
import org.ssssssss.magicapi.core.service.MagicResourceService;

import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
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

public class MagicTinyflowKnowledgeManagementControllerTest {

	@Test
	public void shouldManageTenantScopedKnowledgeWithBoundedPagination() {
		RecordingOperations operations = new RecordingOperations();
		MagicTinyflowKnowledgeController controller = controller(operations, options());
		MagicTinyflowKnowledgeController.KnowledgeRequest request = knowledgeRequest();
		request.setTenantId("body-tenant");

		MagicTinyflowKnowledgeController.KnowledgeView created = controller.createKnowledge(request, null);
		assertEquals("tenant-a", operations.saved.getTenantId());
		assertEquals("fw-plus-local", operations.saved.getProviderKey());
		assertEquals(1L, created.getVersion());

		MagicTinyflowKnowledgeController.PageResponse<MagicTinyflowKnowledgeController.KnowledgeView> page =
				controller.listKnowledge(1, 20, null);
		assertEquals(1, page.getPage());
		assertEquals(20, page.getSize());
		assertEquals(1L, page.getTotal());
		assertEquals(1, page.getItems().size());
		assertEquals(20, operations.lastLimit);

		request.setName("updated");
		request.setExpectedVersion(1L);
		assertEquals(2L, controller.updateKnowledge("kb-1", request, null).getVersion());
		controller.deleteKnowledge("kb-1", 2L, null);
		assertTrue(operations.deleted);
		assertThrows(IllegalArgumentException.class, () -> controller.listKnowledge(0, 20, null));
		assertThrows(IllegalArgumentException.class, () -> controller.listKnowledge(1, 101, null));
	}

	@Test
	public void shouldImportTextFileAndSafeUrlThroughOneBoundedCommand() throws Exception {
		RecordingOperations operations = new RecordingOperations();
		MagicTinyflowKnowledgeController controller = controller(operations, options());

		MagicTinyflowKnowledgeController.TextImportRequest text = new MagicTinyflowKnowledgeController.TextImportRequest();
		text.setKnowledgeId("kb-1"); text.setSourceId("source-text"); text.setName("note.txt"); text.setContent("hello");
		controller.importText(text, null);
		assertEquals("text", operations.imports.get(0).getSourceType());
		assertEquals("hello", new String(operations.imports.get(0).getContent(), StandardCharsets.UTF_8));

		MagicTinyflowKnowledgeController.FileImportRequest file = new MagicTinyflowKnowledgeController.FileImportRequest();
		file.setKnowledgeId("kb-1"); file.setSourceId("source-file"); file.setFilename("manual.md");
		file.setMediaType("text/markdown"); file.setContent("# file".getBytes(StandardCharsets.UTF_8));
		controller.importFile(file, null);
		assertEquals("file", operations.imports.get(1).getSourceType());

		MagicTinyflowKnowledgeController.UrlImportRequest url = new MagicTinyflowKnowledgeController.UrlImportRequest();
		url.setKnowledgeId("kb-1"); url.setSourceId("source-url"); url.setUrl("https://public.test/manual.md");
		controller.importUrl(url, null);
		assertEquals("url", operations.imports.get(2).getSourceType());
		assertEquals("https://public.test/manual.md", operations.imports.get(2).getSourceUri());
		assertEquals("tenant-a", operations.imports.get(2).getTenantId());
	}

	@Test
	public void shouldManageChunksTasksAndRecallWithExplicitLimits() {
		RecordingOperations operations = new RecordingOperations();
		MagicTinyflowKnowledgeController controller = controller(operations, options());

		assertEquals(1, controller.listChunks("kb-1", "doc-1", 1, 10, null).getItems().size());
		MagicTinyflowKnowledgeController.ChunkUpdateRequest update = new MagicTinyflowKnowledgeController.ChunkUpdateRequest();
		update.setKnowledgeId("kb-1"); update.setChunkId("chunk-1"); update.setContent("updated");
		update.setEnabled(false); update.setMetadata(Collections.<String, Object>singletonMap("page", 2));
		assertFalse(controller.updateChunk(update, null).isEnabled());

		assertEquals(1, controller.listTasks("kb-1", 1, 20, null).getItems().size());
		assertEquals("PENDING", controller.retryTask("kb-1", "task-1", null).getStatus());
		assertEquals("CANCELLED", controller.cancelTask("kb-1", "task-1", null).getStatus());

		MagicTinyflowKnowledgeController.RecallRequest recall = new MagicTinyflowKnowledgeController.RecallRequest();
		recall.setKnowledgeId("kb-1"); recall.setQuery("hello"); recall.setTopK(5); recall.setThreshold(0.5D);
		controller.recall(recall, null);
		assertEquals("tenant-a", operations.recall.getTenantId());
		assertEquals("fw-plus-local", operations.recall.getProviderKey());
		assertEquals(5, operations.recall.getTopK());
		recall.setTopK(101);
		assertThrows(IllegalArgumentException.class, () -> controller.recall(recall, null));
	}

	@Test
	public void shouldExposeOnlyBoundedNonSensitiveOptions() {
		MagicTinyflowKnowledgeController controller = controller(new RecordingOperations(), options());
		List<MagicTinyflowKnowledgeOptionProvider.Option> models = controller.listModelOptions(20, null);
		List<MagicTinyflowKnowledgeOptionProvider.Option> vectors = controller.listVectorOptions(20, null);
		List<MagicTinyflowKnowledgeOptionProvider.Option> s3 = controller.listS3Options(20, null);

		assertEquals("model", models.get(0).getType());
		assertEquals("vector", vectors.get(0).getType());
		assertEquals("s3", s3.get(0).getType());
		assertFalse(models.get(0).toString().contains("secret"));
		assertThrows(IllegalArgumentException.class, () -> controller.listS3Options(201, null));
	}

	@Test
	public void shouldRejectOversizedRequestStrings() {
		MagicTinyflowKnowledgeController.KnowledgeRequest request = knowledgeRequest();
		request.setName(repeat('n', 513));

		assertThrows(IllegalArgumentException.class,
				() -> controller(new RecordingOperations(), options()).createKnowledge(request, null));
	}

	@Test
	public void shouldRejectOversizedUrlBeforeFetch() {
		MagicTinyflowKnowledgeController.UrlImportRequest request = new MagicTinyflowKnowledgeController.UrlImportRequest();
		request.setKnowledgeId("kb-1"); request.setSourceId("source-url");
		request.setUrl("https://public.test/" + repeat('u', 4097));

		assertThrows(IllegalArgumentException.class,
				() -> controller(new RecordingOperations(), options()).importUrl(request, null));
	}

	@Test
	public void shouldRejectOversizedRecallQuery() {
		MagicTinyflowKnowledgeController.RecallRequest request = new MagicTinyflowKnowledgeController.RecallRequest();
		request.setKnowledgeId("kb-1"); request.setQuery(repeat('q', 8193)); request.setTopK(5);

		assertThrows(IllegalArgumentException.class,
				() -> controller(new RecordingOperations(), options()).recall(request, null));
	}

	@Test
	public void shouldRejectTooManyStructuredMapEntries() {
		MagicTinyflowKnowledgeController.KnowledgeRequest request = knowledgeRequest();
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		for (int index = 0; index < 65; index++) metadata.put("key-" + index, index);
		request.setMetadata(metadata);

		assertThrows(IllegalArgumentException.class,
				() -> controller(new RecordingOperations(), options()).createKnowledge(request, null));
	}

	@Test
	public void shouldRejectTooManyStructuredCollectionEntries() {
		MagicTinyflowKnowledgeController.RecallRequest request = new MagicTinyflowKnowledgeController.RecallRequest();
		request.setKnowledgeId("kb-1"); request.setQuery("hello"); request.setTopK(5);
		List<Integer> values = new ArrayList<Integer>();
		for (int index = 0; index < 65; index++) values.add(index);
		request.setOptions(Collections.<String, Object>singletonMap("values", values));

		assertThrows(IllegalArgumentException.class,
				() -> controller(new RecordingOperations(), options()).recall(request, null));
	}

	@Test
	public void shouldRejectOverdeepStructuredOptions() {
		MagicTinyflowKnowledgeController.RecallRequest request = new MagicTinyflowKnowledgeController.RecallRequest();
		request.setKnowledgeId("kb-1"); request.setQuery("hello"); request.setTopK(5);
		Map<String, Object> root = new LinkedHashMap<String, Object>();
		Map<String, Object> current = root;
		for (int depth = 0; depth < 9; depth++) {
			Map<String, Object> child = new LinkedHashMap<String, Object>();
			current.put("child", child); current = child;
		}
		request.setOptions(root);

		assertThrows(IllegalArgumentException.class,
				() -> controller(new RecordingOperations(), options()).recall(request, null));
	}

	private MagicTinyflowKnowledgeController controller(RecordingOperations operations,
			MagicTinyflowKnowledgeOptionProvider optionProvider) {
		MagicApiTinyflowKnowledgeRepository repository = new MagicApiTinyflowKnowledgeRepository(
				(MagicResourceService) Proxy.newProxyInstance(MagicResourceService.class.getClassLoader(),
						new Class<?>[]{MagicResourceService.class}, (proxy, method, args) -> null));
		MagicTinyflowSafeUrlImporter.DnsResolver resolver = host ->
				new InetAddress[]{InetAddress.getByName("93.184.216.34")};
		MagicTinyflowSafeUrlImporter.HttpTransport transport = (uri, addresses, connectTimeoutMillis,
				readTimeoutMillis, maxBytes) -> new MagicTinyflowSafeUrlImporter.HttpResponse(200,
				Collections.singletonMap("Content-Type", "text/markdown"),
				"# url".getBytes(StandardCharsets.UTF_8));
		return new MagicTinyflowKnowledgeController(repository, new TinyflowKnowledgeAccessKeyService(repository),
				request -> "tenant-a", (tenantId, action, resourceId, request) -> { }, event -> { }, null,
				operations, new MagicTinyflowSafeUrlImporter(resolver, transport, 1024L, 2, 100, 100,
						Arrays.asList("text/plain", "text/markdown")), optionProvider);
	}

	private MagicTinyflowKnowledgeController.KnowledgeRequest knowledgeRequest() {
		MagicTinyflowKnowledgeController.KnowledgeRequest request = new MagicTinyflowKnowledgeController.KnowledgeRequest();
		request.setKnowledgeId("kb-1"); request.setName("Knowledge"); request.setDescription("description");
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		metadata.put("vectorStoreKey", "vector-main"); metadata.put("s3Key", "s3-main");
		request.setMetadata(metadata); request.setEnabled(true); request.setExpectedVersion(0L);
		return request;
	}

	private String repeat(char value, int length) {
		char[] chars = new char[length];
		Arrays.fill(chars, value);
		return new String(chars);
	}

	private MagicTinyflowKnowledgeOptionProvider options() {
		return new MagicTinyflowKnowledgeOptionProvider() {
			@Override public List<Option> listModels(int limit) { return option("model"); }
			@Override public List<Option> listVectors(int limit) { return option("vector"); }
			@Override public List<Option> listS3(int limit) { return option("s3"); }
			@Override public boolean hasEmbeddingModel(String key) { return "model-1".equals(key); }
			@Override public boolean hasVectorStore(String key) { return "vector-1".equals(key); }
			@Override public boolean hasS3Storage(String key) { return "s3-1".equals(key); }
			private List<Option> option(String type) {
				return Collections.singletonList(new Option(type + "-1", type + " name", type, "provider"));
			}
		};
	}

	private static final class RecordingOperations implements MagicTinyflowKnowledgeOperations {
		private TinyflowKnowledgeDefinition saved;
		private boolean deleted;
		private int lastLimit;
		private final List<ImportCommand> imports = new ArrayList<ImportCommand>();
		private TinyflowKnowledgeRequest recall;

		@Override public KnowledgeRecord create(TinyflowKnowledgeDefinition definition) {
			saved = definition; return new KnowledgeRecord(definition, 1L);
		}
		@Override public KnowledgeRecord update(TinyflowKnowledgeDefinition definition, long expectedVersion) {
			saved = definition; return new KnowledgeRecord(definition, expectedVersion + 1L);
		}
		@Override public KnowledgeRecord find(String tenantId, String knowledgeId) {
			return saved == null ? null : new KnowledgeRecord(saved, 1L);
		}
		@Override public PageResult<KnowledgeRecord> listKnowledge(String tenantId, int page, int size) {
			lastLimit = size;
			TinyflowKnowledgeDefinition value = saved == null ? definition(tenantId) : saved;
			return new PageResult<KnowledgeRecord>(page, size, 1L,
					Collections.singletonList(new KnowledgeRecord(value, 1L)));
		}
		@Override public void delete(String tenantId, String knowledgeId, long expectedVersion) { deleted = true; }
		@Override public TinyflowIngestionTask importDocument(ImportCommand command) {
			imports.add(command); return task(command.getTenantId(), command.getKnowledgeId(), "PENDING");
		}
		@Override public PageResult<TinyflowChunk> listChunks(String tenantId, String knowledgeId,
				String documentId, int page, int size) {
			return new PageResult<TinyflowChunk>(page, size, 1L,
					Collections.singletonList(chunk(tenantId, knowledgeId, true, "content")));
		}
		@Override public TinyflowChunk updateChunk(String tenantId, String knowledgeId, String chunkId,
				boolean enabled, String content, Map<String, Object> metadata) {
			return chunk(tenantId, knowledgeId, enabled, content);
		}
		@Override public PageResult<TinyflowIngestionTask> listTasks(String tenantId, String knowledgeId,
				int page, int size) {
			return new PageResult<TinyflowIngestionTask>(page, size, 1L,
					Collections.singletonList(task(tenantId, knowledgeId, "FAILED")));
		}
		@Override public TinyflowIngestionTask retry(String tenantId, String knowledgeId, String taskId) {
			return task(tenantId, knowledgeId, "PENDING");
		}
		@Override public TinyflowIngestionTask cancel(String tenantId, String knowledgeId, String taskId) {
			return task(tenantId, knowledgeId, "CANCELLED");
		}
		@Override public TinyflowKnowledgeResult retrieve(TinyflowKnowledgeRequest request) {
			recall = request; return TinyflowKnowledgeResult.empty();
		}
		private TinyflowKnowledgeDefinition definition(String tenantId) {
			return TinyflowKnowledgeDefinition.builder().providerKey("fw-plus-local").tenantId(tenantId)
					.knowledgeId("kb-1").name("Knowledge").enabled(true).build();
		}
		private TinyflowChunk chunk(String tenantId, String knowledgeId, boolean enabled, String content) {
			return TinyflowChunk.builder().tenantId(tenantId).knowledgeId(knowledgeId).sourceId("source-1")
					.sourceType("text").documentId("doc-1").chunkId("chunk-1").content(content)
					.enabled(enabled).build();
		}
		private TinyflowIngestionTask task(String tenantId, String knowledgeId, String status) {
			return TinyflowIngestionTask.builder().taskId("task-1").providerKey("fw-plus-local")
					.tenantId(tenantId).knowledgeId(knowledgeId).documentId("doc-1").status(status).build();
		}
	}
}
