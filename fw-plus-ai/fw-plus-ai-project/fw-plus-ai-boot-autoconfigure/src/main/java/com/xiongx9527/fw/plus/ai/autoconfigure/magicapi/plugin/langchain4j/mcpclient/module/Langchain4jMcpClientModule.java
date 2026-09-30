package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.mcpclient.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.mcpclient.model.MagicDynamicLangchain4jMcpClientDataSource;
import dev.langchain4j.service.tool.ToolProvider;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

import java.util.TreeSet;

/**
 * MCP 客户端脚本模块：脚本用 {@code langchain4jMcpClient['key']} 取控制台维护的
 * {@link ToolProvider}（外部 MCP Server 的工具），再注入
 * {@code AssistantBuilder.toolProvider(...)}。
 */
@MagicModule("langchain4jMcpClient")
public class Langchain4jMcpClientModule
		implements DynamicAttribute<ToolProvider, Langchain4jMcpClientModule>, DynamicModule<ToolProvider> {

	private final MagicDynamicLangchain4jMcpClientDataSource dataSource;

	public Langchain4jMcpClientModule(MagicDynamicLangchain4jMcpClientDataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public ToolProvider getDynamicAttribute(String key) {
		ToolProvider provider = dataSource.get(key);
		if (provider == null) {
			throw new IllegalArgumentException(String.format(
					"未配置 LangChain4j MCP 客户端: %s，已配置: %s", key, new TreeSet<>(dataSource.keys())));
		}
		return provider;
	}

	/**
	 * 手动刷新指定实例：重连 MCP Server、重新调 headers-function 取 api-key、重新拉工具列表。
	 * 脚本用法：{@code langchain4jMcpClient.refresh('key')}。
	 */
	public ToolProvider refresh(String key) {
		return dataSource.refresh(key);
	}

	/** 仅使指定实例的工具列表缓存失效（下次提供工具时重新 listTools，不重连）。 */
	public void evictToolListCache(String key) {
		dataSource.evictToolListCache(key);
	}

	@Override
	public Langchain4jMcpClientModule setDynamicAttribute(String key, ToolProvider value) {
		throw new UnsupportedOperationException(
				"脚本不支持设置 MCP 客户端，请在控制台维护 langchain4j-mcp-client 数据源");
	}

	@Override
	public ToolProvider getDynamicModule(MagicScriptContext context) {
		String key = context == null ? null : context.getString(Options.DEFAULT_DATA_SOURCE.getValue());
		return getDynamicAttribute(key);
	}
}
