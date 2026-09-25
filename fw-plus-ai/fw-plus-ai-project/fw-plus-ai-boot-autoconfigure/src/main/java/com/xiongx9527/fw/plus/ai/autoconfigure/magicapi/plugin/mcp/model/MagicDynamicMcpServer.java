package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.auth.McpAuthService;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.cluster.McpRedisBroadcaster;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.runtime.McpRuntimeFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.runtime.McpServerRuntime;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.config.MagicApiMcpProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.starter.MagicApiMcpServerRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.tools.MagicApiMcpToolInvoker;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.tools.MagicApiMcpToolRegistrar;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.tools.MagicApiMcpToolRegistry;
import lombok.extern.slf4j.Slf4j;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.service.MagicApiMcpCapabilityRegistrar;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.ObjectProvider;
import org.ssssssss.magicapi.core.service.MagicResourceService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MCP server 动态数据源：按资源定义创建/销毁独立的 server 实例。
 *
 * <p>每个实例拥有自己的 transport registry、路由与工具过滤配置，互不影响。</p>
 */
@Slf4j
public class MagicDynamicMcpServer implements DisposableBean, SmartInitializingSingleton {

	private final MagicApiMcpProperties properties;

	private final MagicResourceService magicResourceService;

	private final McpRuntimeFactory runtimeFactory;

	private final McpAuthService authService;

	private final McpRedisBroadcaster broadcaster;

	private final MagicApiMcpToolInvoker toolInvoker;

	private final RuntimeRouteBinder routeBinder;

	private final Map<String, McpServerRuntime> serverMap = new LinkedHashMap<>();

	private final Map<String, MagicApiMcpToolRegistrar> registrarMap = new LinkedHashMap<>();

	private final Map<String, MagicMcpServerInfo> serverInfos = new LinkedHashMap<>();

	private final ObjectProvider<MagicApiMcpCapabilityRegistrar> capabilityRegistrarProvider;

	public MagicDynamicMcpServer(MagicApiMcpProperties properties, MagicResourceService magicResourceService,
			McpRuntimeFactory runtimeFactory, McpAuthService authService, McpRedisBroadcaster broadcaster,
			MagicApiMcpToolInvoker toolInvoker, RuntimeRouteBinder routeBinder,
			ObjectProvider<MagicApiMcpCapabilityRegistrar> capabilityRegistrarProvider) {
		this.properties = Objects.requireNonNull(properties, "properties");
		this.magicResourceService = Objects.requireNonNull(magicResourceService, "magicResourceService");
		this.runtimeFactory = Objects.requireNonNull(runtimeFactory, "runtimeFactory");
		this.authService = authService;
		this.broadcaster = broadcaster;
		this.toolInvoker = Objects.requireNonNull(toolInvoker, "toolInvoker");
		this.routeBinder = routeBinder;
		this.capabilityRegistrarProvider = capabilityRegistrarProvider;
	}

	/**
	 * 按插件配置项创建（或重建）一个 MCP server 实例。
	 *
	 * @param server 配置项
	 * @return 创建成功返回 {@code true}
	 */
	public synchronized boolean createClient(MagicApiMcpProperties.Server server) {
		return createClient(toInfo(server));
	}

	/**
	 * 创建（或重建）一个 MCP server 实例。
	 *
	 * @param info 资源定义
	 * @return 创建成功返回 {@code true}
	 */
	public synchronized boolean createClient(MagicMcpServerInfo info) {
		String key = info.getKey();
		if (key == null || key.trim().isEmpty()) {
			return false;
		}
		if (info.getEndpoint() == null || info.getEndpoint().trim().isEmpty()) {
			return false;
		}
		try {
			destroyClientByKey(key);

			McpProperties effective = toMcpProperties(info);
			McpServerRuntime runtime = runtimeFactory.createRuntime(effective, effective, authService, broadcaster,
					(config, featureMgr) -> new MagicApiMcpServerRegistry(toolInvoker));

			MagicApiMcpToolRegistry toolRegistry = new MagicApiMcpToolRegistry(magicResourceService, properties,
					info.getTools());
			MagicApiMcpToolRegistrar registrar = new MagicApiMcpToolRegistrar(toolRegistry);
			registrar.register(runtime.getFeatureMgr(), runtime.getServer());

			if (routeBinder != null) {
				routeBinder.bind(runtime);
			}
			if (capabilityRegistrarProvider != null) {
				MagicApiMcpCapabilityRegistrar capabilities = capabilityRegistrarProvider.getIfAvailable();
				if (capabilities != null) {
					capabilities.registerInstance(runtime.getFeatureMgr());
				}
			}
			serverMap.put(key, runtime);
			registrarMap.put(key, registrar);
			log.info("注册 MCP server:[{}] endpoint=[{}] 完成", key, runtime.getEndpoint());
			return true;
		} catch (Throwable e) {
			log.error("MCP server:[{}]注册失败", key, e);
			return false;
		}
	}

