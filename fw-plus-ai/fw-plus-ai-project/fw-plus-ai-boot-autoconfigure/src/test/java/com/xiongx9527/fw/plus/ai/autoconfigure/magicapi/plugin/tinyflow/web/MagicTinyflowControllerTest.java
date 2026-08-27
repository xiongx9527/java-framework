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
import org.ssssssss.magicapi.core.config.Constants;
import org.ssssssss.magicapi.core.context.MagicUser;
import org.ssssssss.magicapi.core.interceptor.Authorization;
import org.ssssssss.magicapi.core.interceptor.AuthorizationInterceptor;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.model.Group;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MagicTinyflowControllerTest {

	@Test
	void shouldOnlyExposeChatModelDescriptor() throws Exception {
		MagicTinyflowController controller = controller(new RecordingService(), resourceService(null), modelProvider(), vectorProvider(), allowAll());

		JsonBean<List<TinyflowModelDescriptor>> response = controller.models(request(new MagicUser("u", "u", "t")));

		@SuppressWarnings("unchecked")
		List<TinyflowModelDescriptor> models = (List<TinyflowModelDescriptor>) response.getData();
		assertEquals(Collections.singletonList("chat-main"), keys(models));
		assertFalse(new ObjectMapper().writeValueAsString(response.getData()).contains("apiKey"));
	}

	@Test
	void shouldRunOnlySavedResourceIdAndKeepFailureSanitized() {
		RecordingService service = new RecordingService();
		MagicTinyflowController controller = controller(service, resourceService(resource("saved-flow")), modelProvider(), vectorProvider(), allowAll());
		MagicTinyflowController.TinyflowRunRequest runRequest = new MagicTinyflowController.TinyflowRunRequest();
		runRequest.setResourceId("saved-flow");
		runRequest.setVariables(Collections.<String, Object>singletonMap("input", "ok"));

		JsonBean<TinyflowExecutionResult> response = controller.run(runRequest, request(new MagicUser("u", "u", "t")));

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
		MagicTinyflowController controller = new MagicTinyflowController(configuration(allowAll()), rejectingService,
				resourceService(null), storage, modelProvider(), vectorProvider(), null);
		TinyflowResource created = resource(null);
		created.setContent("{\"nodes\":[{\"id\":\"start\",\"type\":\"startNode\"},{\"id\":\"end\",\"type\":\"endNode\"}],\"edges\":[{\"id\":\"e1\",\"source\":\"start\",\"target\":\"end\"}]}");

		JsonBean<Object> response = controller.save(created, request(new MagicUser("u", "u", "t")));

		assertEquals(1, response.getCode());
		assertEquals("ok", response.getData());
	}

	@Test
	void shouldRejectAnonymousEditorRequestsThroughAuthorizationInterceptor() {
		MagicTinyflowController controller = controller(new RecordingService(), resourceService(resource("saved-flow")),
				modelProvider(), vectorProvider(), allowAuthenticated());

		assertThrows(RuntimeException.class, () -> controller.models(request(null)));
		assertThrows(RuntimeException.class, () -> controller.get("saved-flow", request(null)));
	}

	@Test
	void shouldRejectReadOnlyUserFromSaveAndExecution() {
		RecordingService service = new RecordingService();
		MagicTinyflowController controller = controller(service, resourceService(resource("saved-flow")), modelProvider(),
				vectorProvider(), viewOnly());
		MagicTinyflowController.TinyflowRunRequest run = new MagicTinyflowController.TinyflowRunRequest();
		run.setResourceId("saved-flow");

		assertThrows(RuntimeException.class, () -> controller.save(resource("saved-flow"), request(new MagicUser("view", "view", "t"))));
		assertThrows(RuntimeException.class, () -> controller.run(run, request(new MagicUser("view", "view", "t"))));
		assertEquals(null, service.executedWorkflowId);
	}

	@Test
	void shouldAllowAuthorizedUserAndApplyEntityPermissions() {
		RecordingAuthorization authorization = allowAll();
		RecordingService service = new RecordingService();
		MagicTinyflowController controller = controller(service, resourceService(resource("saved-flow")), modelProvider(),
				vectorProvider(), authorization);
		MagicHttpServletRequest request = request(new MagicUser("editor", "editor", "t"));
		MagicTinyflowController.TinyflowRunRequest run = new MagicTinyflowController.TinyflowRunRequest();
		run.setResourceId("saved-flow");

		assertEquals(1, controller.get("saved-flow", request).getCode());
		assertEquals(1, controller.run(run, request).getCode());
		assertTrue(authorization.entityChecks.contains(Authorization.VIEW));
		assertTrue(authorization.entityChecks.contains(Authorization.SAVE));
	}

	@Test
	void shouldRecursivelyRemoveSensitiveNodePropertiesWithoutMutatingProviderDescriptor() throws Exception {
		Map<String, Object> nested = new LinkedHashMap<String, Object>();
		nested.put("Authorization", "bearer-secret");
		nested.put("safe", "value");
		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		properties.put("apiKey", "api-secret");
		properties.put("nested", nested);
		properties.put("items", java.util.Arrays.<Object>asList(nested, "safe"));
		com.xiongx9527.fw.plus.ai.tinyflow.node.TinyflowNodeProvider provider = nodeProvider(properties);
		com.xiongx9527.fw.plus.ai.tinyflow.node.TinyflowNodeProviderRegistry nodes =
				new com.xiongx9527.fw.plus.ai.tinyflow.node.TinyflowNodeProviderRegistry(Collections.singletonList(provider));
		MagicTinyflowController controller = new MagicTinyflowController(configuration(allowAll()), new RecordingService(),
				resourceService(null), null, modelProvider(), vectorProvider(), nodes);

		MagicHttpServletRequest httpRequest = request(new MagicUser("u", "u", "t"));
		Object data = controller.nodes(httpRequest).getData();
		String json = new ObjectMapper().writeValueAsString(data);

		assertFalse(json.toLowerCase().contains("apikey"));
		assertFalse(json.toLowerCase().contains("authorization"));
		assertEquals("api-secret", properties.get("apiKey"));
		assertEquals("bearer-secret", nested.get("Authorization"));
	}

	private List<String> keys(List<TinyflowModelDescriptor> descriptors) {
		return Collections.singletonList(descriptors.get(0).getKey());
	}

	private MagicTinyflowController controller(TinyflowService service, MagicResourceService resources,
			TinyflowModelProvider models, TinyflowVectorStoreProvider vectors, AuthorizationInterceptor authorization) {
		return new MagicTinyflowController(configuration(authorization), service, resources, null, models, vectors, null);
	}

	private MagicConfiguration configuration(AuthorizationInterceptor authorization) {
		MagicConfiguration configuration = new MagicConfiguration();
		configuration.setAuthorizationInterceptor(authorization);
		return configuration;
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

	private MagicHttpServletRequest request(MagicUser user) {
		final Map<String, Object> attributes = new LinkedHashMap<String, Object>();
		if (user != null) attributes.put(Constants.ATTRIBUTE_MAGIC_USER, user);
		return (MagicHttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(),
				new Class<?>[] { MagicHttpServletRequest.class }, (proxy, method, args) -> {
					if ("getAttribute".equals(method.getName())) return attributes.get(args[0]);
					if ("setAttribute".equals(method.getName())) { attributes.put((String) args[0], args[1]); return null; }
					return null;
				});
	}

	private RecordingAuthorization allowAll() { return new RecordingAuthorization(true, true); }
	private RecordingAuthorization allowAuthenticated() { return new RecordingAuthorization(true, true); }
	private RecordingAuthorization viewOnly() { return new RecordingAuthorization(false, false); }

	private com.xiongx9527.fw.plus.ai.tinyflow.node.TinyflowNodeProvider nodeProvider(final Map<String, Object> properties) {
		return new com.xiongx9527.fw.plus.ai.tinyflow.node.TinyflowNodeProvider() {
			@Override public String getType() { return "custom"; }
			@Override public dev.tinyflow.core.parser.NodeParser<?> getParser() {
				return new dev.tinyflow.core.parser.NodeParser<dev.tinyflow.core.node.EndNode>() {
					@Override public dev.tinyflow.core.parser.ChainParser getChainParser() { return null; }
					@Override public dev.tinyflow.core.node.EndNode parse(com.alibaba.fastjson.JSONObject node,
							com.alibaba.fastjson.JSONObject chain, dev.tinyflow.core.parser.ChainParser parser) {
						dev.tinyflow.core.node.EndNode result = new dev.tinyflow.core.node.EndNode();
						result.setId(node.getString("id"));
						return result;
					}
				};
			}
			@Override public com.xiongx9527.fw.plus.ai.tinyflow.node.TinyflowNodeDescriptor getDescriptor() {
				return new com.xiongx9527.fw.plus.ai.tinyflow.node.TinyflowNodeDescriptor("custom", "Custom", "Custom node", properties);
			}
			@Override public int getOrder() { return 0; }
		};
	}

	private static final class RecordingAuthorization implements AuthorizationInterceptor {
		private final boolean saveAllowed;
		private final boolean requireUser;
		private final List<Authorization> entityChecks = new java.util.ArrayList<Authorization>();
		private RecordingAuthorization(boolean saveAllowed, boolean requireUser) { this.saveAllowed = saveAllowed; this.requireUser = requireUser; }
		@Override public boolean requireLogin() { return true; }
		@Override public MagicUser getUserByToken(String token) { return null; }
		@Override public MagicUser login(String username, String password) { return null; }
		@Override public void logout(String token) { }
		@Override public boolean allowVisit(MagicUser user, MagicHttpServletRequest request, Authorization authorization) {
			return (!requireUser || user != null) && (authorization != Authorization.SAVE || saveAllowed);
		}
		@Override public boolean allowVisit(MagicUser user, MagicHttpServletRequest request, Authorization authorization, MagicEntity entity) {
			entityChecks.add(authorization);
			return allowVisit(user, request, authorization);
		}
		@Override public boolean allowVisit(MagicUser user, MagicHttpServletRequest request, Authorization authorization, Group group) {
			return allowVisit(user, request, authorization);
		}
		@Override public void refreshToken(MagicUser user) { }
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
