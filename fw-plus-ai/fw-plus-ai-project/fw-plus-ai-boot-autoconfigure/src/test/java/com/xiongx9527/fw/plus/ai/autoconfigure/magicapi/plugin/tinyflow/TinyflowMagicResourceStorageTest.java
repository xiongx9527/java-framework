package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.model.TinyflowResource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.service.TinyflowMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.tinyflow.TinyflowException;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowWorkflowValidator;
import dev.tinyflow.core.parser.ChainParser;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

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

		assertEquals(validWorkflow(), json);
		assertTrue(json.startsWith("{\"nodes\""));
		assertFalse(json.contains("\"content\""));
		assertFalse(json.contains("\"script\""));
	}

	@Test
	public void shouldReadNativeTinyflowJsonWithoutMetadataWrapper() {
		TinyflowResource resource = storage.read(validWorkflow().getBytes(StandardCharsets.UTF_8));

		assertEquals(validWorkflow(), resource.getContent());
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
}
