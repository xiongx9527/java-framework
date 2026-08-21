package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowChunkMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowDifyConnectionMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowDocumentMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowIngestionTaskMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowKnowledgeKeyMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowKnowledgeMagicResourceStorage;
import org.junit.Test;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class TinyflowKnowledgeMagicResourceTest {

	private final List<MagicResourceStorage<?>> storages = Arrays.<MagicResourceStorage<?>>asList(
			new TinyflowKnowledgeMagicResourceStorage(),
			new TinyflowDocumentMagicResourceStorage(),
			new TinyflowChunkMagicResourceStorage(),
			new TinyflowIngestionTaskMagicResourceStorage(),
			new TinyflowKnowledgeKeyMagicResourceStorage(),
			new TinyflowDifyConnectionMagicResourceStorage());

	@Test
	public void shouldRegisterExactlySixKnowledgeResourceFolders() {
		assertEquals(new HashSet<String>(Arrays.asList("tinyflow-knowledge", "tinyflow-document",
				"tinyflow-chunk", "tinyflow-ingestion-task", "tinyflow-knowledge-key",
				"tinyflow-dify-connection")), folders(storages));
		for (MagicResourceStorage<?> storage : storages) {
			assertEquals(".json", storage.suffix());
			assertFalse(storage.requiredScript());
			assertTrue(storage.allowRoot());
		}
	}

	@Test
	public void shouldRejectMissingTenantAndChunkSourceIdentity() {
		TinyflowKnowledgeMagicResource resource = new TinyflowKnowledgeMagicResource();
		resource.setKnowledgeId("kb-1");
		resource.setName("kb-1");
		assertThrows(IllegalArgumentException.class,
				() -> new TinyflowKnowledgeMagicResourceStorage().validate(resource));

		resource.setTenantId("tenant-a");
		resource.setChunkId("chunk-1");
		assertThrows(IllegalArgumentException.class,
				() -> new TinyflowChunkMagicResourceStorage().validate(resource));
	}

	private Set<String> folders(List<MagicResourceStorage<?>> values) {
		Set<String> result = new HashSet<String>();
		for (MagicResourceStorage<?> storage : values) {
			result.add(storage.folder());
		}
		return result;
	}
}
