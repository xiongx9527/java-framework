package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.web;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.model.TinyflowResource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.service.TinyflowMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.tinyflow.node.TinyflowNodeDescriptor;
import com.xiongx9527.fw.plus.ai.tinyflow.node.TinyflowNodeProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.node.TinyflowNodeProviderRegistry;
import com.xiongx9527.fw.plus.ai.tinyflow.runtime.TinyflowAsyncResult;
import com.xiongx9527.fw.plus.ai.tinyflow.runtime.TinyflowService;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowModelDescriptor;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowModelProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStoreDescriptor;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStoreProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowExecutionResult;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.web.MagicController;
import org.ssssssss.magicapi.core.web.MagicExceptionHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MagicTinyflowController extends MagicController implements MagicExceptionHandler {

	private final TinyflowService tinyflowService;
	private final MagicResourceService magicResourceService;
	private final TinyflowMagicResourceStorage resourceStorage;
	private final TinyflowModelProvider modelProvider;
	private final TinyflowVectorStoreProvider vectorStoreProvider;
	private final TinyflowNodeProviderRegistry nodeProviderRegistry;

	public MagicTinyflowController(MagicConfiguration configuration) {
		this(configuration, null, null, null, null, null, null);
	}

	public MagicTinyflowController(MagicConfiguration configuration, TinyflowService tinyflowService,
			MagicResourceService magicResourceService, TinyflowMagicResourceStorage resourceStorage,
			TinyflowModelProvider modelProvider, TinyflowVectorStoreProvider vectorStoreProvider,
			TinyflowNodeProviderRegistry nodeProviderRegistry) {
		super(configuration);
		this.tinyflowService = tinyflowService;
		this.magicResourceService = magicResourceService;
		this.resourceStorage = resourceStorage;
		this.modelProvider = modelProvider;
		this.vectorStoreProvider = vectorStoreProvider;
		this.nodeProviderRegistry = nodeProviderRegistry;
	}

	@RequestMapping("/tinyflow/resource/get")
	@ResponseBody
	public JsonBean<TinyflowResource> get(@RequestParam String id) {
		try {
			return new JsonBean<TinyflowResource>(resource(id));
		} catch (RuntimeException error) {
			return failure("Tinyflow 资源不存在");
		}
	}

	@RequestMapping("/tinyflow/resource/save")
	@ResponseBody
	public JsonBean<Object> save(@RequestBody TinyflowResource resource) {
		try {
			validateResource(resource);
			if (!resources().saveFile(resource)) {
				return failure("Tinyflow 资源保存失败");
			}
			return new JsonBean<Object>("ok");
		} catch (RuntimeException error) {
			return failure("Tinyflow 资源保存失败");
		}
	}

	@RequestMapping("/tinyflow/resource/validate")
	@ResponseBody
	public JsonBean<Object> validate(@RequestBody TinyflowResource resource) {
		try {
			validateResource(resource);
			return new JsonBean<Object>("ok");
		} catch (RuntimeException error) {
			return failure("Tinyflow 资源校验失败");
		}
	}

	@RequestMapping("/tinyflow/run")
	@ResponseBody
	public JsonBean<TinyflowExecutionResult> run(@RequestBody TinyflowRunRequest request) {
		try {
			TinyflowResource resource = resource(request == null ? null : request.getResourceId());
			return new JsonBean<TinyflowExecutionResult>(service().execute(resource.getId(), variables(request)));
		} catch (RuntimeException error) {
			return failure("Tinyflow 执行失败");
		}
	}

	@RequestMapping("/tinyflow/run-async")
	@ResponseBody
	public JsonBean<TinyflowAsyncResult> runAsync(@RequestBody TinyflowRunRequest request) {
		try {
			TinyflowResource resource = resource(request == null ? null : request.getResourceId());
			return new JsonBean<TinyflowAsyncResult>(service().executeAsync(resource.getId(), variables(request)));
		} catch (RuntimeException error) {
			return failure("Tinyflow 异步执行失败");
		}
	}

	@RequestMapping("/tinyflow/resume")
	@ResponseBody
	public JsonBean<TinyflowExecutionResult> resume(@RequestBody TinyflowRunRequest request) {
		try {
			return new JsonBean<TinyflowExecutionResult>(service().resume(instanceId(request), variables(request)));
		} catch (RuntimeException error) {
			return failure("Tinyflow 恢复执行失败");
		}
	}

	@RequestMapping("/tinyflow/provider/llm")
	@ResponseBody
	public JsonBean<List<TinyflowModelDescriptor>> models() {
		return new JsonBean<List<TinyflowModelDescriptor>>(modelsProvider().listChatModels());
	}

	@RequestMapping("/tinyflow/provider/embedding")
	@ResponseBody
	public JsonBean<List<TinyflowModelDescriptor>> embeddingModels() {
		return new JsonBean<List<TinyflowModelDescriptor>>(modelsProvider().listEmbeddingModels());
	}

	@RequestMapping("/tinyflow/provider/vector")
	@ResponseBody
	public JsonBean<List<TinyflowVectorStoreDescriptor>> vectorStores() {
		return new JsonBean<List<TinyflowVectorStoreDescriptor>>(vectorsProvider().list());
	}

	@RequestMapping("/tinyflow/nodes")
	@ResponseBody
	public JsonBean<List<TinyflowNodeDescriptor>> nodes() {
		return new JsonBean<List<TinyflowNodeDescriptor>>(nodeDescriptors());
	}

	private TinyflowResource resource(String id) {
		if (isBlank(id)) {
			throw new IllegalArgumentException("resourceId must not be blank");
		}
		Object resource = resources().file(id.trim());
		if (!(resource instanceof TinyflowResource)) {
			throw new IllegalArgumentException("Tinyflow resource not found");
		}
		return (TinyflowResource) resource;
	}

	private void validateResource(TinyflowResource resource) {
		storage().validate(resource);
	}

	private Map<String, Object> variables(TinyflowRunRequest request) {
		return request == null || request.getVariables() == null ? Collections.<String, Object>emptyMap()
				: new LinkedHashMap<String, Object>(request.getVariables());
	}

	private String instanceId(TinyflowRunRequest request) {
		if (request == null || isBlank(request.getInstanceId())) {
			throw new IllegalArgumentException("instanceId must not be blank");
		}
		return request.getInstanceId().trim();
	}

	private List<TinyflowNodeDescriptor> nodeDescriptors() {
		TinyflowNodeProviderRegistry registry = nodeProviderRegistry == null ? SpringUtils.getBean(TinyflowNodeProviderRegistry.class)
				: nodeProviderRegistry;
		if (registry == null) {
			return Collections.emptyList();
		}
		List<TinyflowNodeDescriptor> descriptors = new ArrayList<TinyflowNodeDescriptor>();
		for (TinyflowNodeProvider provider : registry.getProviders().values()) {
			TinyflowNodeDescriptor descriptor = provider.getDescriptor();
			if (descriptor != null) {
				descriptors.add(new TinyflowNodeDescriptor(descriptor.getType(), descriptor.getName(), descriptor.getDescription(),
						sanitize(descriptor.getProperties())));
			}
		}
		return descriptors;
	}

	private Map<String, Object> sanitize(Map<String, Object> properties) {
		Map<String, Object> safe = new LinkedHashMap<String, Object>();
		if (properties == null) {
			return safe;
		}
		for (Map.Entry<String, Object> entry : properties.entrySet()) {
			String key = entry.getKey();
			String normalized = key == null ? "" : key.toLowerCase(Locale.ROOT);
			if (!normalized.contains("apikey") && !normalized.contains("secret") && !normalized.contains("header")
					&& !normalized.contains("bean")) {
				safe.put(key, entry.getValue());
			}
		}
		return safe;
	}

	private TinyflowService service() {
		return tinyflowService == null ? SpringUtils.getBean(TinyflowService.class) : tinyflowService;
	}

	private MagicResourceService resources() {
		return magicResourceService == null ? MagicConfiguration.getMagicResourceService() : magicResourceService;
	}

	private TinyflowMagicResourceStorage storage() {
		return resourceStorage == null ? SpringUtils.getBean(TinyflowMagicResourceStorage.class) : resourceStorage;
	}

	private TinyflowModelProvider modelsProvider() {
		return modelProvider == null ? SpringUtils.getBean(TinyflowModelProvider.class) : modelProvider;
	}

	private TinyflowVectorStoreProvider vectorsProvider() {
		return vectorStoreProvider == null ? SpringUtils.getBean(TinyflowVectorStoreProvider.class) : vectorStoreProvider;
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}

	private <T> JsonBean<T> failure(String message) {
		return new JsonBean<T>(-1, message, null);
	}

	public static class TinyflowRunRequest {
		private String resourceId;
		private String instanceId;
		private Map<String, Object> variables;

		public String getResourceId() { return resourceId; }
		public void setResourceId(String resourceId) { this.resourceId = resourceId; }
		public String getInstanceId() { return instanceId; }
		public void setInstanceId(String instanceId) { this.instanceId = instanceId; }
		public Map<String, Object> getVariables() { return variables; }
		public void setVariables(Map<String, Object> variables) { this.variables = variables; }
	}
}
