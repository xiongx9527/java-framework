package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.model.TinyflowResource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.service.TinyflowMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.service.TinyflowResourceDynamicRegistry;
import com.xiongx9527.fw.plus.ai.tinyflow.TinyflowException;
import com.xiongx9527.fw.plus.ai.tinyflow.runtime.DefaultTinyflowService;
import com.xiongx9527.fw.plus.ai.tinyflow.runtime.InMemoryChainDefinitionRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.runtime.InMemoryTinyflowChainStateRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.runtime.TinyflowService;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.InMemoryTinyflowWorkflowRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowWorkflowValidator;
import dev.tinyflow.core.chain.repository.InMemoryNodeStateRepository;
import dev.tinyflow.core.chain.runtime.ChainExecutor;
import dev.tinyflow.core.parser.ChainParser;
import org.junit.Test;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.resource.FileResource;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletResponse;
import org.ssssssss.magicapi.core.servlet.MagicRequestContextHolder;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.core.service.impl.DefaultMagicResourceService;
import org.ssssssss.magicapi.utils.WebUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class TinyflowMagicResourceStorageTest {

	private final TinyflowMagicResourceStorage storage =
			new TinyflowMagicResourceStorage(new TinyflowWorkflowValidator(ChainParser.builder().build()));

	@Test
	public void shouldWriteNativeTinyflowJsonWithoutWrapper() {
		TinyflowResource resource = resource("flow-id", "orders/create", validWorkflow());

		String json = new String(storage.write(resource), StandardCharsets.UTF_8);

		assertTrue(json.startsWith("{\"nodes\""));
		assertTrue(json.contains("\"edges\""));
		assertTrue(json.contains("\"id\":\"flow-id\""));
		assertTrue(json.contains("\"name\":\"flow-id\""));
		assertTrue(json.contains("\"path\":\"orders/create\""));
		assertFalse(json.contains("\"content\""));
		assertFalse(json.contains("\"script\""));
	}

	@Test
	public void shouldReadNativeTinyflowJsonWithoutMetadataWrapper() {
		TinyflowResource resource = storage.read(validWorkflow().getBytes(StandardCharsets.UTF_8));

		assertEquals(validWorkflow(), resource.getContent());
	}

	@Test
	public void shouldReadMagicSaveTransportWithWorkflowMetadata() {
		TinyflowResource resource = storage.read(transfer("flow-id", "flow-id", "tinyflow:0", "flow-id", validWorkflow())
				.getBytes(StandardCharsets.UTF_8));

		assertEquals("flow-id", resource.getId());
		assertEquals("flow-id", resource.getName());
		assertEquals("tinyflow:0", resource.getGroupId());
		assertEquals("flow-id", resource.getPath());
		assertEquals(validWorkflow(), resource.getContent());
	}

	@Test
	public void shouldPersistAndRecoverRawWorkflowThroughDefaultMagicResourceService() throws Exception {
		MagicRequestContextHolder previousContextHolder = WebUtils.magicRequestContextHolder;
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
		try {
			File rootDirectory = Files.createTempDirectory("tinyflow-magic-resource").toFile();
			TinyflowWorkflowValidator validator = new TinyflowWorkflowValidator(ChainParser.builder().build());
			TinyflowMagicResourceStorage lifecycleStorage = new TinyflowMagicResourceStorage(validator);
			TinyflowResourceDynamicRegistry firstRegistry = registry(lifecycleStorage, validator);
			DefaultMagicResourceService firstService = service(rootDirectory, lifecycleStorage, firstRegistry);
			firstService.refresh();

			TinyflowResource created = lifecycleStorage.read(transfer("flow-id", "create-flow", "tinyflow:0", "orders/create", validWorkflow())
					.getBytes(StandardCharsets.UTF_8));
			assertTrue(firstService.saveFile(created));
			String storedJson = new String(firstService.getResource().getResource("tinyflow/create-flow.json").read(), StandardCharsets.UTF_8);
			assertTrue(storedJson.startsWith("{\"nodes\""));
			assertTrue(storedJson.contains("\"name\":\"create-flow\""));
			assertTrue(storedJson.contains("\"path\":\"orders/create\""));
			assertFalse(storedJson.contains("\"content\""));
			assertFalse(storedJson.contains("\"script\""));
			assertEquals("flow-id", firstService.<TinyflowResource>file("flow-id").getId());

			TinyflowMagicResourceStorage restartedStorage = new TinyflowMagicResourceStorage(validator);
			TinyflowResourceDynamicRegistry restartedRegistry = registry(restartedStorage, validator);
			DefaultMagicResourceService restartedService = service(rootDirectory, restartedStorage, restartedRegistry);
			restartedService.refresh();

			TinyflowResource recovered = restartedService.file("flow-id");
			assertEquals("flow-id", recovered.getId());
			assertEquals("create-flow", recovered.getName());
			assertEquals("tinyflow:0", recovered.getGroupId());
			assertEquals("orders/create", recovered.getPath());
			assertEquals(validWorkflow(), recovered.getContent());

			TinyflowResource renamed = restartedStorage.read(transfer("flow-id", "create-flow-v2", "tinyflow:0", "orders/create-v2", validWorkflow())
					.getBytes(StandardCharsets.UTF_8));
			assertTrue(restartedService.saveFile(renamed));
			assertFalse(new File(rootDirectory, "tinyflow/create-flow.json").exists());
			assertTrue(new File(rootDirectory, "tinyflow/create-flow-v2.json").exists());

			TinyflowMagicResourceStorage renamedStorage = new TinyflowMagicResourceStorage(validator);
			TinyflowResourceDynamicRegistry renamedRegistry = registry(renamedStorage, validator);
			DefaultMagicResourceService renamedService = service(rootDirectory, renamedStorage, renamedRegistry);
			renamedService.refresh();

			TinyflowResource renamedRecovered = renamedService.file("flow-id");
			assertEquals("flow-id", renamedRecovered.getId());
			assertEquals("create-flow-v2", renamedRecovered.getName());
			assertEquals("tinyflow:0", renamedRecovered.getGroupId());
			assertEquals("orders/create-v2", renamedRecovered.getPath());
			assertEquals(validWorkflow(), renamedRecovered.getContent());
		} finally {
			WebUtils.magicRequestContextHolder = previousContextHolder;
		}
	}

	@Test
	public void shouldRejectMalformedJsonBeforeResourceCanBePersisted() {
		TinyflowResource resource = resource("flow-id", "orders/create", "{");

		TinyflowException error = assertThrows(TinyflowException.class, () -> storage.validate(resource));

		assertTrue(error.getMessage().contains("flow-id"));
	}

	@Test
	public void shouldRejectUnknownNodesAndDisconnectedWorkflowsBeforeResourceCanBePersisted() {
		TinyflowResource unknown = resource("unknown-flow", "orders/unknown",
				"{\"nodes\":[{\"id\":\"n1\",\"type\":\"missing-node\"}],\"edges\":[]}");
		TinyflowResource disconnected = resource("disconnected-flow", "orders/disconnected",
				"{\"nodes\":[{\"id\":\"start\",\"type\":\"startNode\"},"
						+ "{\"id\":\"end\",\"type\":\"endNode\"}],"
						+ "\"edges\":[{\"id\":\"e1\",\"source\":\"end\",\"target\":\"start\"}]}");

		assertThrows(TinyflowException.class, () -> storage.validate(unknown));
		assertThrows(TinyflowException.class, () -> storage.validate(disconnected));
	}

	private TinyflowResource resource(String id, String path, String json) {
		TinyflowResource resource = new TinyflowResource();
		resource.setId(id);
		resource.setName(id);
		resource.setPath(path);
		resource.setContent(json);
		return resource;
	}

	private String validWorkflow() {
		return "{\"nodes\":[{\"id\":\"start\",\"type\":\"startNode\"},"
				+ "{\"id\":\"end\",\"type\":\"endNode\"}],"
				+ "\"edges\":[{\"id\":\"e1\",\"source\":\"start\",\"target\":\"end\"}]}";
	}

	private String transfer(String id, String name, String groupId, String path, String content) {
		return "{\"id\":\"" + id + "\",\"name\":\"" + name + "\",\"groupId\":\"" + groupId
				+ "\",\"path\":\"" + path + "\",\"content\":" + quote(content) + "}";
	}

	private String quote(String value) {
		return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}

	private TinyflowResourceDynamicRegistry registry(TinyflowMagicResourceStorage lifecycleStorage,
			TinyflowWorkflowValidator validator) {
		InMemoryTinyflowWorkflowRepository workflows = new InMemoryTinyflowWorkflowRepository();
		InMemoryChainDefinitionRepository definitions = new InMemoryChainDefinitionRepository();
		TinyflowService tinyflowService = new DefaultTinyflowService(validator, workflows, definitions,
				new ChainExecutor(definitions, new InMemoryTinyflowChainStateRepository(), new InMemoryNodeStateRepository()),
				Duration.ofSeconds(2));
		return new TinyflowResourceDynamicRegistry(lifecycleStorage, tinyflowService);
	}

	private DefaultMagicResourceService service(File rootDirectory, TinyflowMagicResourceStorage lifecycleStorage,
			TinyflowResourceDynamicRegistry registry) {
		ApplicationEventPublisher publisher = new ApplicationEventPublisher() {
			@Override
			public void publishEvent(ApplicationEvent event) {
				publishEvent((Object) event);
			}

			@Override
			public void publishEvent(Object event) {
				if (event instanceof FileEvent) {
					registry.onFileEvent((FileEvent) event);
				}
			}
		};
		return new DefaultMagicResourceService(new FileResource(rootDirectory, false, rootDirectory.getAbsolutePath()),
				Collections.<MagicResourceStorage<? extends org.ssssssss.magicapi.core.model.MagicEntity>>singletonList(lifecycleStorage),
				publisher);
	}

}
