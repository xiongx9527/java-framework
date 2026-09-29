package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.model.MagicDynamicLangchain4jToolProviderDataSource;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;

import java.util.Objects;

/**
 * 监听 Magic-API 的 api/function 资源变更，使全部 ToolProvider 实例的工具快照失效：
 * 下次 {@code provideTools} 重建，LLM 即可看到新增/删除的工具。
 *
 * <p>事件类型过滤由 Spring 的 {@code condition} 承担，task/ws 等无关资源不会派发进来。</p>
 */
public class MagicApiToolChangeListener {

	private final MagicDynamicLangchain4jToolProviderDataSource dataSource;

	public MagicApiToolChangeListener(MagicDynamicLangchain4jToolProviderDataSource dataSource) {
		this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
	}

	@EventListener(condition = "#event.type == 'api' || #event.type == 'function'")
	public void onFileEvent(FileEvent event) {
		dataSource.invalidateAll();
	}

	@EventListener(condition = "#event.type == 'api' || #event.type == 'function'")
	public void onGroupEvent(GroupEvent event) {
		dataSource.invalidateAll();
	}
}
