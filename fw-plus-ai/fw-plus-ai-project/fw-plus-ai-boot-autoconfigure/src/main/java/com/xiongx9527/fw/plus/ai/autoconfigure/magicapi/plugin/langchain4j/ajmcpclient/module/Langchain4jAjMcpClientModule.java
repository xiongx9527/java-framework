package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.ajmcpclient.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.ajmcpclient.model.MagicDynamicLangchain4jAjMcpClientDataSource;
import dev.langchain4j.service.tool.ToolProvider;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

import java.util.TreeSet;

/**
 * aj-mcp 客户端脚本模块：脚本用 {@code langchain4jAjMcpClient['key']} 取控制台维护的
 * {@link ToolProvider}（外部 MCP Server 的工具，aj-mcp Java 8 实现），再注入
 * {@code AssistantBuilder.toolProvider(...)}。
 */
@MagicModule("langchain4jAjMcpClient")
public class Langchain4jAjMcpClientModule
		implements DynamicAttribute<ToolProvider, Langchain4jAjMcpClientModule>, DynamicModule<ToolProvider> {

	private final MagicDynamicLangchain4jAjMcpClientDataSource dataSource;

	public Langchain4jAjMcpClientModule(MagicDynamicLangchain4jAjMcpClientDataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public ToolProvider getDynamicAttribute(String key) {
		ToolProvider provider = dataSource.get(key);
		if (provider == null) {
			throw new IllegalArgumentException(String.format(
					"未配置 LangChain4j aj-mcp 客户端: %s，已配置: %s", key, new TreeSet<String>(dataSource.keys())));
		}
		return provider;
	}

	/**
	 * 手动刷新指定实例：重连 MCP Server、重新拉取工具列表。
	 * 脚本用法：{@code langchain4jAjMcpClient.refresh('key')}。
	 */
	public ToolProvider refresh(String key) {
		return dataSource.refresh(key);
	}

	@Override
	public Langchain4jAjMcpClientModule setDynamicAttribute(String key, ToolProvider value) {
		throw new UnsupportedOperationException(
				"脚本不支持设置 aj-mcp 客户端，请在控制台维护 langchain4j-aj-mcp-client 数据源");
	}

	@Override
	public ToolProvider getDynamicModule(MagicScriptContext context) {
		String key = context == null ? null : context.getString(Options.DEFAULT_DATA_SOURCE.getValue());
		return getDynamicAttribute(key);
	}
}
