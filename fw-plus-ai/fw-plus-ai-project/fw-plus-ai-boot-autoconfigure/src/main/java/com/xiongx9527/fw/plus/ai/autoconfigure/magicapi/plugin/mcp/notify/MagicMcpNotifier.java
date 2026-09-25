package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.notify;

import com.ajaxjs.mcp.server.McpServer;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.runtime.McpServerRuntime;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.model.MagicDynamicMcpServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * MCP 能力变更通知：把 list_changed 与 resources/updated 广播到全部 server 实例。
 *
 * <p>覆盖 starter 的 runtime（{@code fw-plus.ai.mcp.servers[]}）与插件通过
 * {@code mcp-server} 资源动态创建的实例。transport 尚未绑定的实例（理论上只出现在
 * 装配中途）会被跳过，避免内核 NPE。</p>
 */
public class MagicMcpNotifier {

	private final List<McpServerRuntime> runtimes;

	private final MagicDynamicMcpServer dynamicServer;

	public MagicMcpNotifier(List<McpServerRuntime> runtimes, MagicDynamicMcpServer dynamicServer) {
		this.runtimes = runtimes == null ? Collections.emptyList() : new ArrayList<>(runtimes);
		this.dynamicServer = dynamicServer;
	}

	/**
	 * 工具列表变更（api/function 新增、修改、删除）。
	 */
	public void publishToolsChanged() {
		for (McpServer server : servers()) {
			server.publishToolsChanged();
		}
	}

	/**
	 * 资源列表变更（mcp-resource 新增或删除）。
	 */
	public void publishResourcesChanged() {
		for (McpServer server : servers()) {
			server.publishResourcesChanged();
		}
	}

	/**
	 * 提示词列表变更（mcp-prompt 新增、修改或删除）。
	 */
	public void publishPromptsChanged() {
		for (McpServer server : servers()) {
			server.publishPromptsChanged();
		}
	}

	/**
	 * 单个资源内容更新：仅通知订阅了该 uri 的客户端。
	 *
	 * @param uri 资源 uri
	 */
	public void publishResourceUpdated(String uri) {
		if (uri == null || uri.trim().isEmpty()) {
			return;
		}
		for (McpServer server : servers()) {
			server.publishResourceUpdated(uri);
		}
	}

	/**
	 * 刷新工具列表：供控制台手工触发，等价于对全部实例重放一次工具 diff。
	 */
	public void refreshTools() {
		for (McpServerRuntime runtime : runtimes) {
			if (runtime.getServer() != null) {
				runtime.getServer().publishToolsChanged();
			}
		}
		if (dynamicServer != null) {
			dynamicServer.refreshTools();
		}
	}

	private List<McpServer> servers() {
		List<McpServer> servers = new ArrayList<>();
		for (McpServerRuntime runtime : runtimes) {
			if (runtime.getServer() != null && runtime.getServer().getTransport() != null) {
				servers.add(runtime.getServer());
			}
		}
		if (dynamicServer != null) {
			for (McpServerRuntime runtime : dynamicServer.serverNodes()) {
				if (runtime.getServer() != null && runtime.getServer().getTransport() != null) {
					servers.add(runtime.getServer());
				}
			}
		}
		return servers;
	}
}
