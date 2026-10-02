package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.AgenticToolProviders;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.model.Langchain4jAgenticScriptInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.model.MagicScriptAgent;
import dev.langchain4j.service.tool.ToolProvider;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.utils.PathUtils;

import java.util.Arrays;

/**
 * agentic 脚本模块：脚本用 {@code langchain4jAgenticScript.node('/path')} 取脚本节点
 * （挂入 {@code AgenticWorkflows} 编排 subAgents），或用
 * {@code langchain4jAgenticScript.toolProvider('/path', '工具名', '描述')} 包装为工具挂入助手。
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

	/** 把脚本节点包装为 ToolProvider（agent-as-tool），挂入助手 toolProvider。 */
	public ToolProvider toolProvider(String path, String toolName, String toolDescription) {
		MagicScriptAgent node = node(path);
		return AgenticToolProviders.agenticToolProvider(node,
				toolName == null || toolName.trim().isEmpty() ? path : toolName,
				toolDescription == null || toolDescription.trim().isEmpty()
						? node.getDescription() : toolDescription,
				node.getOutputKey(),
				node.getInputKeys() == null ? new String[0] : node.getInputKeys().toArray(new String[0]));
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
