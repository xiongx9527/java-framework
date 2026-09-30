package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.ajmcpclient;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.AbstractLangchain4jPluginConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.ajmcpclient.model.MagicDynamicLangchain4jAjMcpClientDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.ajmcpclient.module.Langchain4jAjMcpClientModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.ajmcpclient.registry.Langchain4jAjMcpClientMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.ajmcpclient.resource.Langchain4jAjMcpClientMagicResourceStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@code langchain4j-aj-mcp-client} 数据源的装配：实例持有者、资源存储、动态注册器与脚本模块
 * {@code langchain4jAjMcpClient['key']}。
 *
 * <p>基于 aj-mcp（com.ajaxjs:aj-mcp-client，Java 8 兼容、GPL v3）连接外部 MCP Server，
 * 把其工具暴露成 LangChain4j {@code ToolProvider}，与
 * {@code langchain4jToolProvider['key']}（本地 api/function）互补，一起挂到
 * {@code AssistantBuilder.toolProvider(...)}。支持 http-sse / streamable-http / stdio 三种传输。</p>
 */
@ConditionalOnClass(name = "com.ajaxjs.mcp.client.McpClient")
@ConditionalOnProperty(prefix = "fw.plus.magicapi.langchain4j.aj-mcp-client", name = "enabled",
		havingValue = "true", matchIfMissing = true)
@Configuration(proxyBeanMethods = false)
public class Langchain4jAjMcpClientConfiguration extends AbstractLangchain4jPluginConfiguration {

	private static final String JAVASCRIPT_FILENAME = "magic-langchain4j-aj-mcp-client.1.0.0.iife.js";

	public Langchain4jAjMcpClientConfiguration() {
		super("langchain4j-aj-mcp-client", "MagicLangchain4jAjMcpClient", JAVASCRIPT_FILENAME);
	}

	/** 数据源实例持有者：按 key 持有多个 aj-mcp 客户端实例。 */
	@Bean
	@ConditionalOnMissingBean
	public MagicDynamicLangchain4jAjMcpClientDataSource langchain4jAjMcpClientDataSource() {
		return new MagicDynamicLangchain4jAjMcpClientDataSource();
	}

	/** langchain4j-aj-mcp-client 资源类型存储。 */
	@Bean
	@ConditionalOnMissingBean
	public Langchain4jAjMcpClientMagicResourceStorage langchain4jAjMcpClientMagicResourceStorage() {
		return new Langchain4jAjMcpClientMagicResourceStorage();
	}

	/** langchain4j-aj-mcp-client 动态注册器：资源变更即新建/重建/关闭 aj-mcp 客户端。 */
	@Bean
	@ConditionalOnMissingBean
	public Langchain4jAjMcpClientMagicDynamicRegistry langchain4jAjMcpClientMagicDynamicRegistry(
			Langchain4jAjMcpClientMagicResourceStorage storage,
			MagicDynamicLangchain4jAjMcpClientDataSource dataSource) {
		return new Langchain4jAjMcpClientMagicDynamicRegistry(storage, dataSource);
	}

	/** 脚本模块：langchain4jAjMcpClient['key'] 按 key 取 ToolProvider。 */
	@Bean
	@ConditionalOnMissingBean
	public Langchain4jAjMcpClientModule langchain4jAjMcpClientModule(
			MagicDynamicLangchain4jAjMcpClientDataSource dataSource) {
		return new Langchain4jAjMcpClientModule(dataSource);
	}
}
