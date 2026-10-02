package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.AgenticToolProviders;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.AgenticWorkflows;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.model.Langchain4jAgenticScriptInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.model.MagicScriptAgent;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.model.MagicScriptGraphNodes;
import dev.langchain4j.service.tool.ToolProvider;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.state.AgentState;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.utils.PathUtils;
import org.ssssssss.magicapi.utils.ScriptManager;
import org.ssssssss.script.MagicScriptContext;

/**
 * agentic 脚本模块：脚本用 {@code langchain4jAgenticScript.node('/path')} 取脚本节点
 * （挂入 {@code AgenticWorkflows} 编排 subAgents，或经 {@code graphNode('/path')} 嵌入
 * langgraph4j 图），用 {@code langchain4jAgenticScript.toolProvider('/path', '工具名', '描述')}
 * 包装为工具挂入助手；graph 形态脚本构建并返回 StateGraph，用
 * {@code graph(path)} 编译执行或 {@code graphToolProvider(path,...)} 包装为工具。
 */
@MagicModule("langchain4jAgenticScript")
public class Langchain4jAgenticScriptModule {

	private final AbstractMagicDynamicRegistry<Langchain4jAgenticScriptInfo> registry;

	private final MagicResourceService magicResourceService;

	public Langchain4jAgenticScriptModule(AbstractMagicDynamicRegistry<Langchain4jAgenticScriptInfo> registry,
			MagicResourceService magicResourceService) {
		this.registry = registry;
		this.magicResourceService = magicResourceService;
	}

	/** 按资源路径取脚本节点：编排构建时挂入 subAgents，每次取用实时解析（资源热更新即时生效）。 */
	public MagicScriptAgent node(String path) {
		return new MagicScriptAgent(find(path));
	}

	/** 脚本节点 → langgraph4j 图节点动作：嵌入 {@code agentic.graph(stateFactory)} 构建的 StateGraph。 */
	public <T extends AgentState> AsyncNodeAction<T> graphNode(String path) {
		return MagicScriptGraphNodes.nodeAsync(node(path));
	}

	/**
	 * 取图脚本并编译：type=graph 的脚本需构建并返回 langgraph4j {@code StateGraph}
	 * （或直接返回 {@code CompiledGraph}）。求值上下文注入 {@code agentic}
	 * （AgenticWorkflows 门面）与 {@code langchain4jAgenticScript}（本模块）。
	 */
	public <T extends AgentState> CompiledGraph<T> graph(String path) {
		return compileGraph(find(path));
	}

	/** 把图脚本包装为 ToolProvider（agent-as-tool）：工具名/描述缺省取资源定义。 */
	public ToolProvider graphToolProvider(String path, String toolName, String toolDescription) {
		Langchain4jAgenticScriptInfo info = find(path);
		CompiledGraph<? extends AgentState> compiled = compileGraph(info);
		return AgenticToolProviders.graphToolProvider(compiled,
				blank(toolName) ? defaultName(info) : toolName,
				blank(toolDescription) ? info.getDescription() : toolDescription);
	}

	/** 简化形态：工具名/描述直接取资源定义（name/description）。 */
	public ToolProvider graphToolProvider(String path) {
		return graphToolProvider(path, null, null);
	}

	/** 编译图脚本：脚本返回 StateGraph 时 compile，返回 CompiledGraph 时直接复用。 */
	@SuppressWarnings("unchecked")
	public <T extends AgentState> CompiledGraph<T> compileGraph(Langchain4jAgenticScriptInfo info) {
		MagicScriptContext context = new MagicScriptContext();
		context.setScriptName(defaultName(info));
		context.set("langchain4jAgenticScript", this);
		context.set("agentic", AgenticWorkflows.instance());
		Object result = ScriptManager.executeScript(info.getScript(), context);
		if (result instanceof CompiledGraph) {
			return (CompiledGraph<T>) result;
		}
		if (result instanceof org.bsc.langgraph4j.StateGraph) {
			try {
				return (CompiledGraph<T>) ((org.bsc.langgraph4j.StateGraph<T>) result)
						.compile(CompileConfig.builder().build());
			} catch (org.bsc.langgraph4j.GraphStateException e) {
				throw new IllegalArgumentException(
						String.format("agentic 图脚本 %s 编译失败: %s", info.getPath(), e.getMessage()), e);
			}
		}
		throw new IllegalArgumentException(String.format(
				"agentic 图脚本 %s 需返回 StateGraph 或 CompiledGraph，实际: %s",
				info.getPath(), result == null ? "null" : result.getClass().getName()));
	}

	/** 把脚本节点包装为 ToolProvider（agent-as-tool），挂入助手 toolProvider。 */
	public ToolProvider toolProvider(String path, String toolName, String toolDescription) {
		MagicScriptAgent node = node(path);
		return AgenticToolProviders.agenticToolProvider(node,
				toolName == null || toolName.trim().isEmpty() ? node.getName() : toolName,
				toolDescription == null || toolDescription.trim().isEmpty()
						? node.getDescription() : toolDescription,
				node.getOutputKey(),
				node.getInputKeys() == null ? new String[0] : node.getInputKeys().toArray(new String[0]));
	}

	/** 简化形态：工具名/描述直接取资源定义（name/description），无需调用方传参。 */
	public ToolProvider toolProvider(String path) {
		return toolProvider(path, null, null);
	}

	private static boolean blank(String value) {
		return value == null || value.trim().isEmpty();
	}

	private static String defaultName(Langchain4jAgenticScriptInfo info) {
		return blank(info.getName()) ? info.getPath() : info.getName();
	}

	private Langchain4jAgenticScriptInfo find(String path) {
		if (path == null || path.trim().isEmpty()) {
			throw new IllegalArgumentException("agentic 脚本路径不能为空");
		}
		String normalized = PathUtils.replaceSlash(path.startsWith("/") ? path : "/" + path);
		Langchain4jAgenticScriptInfo info = registry.getMapping(normalized);
		if (info == null) {
			info = magicResourceService.file(normalized);
		}
		if (info == null) {
			throw new IllegalArgumentException(String.format(
					"未找到 agentic 脚本资源: %s", normalized));
		}
		return info;
	}
}
