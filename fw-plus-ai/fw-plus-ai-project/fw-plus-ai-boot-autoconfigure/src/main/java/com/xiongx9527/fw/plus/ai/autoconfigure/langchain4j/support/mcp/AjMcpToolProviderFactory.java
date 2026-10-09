package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.mcp;

import com.ajaxjs.mcp.client.IMcpClient;
import com.ajaxjs.mcp.client.McpToolProvider;

import java.util.Collections;
import java.util.List;

/**
 * aj-mcp {@link McpToolProvider} 的便捷构造工厂。
 *
 * <p>aj-mcp 的 {@code McpToolProvider} 负责从一个或多个已 {@link IMcpClient#initialize()}
 * 的客户端拉取工具列表，并把每个工具与它的 client 绑定成可执行函数；本工厂只做装配，
 * 不接管生命周期（client 的开关由调用方负责，如 try-with-resources）。</p>
 *
 * <p>用法：
 * <pre>McpToolProvider provider = AjMcpToolProviderFactory.fromClient(client);
 * McpToolProviderResult result = provider.provideTools();</pre></p>
 */
public final class AjMcpToolProviderFactory {

	private AjMcpToolProviderFactory() {
	}

	/**
	 * 单客户端装配，单 server 失败时忽略并继续（{@code failIfOneServerFails=false}）。
	 *
	 * @param client 已初始化的 MCP 客户端
	 * @return 工具提供者
	 */
	public static McpToolProvider fromClient(IMcpClient client) {
		return fromClients(Collections.singletonList(client), false);
	}

	/**
	 * 多客户端装配，任一 server 失败时忽略并继续。
	 *
	 * @param clients 已初始化的 MCP 客户端列表
	 * @return 工具提供者
	 */
	public static McpToolProvider fromClients(List<IMcpClient> clients) {
		return fromClients(clients, false);
	}

	/**
	 * 多客户端装配，可指定失败策略。
	 *
	 * @param clients              已初始化的 MCP 客户端列表，{@code null} 视为空列表
	 * @param failIfOneServerFails 为 {@code true} 时任一 server 拉取工具失败即抛异常；
	 *                             为 {@code false}（默认）时记日志并跳过该 server
	 * @return 工具提供者
	 */
	public static McpToolProvider fromClients(List<IMcpClient> clients, boolean failIfOneServerFails) {
		McpToolProvider provider = new McpToolProvider();
		provider.setMcpClients(clients == null ? Collections.<IMcpClient>emptyList() : clients);
		provider.setFailIfOneServerFails(failIfOneServerFails);
		return provider;
	}
}
