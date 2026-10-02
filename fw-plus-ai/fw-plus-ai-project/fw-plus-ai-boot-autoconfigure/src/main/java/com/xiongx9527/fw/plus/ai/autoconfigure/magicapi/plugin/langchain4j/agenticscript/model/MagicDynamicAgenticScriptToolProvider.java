package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.module.Langchain4jAgenticScriptModule;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;

/**
 * agentic 脚本资源的 ToolProvider 动态代理：每次 {@link #provideTools} 实时按 path 解析
 * 脚本资源并重新包装（脚本内容/元数据热更新即时生效，不持有失效引用）。
 * 脚本形态（type=tool/graph）由模块 {@code toolProvider(path)} 内部按最新资源定义分发。
 */
public class MagicDynamicAgenticScriptToolProvider implements ToolProvider {

	private final Langchain4jAgenticScriptModule module;

	private final String path;

	public MagicDynamicAgenticScriptToolProvider(Langchain4jAgenticScriptModule module, String path) {
		this.module = module;
		this.path = path;
	}

	public String getPath() {
		return path;
	}

	@Override
	public ToolProviderResult provideTools(ToolProviderRequest request) {
		return module.toolProvider(path).provideTools(request);
	}
}
