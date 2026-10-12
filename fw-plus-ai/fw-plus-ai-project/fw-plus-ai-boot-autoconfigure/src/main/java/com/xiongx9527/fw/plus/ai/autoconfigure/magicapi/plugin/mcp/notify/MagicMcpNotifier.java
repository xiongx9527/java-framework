package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.notify;

import com.ajaxjs.mcp.server.McpServer;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.runtime.McpServerRuntime;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.model.MagicDynamicMcpServer;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.service.MagicApiMcpCapabilityRegistrar;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.tools.MagicApiMcpToolRegistrar;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

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

	private final ObjectProvider<MagicApiMcpToolRegistrar> toolRegistrarProvider;

	private final ObjectProvider<MagicApiMcpCapabilityRegistrar> capabilityRegistrarProvider;

	private final ApplicationEventPublisher eventPublisher;

	/**
	 * 两个 registrar 用 {@link ObjectProvider} 惰性取：动态注册表依赖本类，而本类的刷新又需要
	 * registrar，构造期直接依赖会成环。
	 */
	public MagicMcpNotifier(List<McpServerRuntime> runtimes, MagicDynamicMcpServer dynamicServer,
			ObjectProvider<MagicApiMcpToolRegistrar> toolRegistrarProvider,
			ObjectProvider<MagicApiMcpCapabilityRegistrar> capabilityRegistrarProvider,
			ApplicationEventPublisher eventPublisher) {
		this.runtimes = runtimes == null ? Collections.emptyList() : new ArrayList<>(runtimes);
		this.dynamicServer = dynamicServer;
		this.toolRegistrarProvider = toolRegistrarProvider;
		this.capabilityRegistrarProvider = capabilityRegistrarProvider;
		this.eventPublisher = eventPublisher;
	}

	/**
	 * 工具列表变更（api/function 新增、修改、删除）。
	 */
	public void publishToolsChanged() {
		for (McpServer server : servers()) {
			server.publishToolsChanged();
		}
		emit("tools");
	}

	/**
	 * 集群消息驱动：先按最新快照刷新各实例，再广播 list_changed。
	 */
	public void refreshTools() {
		MagicApiMcpToolRegistrar registrar = toolRegistrarProvider == null
				? null : toolRegistrarProvider.getIfAvailable();
		if (registrar != null) {
			registrar.registerAll(runtimeTargets(), new java.util.concurrent.atomic.AtomicBoolean());
		}
		publishToolsChanged();
	}

	/**
	 * 集群消息驱动：刷新 resources 能力并广播。
	 */
	public void refreshResources() {
		refreshCapabilities();
		publishResourcesChanged();
	}

	/**
	 * 集群消息驱动：刷新 prompts 能力并广播。
	 */
	public void refreshPrompts() {
		refreshCapabilities();
		publishPromptsChanged();
	}

	private void refreshCapabilities() {
		MagicApiMcpCapabilityRegistrar registrar = capabilityRegistrarProvider == null
				? null : capabilityRegistrarProvider.getIfAvailable();
		if (registrar != null) {
			registrar.refresh();
		}
	}

	/**
	 * 资源列表变更（mcp-resource 新增或删除）。
	 */
	public void publishResourcesChanged() {
		for (McpServer server : servers()) {
			server.publishResourcesChanged();
		}
		emit("resources");
	}

	/**
	 * 提示词列表变更（mcp-prompt 新增、修改或删除）。
	 */
	public void publishPromptsChanged() {
		for (McpServer server : servers()) {
			server.publishPromptsChanged();
		}
		emit("prompts");
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
	 * 全部实例（starter runtime + 插件动态实例）：供诊断接口等需要统一视图的调用方使用。
	 *
	 * @return 实例列表
	 */
	public List<McpServerRuntime> all() {
		List<McpServerRuntime> all = new ArrayList<>(runtimes);
		if (dynamicServer != null) {
			all.addAll(dynamicServer.serverNodes());
		}
		return all;
	}

	/**
	 * 全部实例的能力目标，供工具批量注册复用一次快照扫描。
	 */
	private List<MagicApiMcpToolRegistrar.RuntimeTarget> runtimeTargets() {
		List<MagicApiMcpToolRegistrar.RuntimeTarget> targets = new ArrayList<>();
		for (McpServerRuntime runtime : runtimes) {
			if (runtime.getFeatureMgr() != null) {
				targets.add(new MagicApiMcpToolRegistrar.RuntimeTarget(runtime.getFeatureMgr(),
						runtime.getServer()));
			}
		}
		if (dynamicServer != null) {
			for (McpServerRuntime runtime : dynamicServer.serverNodes()) {
				if (runtime.getFeatureMgr() != null) {
					targets.add(new MagicApiMcpToolRegistrar.RuntimeTarget(runtime.getFeatureMgr(),
							runtime.getServer()));
				}
			}
		}
		return targets;
	}

	/**
	 * 发布能力变更事件：集群广播端（有 Redis 时才注册）据此转发到其它节点；
	 * 单节点无订阅者，天然降级为仅本地生效。
	 */
	private void emit(String capability) {
		if (eventPublisher != null) {
			eventPublisher.publishEvent(new MagicMcpCapabilityChangedEvent(capability));
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
