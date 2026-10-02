package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.AgenticToolProviders;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.model.MagicDynamicLangchain4jAgenticScriptDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.model.MagicScriptAgent;
import dev.langchain4j.service.tool.ToolProvider;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

import java.util.TreeSet;

/**
 * agentic 脚本节点/工具模块：脚本用 {@code langchain4jAgenticScript['key']} 取脚本节点
 * （挂入 {@code AgenticWorkflows} 编排 subAgents），或用
 * {@code langchain4jAgenticScript.toolProvider('key', '工具名', '描述')} 把节点包装为工具。
 */
@MagicModule("langchain4jAgenticScript")
public class Langchain4jAgenticScriptModule
		implements DynamicAttribute<MagicScriptAgent, Langchain4jAgenticScriptModule>, DynamicModule<MagicScriptAgent> {

	private final MagicDynamicLangchain4jAgenticScriptDataSource dataSource;

	public Langchain4jAgenticScriptModule(MagicDynamicLangchain4jAgenticScriptDataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public MagicScriptAgent getDynamicAttribute(String key) {
		MagicScriptAgent agent = dataSource.get(key);
		if (agent == null) {
			throw new IllegalArgumentException(String.format(
					"未配置 LangChain4j Agentic Script: %s，已配置: %s", key, new TreeSet<>(dataSource.keys())));
		}
		return agent;
	}

	/** 把脚本节点（或任意 agentic 编排产物）包装为 ToolProvider，挂入助手 toolProvider。 */
	public ToolProvider toolProvider(MagicScriptAgent node, String toolName, String toolDescription) {
		return AgenticToolProviders.agenticToolProvider(node, toolName == null || toolName.trim().isEmpty()
				? node.getKey() : toolName, toolDescription, node.getOutputKey(),
				node.getInputKeys() == null ? new String[0] : node.getInputKeys().toArray(new String[0]));
	}

	@Override
	public Langchain4jAgenticScriptModule setDynamicAttribute(String key, MagicScriptAgent value) {
		throw new UnsupportedOperationException(
				"脚本不支持设置 Agentic Script，请在控制台维护 langchain4j-agentic-script 数据源");
	}

	@Override
	public MagicScriptAgent getDynamicModule(MagicScriptContext context) {
		String key = context == null ? null : context.getString(Options.DEFAULT_DATA_SOURCE.getValue());
		return getDynamicAttribute(key);
	}
}
