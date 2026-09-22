package com.xiongx9527.fw.plus.ai.mcp.server;

import com.xiongx9527.fw.plus.ai.mcp.spi.McpCompletionProvider;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpPromptInfo;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpPromptProvider;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpResourceInfo;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpResourceProvider;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpResourceTemplate;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpTool;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpToolProvider;
import com.xiongx9527.fw.plus.ai.mcp.transport.McpServerTransportProvider;
import com.xiongx9527.fw.plus.ai.mcp.transport.McpStreamableServerTransportProvider;

/**
 * MCP Server 构建器
 *
 * <p>提供流式 API 配置 MCP Server 实例：serverInfo、capabilities、
 * tools、resources、resourceTemplates、prompts、completions。
 *
 * <p>支持两种 transportProvider：
 * <ul>
 *   <li>{@link McpStreamableServerTransportProvider}：Streamable HTTP（POST/GET/DELETE）</li>
 *   <li>{@link McpServerTransportProvider}：Classic 模式（STDIO/Legacy SSE 单连接）</li>
 * </ul>
 *
 * @author xiongxiao
 * @date 2025-09-20
 */
public class McpServerBuilder {

	private final Object transportProvider; // McpServerTransportProvider or McpStreamableServerTransportProvider
	private final boolean async;

	private McpServerInfo serverInfo = new McpServerInfo("fw-plus-mcp-server", "0.1.1");
	private McpServerCapabilities serverCapabilities;
	private String instructions;

	private final McpToolRegistry toolRegistry = new McpToolRegistry();
	private final McpResourceRegistry resourceRegistry = new McpResourceRegistry();
	private final McpPromptRegistry promptRegistry = new McpPromptRegistry();
	private final McpCompletionRegistry completionRegistry = new McpCompletionRegistry();

	public McpServerBuilder(Object transportProvider) {
		this(transportProvider, false);
	}

	public McpServerBuilder(Object transportProvider, boolean async) {
		if (transportProvider == null) {
			throw new IllegalArgumentException("Transport provider must not be null");
		}
		if (!(transportProvider instanceof McpStreamableServerTransportProvider)
				&& !(transportProvider instanceof McpServerTransportProvider)) {
			throw new IllegalArgumentException(
					"Unsupported transport provider type: " + transportProvider.getClass().getName());
		}
		this.transportProvider = transportProvider;
		this.async = async;
	}

	/**
	 * 设置服务器信息
	 */
	public McpServerBuilder serverInfo(String name, String version) {
		this.serverInfo = new McpServerInfo(name, version);
		return this;
	}

	/**
	 * 设置服务器信息
	 */
	public McpServerBuilder serverInfo(McpServerInfo serverInfo) {
		this.serverInfo = serverInfo;
		return this;
	}

	/**
	 * 设置服务器指令（说明文本）
	 */
	public McpServerBuilder instructions(String instructions) {
		this.instructions = instructions;
		return this;
	}

	/**
	 * 设置服务器能力
	 */
	public McpServerBuilder capabilities(McpServerCapabilities capabilities) {
		this.serverCapabilities = capabilities;
		return this;
	}

	/**
	 * 注册工具
	 */
	public McpServerBuilder tool(McpTool tool, McpToolProvider provider) {
		this.toolRegistry.register(tool, provider);
		return this;
	}

	/**
	 * 注册资源
	 */
	public McpServerBuilder resource(McpResourceInfo resource, McpResourceProvider provider) {
		this.resourceRegistry.register(resource, provider);
		return this;
	}

	/**
	 * 注册资源模板
	 */
	public McpServerBuilder resourceTemplate(McpResourceTemplate template, McpResourceProvider provider) {
		this.resourceRegistry.registerTemplate(template, provider);
		return this;
	}

	/**
	 * 注册提示词模板
	 */
	public McpServerBuilder prompt(McpPromptInfo prompt, McpPromptProvider provider) {
		this.promptRegistry.register(prompt, provider);
		return this;
	}

	/**
	 * 注册补全提供者
	 */
	public McpServerBuilder completion(McpCompletionProvider provider) {
		this.completionRegistry.register(provider);
		return this;
	}

	/**
	 * 构建 MCP Server 实例
	 */
	public McpAsyncServerImpl build() {
		// 构建能力集（如果未提供则按注册表自动推断）
		if (serverCapabilities == null) {
			serverCapabilities = McpServerCapabilities.builder()
					.logging(true)
					.tools(!toolRegistry.isEmpty())
					.resources(!resourceRegistry.isEmpty())
					.prompts(!promptRegistry.isEmpty())
					.completions(!completionRegistry.isEmpty())
					.build();
		}
		if (transportProvider instanceof McpStreamableServerTransportProvider) {
			return new McpAsyncServerImpl(
					(McpStreamableServerTransportProvider) transportProvider,
					serverInfo,
					serverCapabilities,
					instructions,
					toolRegistry,
					resourceRegistry,
					promptRegistry,
					completionRegistry,
					async);
		}
		return new McpAsyncServerImpl(
				(McpServerTransportProvider) transportProvider,
				serverInfo,
				serverCapabilities,
				instructions,
				toolRegistry,
				resourceRegistry,
				promptRegistry,
				completionRegistry,
				async);
	}
}
