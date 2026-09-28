package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.tool;

import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;

import java.util.Objects;

/**
 * 监听 Magic-API 的 api/function 资源变更，使工具快照失效：
 * 下次 {@code provideTools} 重建，LLM 即可看到新增/删除的工具。
 *
 * <p>事件类型过滤由 Spring 的 {@code condition} 承担，task/ws 等无关资源不会派发进来。</p>
 */
public class MagicApiToolChangeListener {

	private final MagicApiToolRegistry registry;

	public MagicApiToolChangeListener(MagicApiToolRegistry registry) {
		this.registry = Objects.requireNonNull(registry, "registry");
	}

	@EventListener(condition = "#event.type == 'api' || #event.type == 'function'")
	public void onFileEvent(FileEvent event) {
		registry.invalidateSnapshot();
	}

	@EventListener(condition = "#event.type == 'api' || #event.type == 'function'")
	public void onGroupEvent(GroupEvent event) {
		registry.invalidateSnapshot();
	}
}
