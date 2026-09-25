package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.tools;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.tools.ToolItem;
import com.ajaxjs.mcp.server.McpServer;
import com.ajaxjs.mcp.server.feature.FeatureMgr;
import com.ajaxjs.mcp.server.feature.model.ServerStoreTool;
import org.ssssssss.magicapi.core.service.MagicResourceService;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 将 {@link MagicApiMcpToolRegistry} 的工具快照同步到 aj-mcp {@link FeatureMgr} 的 toolStore。
 *
 * <p>aj-mcp 的 {@code toolCall} 以「反射 Method + paramsOrder 位置参数」执行工具，动态的
 * Magic-API 资源无法映射为编译期方法，因此这里注册一个无参占位方法：{@code tools/list} 由内核
 * 直接投影快照，实际执行交由 {@link MagicApiMcpServerRegistry} 在响应侧改写。工具集合无变化
 * 时不重复通知，避免无意义的 {@code notifications/tools/list_changed}。</p>
 */
public class MagicApiMcpToolRegistrar {

	/**
	 * 占位执行结果，客户端不会看到（响应会被 registry 侧改写）。
	 */
	public static final Object PLACEHOLDER = new Object() {
		@Override
		public String toString() {
			return "magic-api-mcp-placeholder";
		}
	};

	private static final Method PLACEHOLDER_METHOD = resolvePlaceholderMethod();

	private final MagicApiMcpToolRegistry registry;

	private final Set<String> registered = new LinkedHashSet<>();

	public MagicApiMcpToolRegistrar(MagicApiMcpToolRegistry registry) {
		this.registry = Objects.requireNonNull(registry, "registry");
	}

	/**
	 * 为新增的 server 实例派生独立注册器：共享工具快照来源，但保持各自的增量状态，
	 * 避免实例间 {@code registered} 集合互相干扰导致工具被误删或漏发通知。
	 *
	 * @return 新的注册器
	 */
	public MagicApiMcpToolRegistrar newInstanceRegistrar() {
		return new MagicApiMcpToolRegistrar(registry);
	}

	/**
	 * 同步工具快照到 toolStore。
	 *
	 * <p>幂等：工具集合与内容均未变化时不改动 store。广播 {@code notifications/tools/list_changed}
	 * 由调用方经 {@code MagicMcpNotifier} 统一负责，本类只做 store diff。</p>
	 *
	 * @return 本次注册的工具名
	 */
	public synchronized List<String> register(FeatureMgr featureMgr, McpServer server) {
		return register(featureMgr, server, null);
	}

	/**
	 * 同步工具快照到 toolStore，并回传本次是否真的发生变化。
	 *
	 * @param featureMgr  目标实例的能力管理器
	 * @param server      目标实例
	 * @param changedSink 变更标记容器，调用方可据其决定是否广播通知
	 * @return 本次注册的工具名
	 */
	public synchronized List<String> register(FeatureMgr featureMgr, McpServer server,
			AtomicBoolean changedSink) {
		return register(featureMgr, server, registry.snapshot(), changedSink);
	}

	/**
	 * 批量注册多个实例：整批只扫描一次工具快照，避免实例数 × 全量扫描。
	 *
	 * @param targets     目标实例列表
	 * @param changedSink 变更标记容器
	 * @return 各实例注册的工具名
	 */
	public synchronized Map<FeatureMgr, List<String>> registerAll(List<RuntimeTarget> targets,
			AtomicBoolean changedSink) {
		List<MagicApiMcpTool> snapshot = registry.snapshot();
		Map<FeatureMgr, List<String>> registered = new LinkedHashMap<>();
		for (RuntimeTarget target : targets) {
			registered.put(target.getFeatureMgr(),
					register(target.getFeatureMgr(), target.getServer(), snapshot, changedSink));
		}
		return registered;
	}

	private synchronized List<String> register(FeatureMgr featureMgr, McpServer server,
			List<MagicApiMcpTool> snapshot, AtomicBoolean changedSink) {
		Objects.requireNonNull(featureMgr, "featureMgr");
		Map<String, ServerStoreTool> toolStore = featureMgr.getToolStore();
		Set<String> current = new LinkedHashSet<>();
		List<String> names = new ArrayList<>();
		boolean updated = false;
		for (MagicApiMcpTool tool : snapshot) {
			if (tool.getName() == null || !current.add(tool.getName())) {
				continue;
			}
			names.add(tool.getName());
			ServerStoreTool existing = toolStore.get(tool.getName());
			ServerStoreTool desired = serverStoreTool(tool);
			if (existing == null) {
				toolStore.put(tool.getName(), desired);
				updated = true;
			} else if (!sameTool(existing.getTool(), desired.getTool())) {
				toolStore.put(tool.getName(), desired);
				updated = true;
			}
		}
		boolean removed = toolStore.keySet().removeIf(name -> registered.contains(name) && !current.contains(name));
		boolean changed = !current.equals(registered);
		registered.clear();
		registered.addAll(current);
		if (changedSink != null) {
			changedSink.set(changed || removed || updated);
		}
		return Collections.unmodifiableList(names);
	}

	/**
	 * 批量注册目标：实例能力管理器与 server。
	 */
	public static final class RuntimeTarget {

		private final FeatureMgr featureMgr;

		private final McpServer server;

		public RuntimeTarget(FeatureMgr featureMgr, McpServer server) {
			this.featureMgr = Objects.requireNonNull(featureMgr, "featureMgr");
			this.server = server;
		}

		public FeatureMgr getFeatureMgr() {
			return featureMgr;
		}

		public McpServer getServer() {
			return server;
		}
	}

	public boolean isRegistered(String toolName) {
		return registered.contains(toolName);
	}

	private ServerStoreTool serverStoreTool(MagicApiMcpTool tool) {
		ToolItem item = new ToolItem();
		item.setName(tool.getName());
		item.setDescription(tool.getDescription());
		item.setInputSchema(JsonUtils.convertValue(tool.getInputSchema(),
				com.ajaxjs.mcp.protocol.tools.JsonSchema.class));
		ServerStoreTool store = new ServerStoreTool();
		store.setTool(item);
		store.setMethod(PLACEHOLDER_METHOD);
		store.setInstance(this);
		store.setParamsOrder(Collections.emptyList());
		return store;
	}

	/**
	 * 占位方法，供 aj-mcp 反射调用。
	 */
	public Object placeholder() {
		return PLACEHOLDER;
	}

	private static Method resolvePlaceholderMethod() {
		try {
			Method method = MagicApiMcpToolRegistrar.class.getMethod("placeholder");
			method.setAccessible(true);
			return method;
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException("MagicApiMcpToolRegistrar#placeholder not found", e);
		}
	}

	/**
	 * 比较工具内容，以序列化结果为准，忽略实现类差异。
	 */
	private boolean sameTool(ToolItem left, ToolItem right) {
		if (left == right) {
			return true;
		}
		if (left == null || right == null) {
			return false;
		}
		return Objects.equals(JsonUtils.toJson(left), JsonUtils.toJson(right));
	}

	static MagicResourceService resourceService(MagicApiMcpToolRegistry registry) {
		return registry.getMagicResourceService();
	}
}
