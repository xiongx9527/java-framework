package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.mcpclient;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.AbstractLangchain4jPluginConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.mcpclient.model.MagicDynamicLangchain4jMcpClientDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.mcpclient.module.Langchain4jMcpClientModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.mcpclient.registry.Langchain4jMcpClientMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.mcpclient.resource.Langchain4jMcpClientMagicResourceStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@code langchain4j-mcp-client} 数据源的装配：实例持有者、资源存储、动态注册器与脚本模块
 * {@code langchain4jMcpClient['key']}。
 *
 * <p>把外部 MCP Server（Streamable HTTP / stdio / docker）的 工具暴露成 LangChain4j
 * {@code ToolProvider}，与 {@code langchain4jToolProvider['key']}（本地 api/function）互补，
 * 一起挂到 {@code AssistantBuilder.toolProvider(...)}。</p>
 */
@ConditionalOnClass(name = "dev.langchain4j.mcp.McpToolProvider")
@ConditionalOnProperty(prefix = "fw.plus.magicapi.langchain4j.mcp-client", name = "enabled",
		havingValue = "true", matchIfMissing = true)
@Configuration(proxyBeanMethods = false)
public class Langchain4jMcpClientConfiguration extends AbstractLangchain4jPluginConfiguration {

	private static final String JAVASCRIPT_FILENAME = "magic-langchain4j-mcp-client.1.0.0.iife.js";

	public Langchain4jMcpClientConfiguration() {
		super("langchain4j-mcp-client", "MagicLangchain4jMcpClient", JAVASCRIPT_FILENAME, false);
	}

	/** 数据源实例持有者：按 key 持有多个 MCP 客户端实例。 */
	@Bean
	@ConditionalOnMissingBean
	public MagicDynamicLangchain4jMcpClientDataSource langchain4jMcpClientDataSource(
			org.springframework.beans.factory.ObjectProvider<com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.MagicToolInvoker> invokerProvider) {
		return new MagicDynamicLangchain4jMcpClientDataSource(
				invokerProvider.getIfAvailable(
						com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.ScriptManagerMagicToolInvoker::new));
	}

	/** langchain4j-mcp-client 资源类型存储。 */
	@Bean
	@ConditionalOnMissingBean
	public Langchain4jMcpClientMagicResourceStorage langchain4jMcpClientMagicResourceStorage() {
		return new Langchain4jMcpClientMagicResourceStorage();
	}

	/** langchain4j-mcp-client 动态注册器：资源变更即新建/重建/关闭 MCP 客户端。 */
	@Bean
	@ConditionalOnMissingBean
	public Langchain4jMcpClientMagicDynamicRegistry langchain4jMcpClientMagicDynamicRegistry(
			Langchain4jMcpClientMagicResourceStorage storage,
			MagicDynamicLangchain4jMcpClientDataSource dataSource) {
		return new Langchain4jMcpClientMagicDynamicRegistry(storage, dataSource);
	}

	/** 脚本模块：langchain4jMcpClient['key'] 按 key 取 ToolProvider。 */
	@Bean
	@ConditionalOnMissingBean
	public Langchain4jMcpClientModule langchain4jMcpClientModule(
			MagicDynamicLangchain4jMcpClientDataSource dataSource) {
		return new Langchain4jMcpClientModule(dataSource);
	}
}
