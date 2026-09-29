package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider;

import dev.langchain4j.agent.tool.ToolParameters;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Magic-API 的 LangChain4j {@link ToolProvider}：把 magic-api 的 api + function 资源作为
 * LLM 工具本地提供，不经过 MCP。
 *
 * <p>每次 {@link #provideTools} 都读取注册表快照：快照自带缓存，资源变更后由
 * {@code MagicApiToolChangeListener} 失效，因此工具的增删对后续轮次的 LLM 可见。</p>
 *
 * <p>用法：{@code AiServices.builder(Svc.class).toolProvider(magicApiToolProvider).build()}</p>
 */
public class MagicApiToolProvider implements ToolProvider {

	private final MagicApiToolRegistry registry;

	private final ToolExecutor toolExecutor;

	public MagicApiToolProvider(MagicApiToolRegistry registry, ToolExecutor toolExecutor) {
		this.registry = registry;
		this.toolExecutor = toolExecutor;
	}

	@Override
	public ToolProviderResult provideTools(ToolProviderRequest request) {
		// 按工具名去重（spec 还带 description/parameters，equals 不等于同名判断）；
		// 注册表保证 function 先于 api 登记，故同名时 function 胜出。
		Map<String, ToolSpecification> specificationsByName = new LinkedHashMap<>();
		for (MagicApiTool tool : registry.snapshot()) {
			specificationsByName.putIfAbsent(tool.getName(), ToolSpecification.builder()
					.name(tool.getName())
					.description(tool.getDescription())
					.parameters(toToolParameters(tool.getInputSchema()))
					.build());
		}
		Map<ToolSpecification, ToolExecutor> tools = new LinkedHashMap<>();
		for (ToolSpecification specification : specificationsByName.values()) {
			tools.put(specification, toolExecutor);
		}
		return ToolProviderResult.builder().addAll(tools).build();
	}

	/**
	 * 入参 schema（JSON Schema 子集）转 langchain4j 工具参数；无 properties 时给空参数表。
	 */
	private ToolParameters toToolParameters(Map<String, Object> inputSchema) {
		if (inputSchema == null || !(inputSchema.get("properties") instanceof Map)) {
			return ToolParameters.builder().build();
		}
		ToolParameters.Builder builder = ToolParameters.builder()
				.properties(unsafeProperties(inputSchema.get("properties")));
		Object required = inputSchema.get("required");
		if (required instanceof List) {
			builder.required(unsafeRequired((List<?>) required));
		}
		return builder.build();
	}

	@SuppressWarnings("unchecked")
	private Map<String, Map<String, Object>> unsafeProperties(Object properties) {
		return (Map<String, Map<String, Object>>) properties;
	}

	@SuppressWarnings("unchecked")
	private List<String> unsafeRequired(List<?> required) {
		return (List<String>) required;
	}
}
