package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowChunkMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowDifyConnectionMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowDocumentMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowIngestionTaskMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowKnowledgeKeyMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowKnowledgeMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowChunk;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDocument;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowIngestionTask;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeDefinition;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.ssssssss.magicapi.core.resource.FileResource;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.core.service.impl.DefaultMagicResourceService;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletResponse;
import org.ssssssss.magicapi.core.servlet.MagicRequestContextHolder;
import org.ssssssss.magicapi.utils.WebUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class MagicApiTinyflowKnowledgeRepositoryPersistenceTest {

	private MagicRequestContextHolder previousContextHolder;

	@Before
	public void installMagicRequestContextHolder() {
		previousContextHolder = WebUtils.magicRequestContextHolder;
		WebUtils.magicRequestContextHolder = new MagicRequestContextHolder() {
			@Override public org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest getRequest() { return null; }
			@Override public MagicHttpServletResponse getResponse() { return null; }
		};
	}

	@After
	public void restoreMagicRequestContextHolder() {
		WebUtils.magicRequestContextHolder = previousContextHolder;
	}

	@Test
	public void shouldRecoverScopedResourcesAndDisplayNamesAfterRestartAndRename() throws Exception {
		File root = Files.createTempDirectory("tinyflow-knowledge-restart").toFile();
		MagicApiTinyflowKnowledgeRepository first = repository(service(root));
		first.saveKnowledge(knowledge("kb-1", "Initial name / 中文"), 0L);
		String filename = singleFilename(new File(root, TinyflowKnowledgeMagicResourceStorage.FOLDER));

		first.saveKnowledge(knowledge("kb-1", "Renamed name / 中文"), 1L);
		first.saveChunks(Arrays.asList(chunk("kb-1", "knowledge-one"), chunk("kb-2", "knowledge-two")));
		assertEquals(filename, singleFilename(new File(root, TinyflowKnowledgeMagicResourceStorage.FOLDER)));
		assertTrue(filename.startsWith("tfk1."));

		MagicApiTinyflowKnowledgeRepository restarted = repository(service(root));
		assertEquals("Renamed name / 中文", restarted.findKnowledge("tenant-a", "kb-1").getName());
		assertEquals("knowledge-one", restarted.findChunk("tenant-a", "kb-1", "chunk-shared").getContent());
		assertEquals("knowledge-two", restarted.findChunk("tenant-a", "kb-2", "chunk-shared").getContent());
	}

	@Test
	public void shouldPreserveOriginalFailureAndFailClosedWhenRollbackFails() throws Exception {
		File root = Files.createTempDirectory("tinyflow-knowledge-rollback-failure").toFile();
		MagicResourceService delegate = service(root);
		final IllegalStateException rollbackFailure = new IllegalStateException("rollback delete failed");
		MagicResourceService failing = (MagicResourceService) Proxy.newProxyInstance(
				MagicResourceService.class.getClassLoader(), new Class<?>[]{MagicResourceService.class},
				(Object proxy, Method method, Object[] args) -> {
					if ("delete".equals(method.getName())) throw rollbackFailure;
					try {
						return method.invoke(delegate, args);
					} catch (InvocationTargetException exception) {
						throw exception.getCause();
					}
				});
		MagicApiTinyflowKnowledgeRepository repository = repository(failing);
		repository.failNextCompletionAt("chunks");

		IllegalStateException original = assertThrows(IllegalStateException.class,
				() -> repository.completeIngestion(document(), Collections.singletonList(chunk("kb-1", "content")), task()));

		assertEquals("Completion failed after chunks", original.getMessage());
		assertEquals(1, original.getSuppressed().length);
		assertSame(rollbackFailure, original.getSuppressed()[0]);
		IllegalStateException diagnostic = assertThrows(IllegalStateException.class,
				() -> repository.findKnowledge("tenant-a", "kb-1"));
		assertEquals("Magic knowledge repository rollback failed", diagnostic.getMessage());
		assertSame(rollbackFailure, diagnostic.getCause());
	}

	private MagicApiTinyflowKnowledgeRepository repository(MagicResourceService service) {
		return new MagicApiTinyflowKnowledgeRepository(service);
	}

	private DefaultMagicResourceService service(File root) {
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

	private TinyflowKnowledgeDefinition knowledge(String knowledgeId, String name) {
		return TinyflowKnowledgeDefinition.builder().providerKey("fw-plus-local").tenantId("tenant-a")
				.knowledgeId(knowledgeId).name(name).build();
	}

	private TinyflowChunk chunk(String knowledgeId, String content) {
		return TinyflowChunk.builder().tenantId("tenant-a").knowledgeId(knowledgeId).sourceId("source-1")
				.sourceType("text").documentId("document-shared").chunkId("chunk-shared").content(content).build();
	}

	private TinyflowDocument document() {
		return TinyflowDocument.builder().tenantId("tenant-a").knowledgeId("kb-1").sourceId("source-1")
				.sourceType("text").documentId("document-shared").checksum("checksum-1").content("content").build();
	}

	private TinyflowIngestionTask task() {
		return TinyflowIngestionTask.builder().taskId("task-1").providerKey("fw-plus-local").tenantId("tenant-a")
				.knowledgeId("kb-1").documentId("document-shared").status("COMPLETED").build();
	}

	private String singleFilename(File folder) {
		File[] files = folder.listFiles((directory, name) -> name.endsWith(".json"));
		assertEquals(1, files == null ? 0 : files.length);
		return files[0].getName().substring(0, files[0].getName().length() - ".json".length());
	}
}
