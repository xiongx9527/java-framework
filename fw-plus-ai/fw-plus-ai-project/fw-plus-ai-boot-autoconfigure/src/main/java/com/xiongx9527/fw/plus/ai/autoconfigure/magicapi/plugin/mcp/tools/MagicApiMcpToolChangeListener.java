package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.tools;

import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.runtime.McpServerRuntime;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.model.MagicDynamicMcpServer;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.notify.MagicMcpNotifier;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 监听 Magic-API {@code api} 与 {@code function} 资源变更事件，增量同步工具快照到各实例，
 * 并由 {@link MagicMcpNotifier} 广播 {@code notifications/tools/list_changed}。
 *
 * <p>Spring 事件为同步派发，{@link MagicApiMcpToolRegistrar#register} 自身 synchronized，
 * 因此多个事件在同一时刻只会有一次 diff 在跑，不会并发改写 toolStore。</p>
 */
public class MagicApiMcpToolChangeListener {

	private final MagicApiMcpToolRegistrar registrar;

	private final List<McpServerRuntime> runtimes;

	private final MagicDynamicMcpServer dynamicServer;

	private final MagicMcpNotifier notifier;

	public MagicApiMcpToolChangeListener(MagicApiMcpToolRegistrar registrar, List<McpServerRuntime> runtimes) {
		this(registrar, runtimes, null, null);
	}

	public MagicApiMcpToolChangeListener(MagicApiMcpToolRegistrar registrar, List<McpServerRuntime> runtimes,
			MagicDynamicMcpServer dynamicServer, MagicMcpNotifier notifier) {
		this.registrar = Objects.requireNonNull(registrar, "registrar");
		this.runtimes = runtimes == null ? Collections.emptyList() : new ArrayList<>(runtimes);
		this.dynamicServer = dynamicServer;
		this.notifier = notifier;
	}

	@EventListener(condition = "#event.type == 'api' || #event.type == 'function'")
	public void onFileEvent(FileEvent event) {
		refresh();
	}

	@EventListener(condition = "#event.type == 'api' || #event.type == 'function'")
	public void onGroupEvent(GroupEvent event) {
		refresh();
	}

	/**
	 * 对全部实例执行一次增量同步，任一实例发生变化即广播一次工具列表变更通知。
	 */
	public void refresh() {
		AtomicBoolean changed = new AtomicBoolean(false);
		registrar.invalidateSource();
		List<MagicApiMcpToolRegistrar.RuntimeTarget> targets = new ArrayList<>();
		for (McpServerRuntime runtime : runtimes) {
			targets.add(new MagicApiMcpToolRegistrar.RuntimeTarget(runtime.getFeatureMgr(), runtime.getServer()));
		}
		if (!targets.isEmpty()) {
			registrar.registerAll(targets, changed);
		}
		if (dynamicServer != null) {
			dynamicServer.refreshTools(changed);
		}
		if (notifier != null && changed.get()) {
			notifier.publishToolsChanged();
		}
	}
}
