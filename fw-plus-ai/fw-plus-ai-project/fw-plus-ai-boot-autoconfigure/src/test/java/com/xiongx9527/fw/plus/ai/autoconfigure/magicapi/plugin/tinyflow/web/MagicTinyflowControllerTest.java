package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.model.TinyflowResource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.service.TinyflowMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.tinyflow.runtime.TinyflowAsyncResult;
import com.xiongx9527.fw.plus.ai.tinyflow.runtime.TinyflowService;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowEmbeddingModel;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowModelDescriptor;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowModelProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStore;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStoreDescriptor;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStoreProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowExecutionResult;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowWorkflow;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowWorkflowValidator;
import dev.tinyflow.core.parser.ChainParser;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MagicTinyflowControllerTest {

	@Test
	void shouldOnlyExposeChatModelDescriptor() throws Exception {
		MagicTinyflowController controller = controller(new RecordingService(), resourceService(null), modelProvider(), vectorProvider());

		JsonBean<List<TinyflowModelDescriptor>> response = controller.models();

		@SuppressWarnings("unchecked")
		List<TinyflowModelDescriptor> models = (List<TinyflowModelDescriptor>) response.getData();
		assertEquals(Collections.singletonList("chat-main"), keys(models));
		assertFalse(new ObjectMapper().writeValueAsString(response.getData()).contains("apiKey"));
	}

	@Test
	void shouldRunOnlySavedResourceIdAndKeepFailureSanitized() {
		RecordingService service = new RecordingService();
		MagicTinyflowController controller = controller(service, resourceService(resource("saved-flow")), modelProvider(), vectorProvider());
		MagicTinyflowController.TinyflowRunRequest request = new MagicTinyflowController.TinyflowRunRequest();
		request.setResourceId("saved-flow");
		request.setVariables(Collections.<String, Object>singletonMap("input", "ok"));

		JsonBean<TinyflowExecutionResult> response = controller.run(request);

		assertEquals(1, response.getCode());
		assertNotNull(response.getData());
		assertEquals("saved-flow", service.executedWorkflowId);
	}

	@Test
	void shouldAllowMagicResourceServiceToGenerateIdOnCreate() {
		TinyflowMagicResourceStorage storage = new TinyflowMagicResourceStorage(new TinyflowWorkflowValidator(ChainParser.builder().build()));
		TinyflowService rejectingService = new RecordingService() {
			@Override public void validate(TinyflowWorkflow workflow) {
				throw new AssertionError("save must not validate an id before Magic resource persistence");
			}
		};
		MagicTinyflowController controller = new MagicTinyflowController(new MagicConfiguration(), rejectingService,
				resourceService(null), storage, modelProvider(), vectorProvider(), null);
		TinyflowResource created = resource(null);
		created.setContent("{\"nodes\":[{\"id\":\"start\",\"type\":\"startNode\"},{\"id\":\"end\",\"type\":\"endNode\"}],\"edges\":[{\"id\":\"e1\",\"source\":\"start\",\"target\":\"end\"}]}");

		JsonBean<Object> response = controller.save(created);

		assertEquals(1, response.getCode());
		assertEquals("ok", response.getData());
	}

	private List<String> keys(List<TinyflowModelDescriptor> descriptors) {
		return Collections.singletonList(descriptors.get(0).getKey());
	}

	private MagicTinyflowController controller(TinyflowService service, MagicResourceService resources,
			TinyflowModelProvider models, TinyflowVectorStoreProvider vectors) {
		return new MagicTinyflowController(new MagicConfiguration(), service, resources, null, models, vectors, null);
	}

	private TinyflowResource resource(String id) {
		TinyflowResource resource = new TinyflowResource();
		resource.setId(id);
		resource.setPath("orders/create");
		resource.setContent("{}");
		return resource;
	}

	private MagicResourceService resourceService(final TinyflowResource resource) {
		return (MagicResourceService) Proxy.newProxyInstance(getClass().getClassLoader(),
				new Class<?>[] { MagicResourceService.class }, (proxy, method, args) -> {
					if ("file".equals(method.getName())) return resource;
					if ("saveFile".equals(method.getName())) return Boolean.TRUE;
					return null;
				});
	}

	private TinyflowModelProvider modelProvider() {
		return new TinyflowModelProvider() {
			@Override public List<TinyflowModelDescriptor> listChatModels() {
				return Collections.singletonList(new TinyflowModelDescriptor("chat-main", "Chat", "CHAT", "test"));
			}
			@Override public List<TinyflowModelDescriptor> listEmbeddingModels() { return Collections.emptyList(); }
			@Override public ChatLanguageModel getChatModel(String key) { return null; }
			@Override public TinyflowEmbeddingModel getEmbeddingModel(String key) { return null; }
		};
	}

	private TinyflowVectorStoreProvider vectorProvider() {
		return new TinyflowVectorStoreProvider() {
			@Override public List<TinyflowVectorStoreDescriptor> list() { return Collections.emptyList(); }
			@Override public TinyflowVectorStore get(String key) { return null; }
		};
	}

	private static class RecordingService implements TinyflowService {
		private String executedWorkflowId;
		@Override public void validate(TinyflowWorkflow workflow) { }
		@Override public void register(TinyflowWorkflow workflow) { }
		@Override public void remove(String workflowId) { }
		@Override public TinyflowExecutionResult execute(String workflowId, Map<String, Object> variables) {
			executedWorkflowId = workflowId;
			return new TinyflowExecutionResult("instance", Collections.<String, Object>emptyMap(), false);
		}
		@Override public TinyflowAsyncResult executeAsync(String workflowId, Map<String, Object> variables) { return new TinyflowAsyncResult("instance"); }
		@Override public TinyflowExecutionResult resume(String instanceId, Map<String, Object> variables) { return execute(instanceId, variables); }
		@Override public TinyflowAsyncResult resumeAsync(String instanceId, Map<String, Object> variables) { return new TinyflowAsyncResult(instanceId); }
	}
}