	/**
	 * 销毁一个 MCP server 实例。
	 *
	 * @param key 实例 key
	 */
	public synchronized void destroyClientByKey(String key) {
		McpServerRuntime runtime = serverMap.remove(key);
		registrarMap.remove(key);
		serverInfos.remove(key);
		if (runtime == null) {
			return;
		}
		if (capabilityRegistrarProvider != null) {
			MagicApiMcpCapabilityRegistrar capabilities = capabilityRegistrarProvider.getIfAvailable();
			if (capabilities != null) {
				capabilities.unregisterInstance(runtime.getFeatureMgr());
			}
		}
		if (routeBinder != null) {
			routeBinder.unbind(runtime);
		}
		if (runtime.getServer().getTransport() != null) {
			try {
				runtime.getServer().getTransport().close();
			} catch (Exception e) {
				log.warn("关闭 MCP server {} 的 transport 时发生异常", key, e);
			}
		}
		log.info("已关闭 MCP server: {}", key);
	}

	/**
	 * 已创建实例的定义快照，供控制台资源树展示默认映射。
	 *
	 * @return 实例定义集合
	 */
	public synchronized List<MagicMcpServerInfo> serverInfos() {
		List<MagicMcpServerInfo> infos = new ArrayList<>(serverInfos.values());
		List<MagicApiMcpProperties.Server> servers = properties.getServers();
		if (servers != null) {
			for (MagicApiMcpProperties.Server server : servers) {
				if (server.getKey() != null && !serverInfos.containsKey(server.getKey().trim())) {
					infos.add(toInfo(server));
				}
			}
		}
		return infos;
	}

	MagicMcpServerInfo toInfo(MagicApiMcpProperties.Server server) {
		MagicMcpServerInfo info = new MagicMcpServerInfo();
		info.setKey(server.getKey() == null ? null : server.getKey().trim());
		info.setName(server.getName());
		info.setTransport(server.getTransport());
		info.setEndpoint(server.getEndpoint());
		info.setTools(server.getTools());
		return info;
	}

	/**
	 * 按 key 获取实例。
	 *
	 * @param key 实例 key
	 * @return runtime，不存在返回 {@code null}
	 */
	public McpServerRuntime getServer(String key) {
		return serverMap.get(key);
	}

	/**
	 * 全部实例。
	 *
	 * @return runtime 集合
	 */
	public Collection<McpServerRuntime> serverNodes() {
		return Collections.unmodifiableList(new ArrayList<>(serverMap.values()));
	}

	/**
	 * 按 key 列出实例概要，供控制台展示。
	 *
	 * @return 概要集合
	 */
	public List<Map<String, Object>> serverSummaries() {
		List<Map<String, Object>> items = new ArrayList<>();
		for (McpServerRuntime runtime : serverMap.values()) {
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("key", runtime.getKey());
			item.put("endpoint", runtime.getEndpoint());
			item.put("transport", runtime.getTransport());
			items.add(item);
		}
		return items;
	}

	/**
	 * 刷新全部实例的工具快照，并回传是否真的发生了变化。
	 *
	 * @param changed 变更标记容器，调用方可据其决定是否广播通知
	 */
	public synchronized void refreshTools(AtomicBoolean changed) {
		for (Map.Entry<String, MagicApiMcpToolRegistrar> entry : registrarMap.entrySet()) {
			McpServerRuntime runtime = serverMap.get(entry.getKey());
			if (runtime != null) {
				entry.getValue().register(runtime.getFeatureMgr(), runtime.getServer(), changed);
			}
		}
	}

	/**
	 * 刷新全部实例的工具快照。
	 */
	public synchronized void refreshTools() {
		refreshTools(new AtomicBoolean());
	}

	@Override
	public void afterSingletonsInstantiated() {
		// 配置项驱动的实例必须在此真正创建：AbstractMagicDynamicRegistry#defaultMappings
		// 只服务于控制台资源树展示，打开控制台前不会被调用。
		List<MagicApiMcpProperties.Server> servers = properties.getServers();
		if (servers == null) {
			return;
		}
		for (MagicApiMcpProperties.Server server : servers) {
			createClient(server);
		}
	}

	@Override
	public void destroy() {
		for (String key : new ArrayList<>(serverMap.keySet())) {
			destroyClientByKey(key);
		}
	}

	private McpProperties toMcpProperties(MagicMcpServerInfo info) {
		McpProperties properties = new McpProperties();
		properties.setKey(info.getKey().trim());
		String transport = info.getTransport() == null || info.getTransport().trim().isEmpty()
				? "http-sse" : info.getTransport().trim();
		properties.setTransport(transport);
		properties.setEndpoint(info.getEndpoint().trim());
		properties.setServer(new McpProperties.Server());
		properties.getServer().setName(info.getName() == null || info.getName().trim().isEmpty()
				? info.getKey().trim() : info.getName().trim());
		properties.getServer().setVersion("1.0.0");
		return properties;
	}

	/**
	 * 路由绑定回调：由 web 栈实现，完成 endpoint 注册/注销。
	 */
	public interface RuntimeRouteBinder {

		void bind(McpServerRuntime runtime);

		void unbind(McpServerRuntime runtime);
	}
}
