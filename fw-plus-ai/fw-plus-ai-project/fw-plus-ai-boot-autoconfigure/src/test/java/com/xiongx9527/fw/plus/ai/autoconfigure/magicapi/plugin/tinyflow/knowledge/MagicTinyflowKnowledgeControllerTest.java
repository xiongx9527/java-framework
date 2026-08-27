package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowChunkMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowDifyConnectionMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowDocumentMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowIngestionTaskMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowKnowledgeKeyMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowKnowledgeMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow.dify.TinyflowKnowledgeAccessKeyService;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class MagicTinyflowKnowledgeControllerTest {

	private MagicRequestContextHolder previousContextHolder;

	@Before
	public void installMagicRequestContextHolder() {
		previousContextHolder = WebUtils.magicRequestContextHolder;
		WebUtils.magicRequestContextHolder = new MagicRequestContextHolder() {
			@Override
			public org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest getRequest() {
				return null;
			}

			@Override
			public MagicHttpServletResponse getResponse() {
				return null;
			}
		};
	}

	@After
	public void restoreMagicRequestContextHolder() {
		WebUtils.magicRequestContextHolder = previousContextHolder;
	}

	@Test
	public void shouldNeverPersistPlainSecrets() throws Exception {
		File root = Files.createTempDirectory("tinyflow-knowledge-secret").toFile();
		MagicResourceService resources = resources(root);
		MagicApiTinyflowKnowledgeRepository repository = new MagicApiTinyflowKnowledgeRepository(resources);
		MagicTinyflowKnowledgeController controller = new MagicTinyflowKnowledgeController(
				repository, new TinyflowKnowledgeAccessKeyService(repository),
				request -> "tenant-a", (tenantId, action, resourceId, request) -> { }, event -> { });

		MagicTinyflowKnowledgeController.CreateKeyRequest keyRequest =
				new MagicTinyflowKnowledgeController.CreateKeyRequest();
		keyRequest.setTenantId("tenant-from-untrusted-body");
		keyRequest.setKnowledgeId("kb-1");
		MagicTinyflowKnowledgeController.CreatedKeyResponse created = controller.createKey(keyRequest, null);
		assertNotNull(created.getToken());

		MagicTinyflowKnowledgeController.DifyConnectionRequest connection =
				new MagicTinyflowKnowledgeController.DifyConnectionRequest();
		connection.setTenantId("tenant-from-untrusted-body");
		connection.setConnectionId("dify-1");
		connection.setKnowledgeId("kb-1");
		connection.setBaseUrl("https://dify.example.test");
		connection.setDatasetId("dataset-1");
		connection.setCredentialRef("vault://dify-main");
		connection.setApiKey("dify-plain-api-key");
		controller.saveDifyConnection(connection, null);

		String dump = dump(root);
		assertFalse(dump.contains(created.getToken()));
		assertFalse(dump.contains("dify-plain-api-key"));
		assertFalse(dump.contains("tenant-from-untrusted-body"));
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

	private String dump(File root) throws Exception {
		StringBuilder result = new StringBuilder();
		Files.walk(root.toPath()).filter(Files::isRegularFile).forEach(path -> {
			try {
				result.append(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
			} catch (Exception exception) {
				throw new IllegalStateException(exception);
			}
		});
		return result.toString();
	}
}
