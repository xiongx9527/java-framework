package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 统一上下文注入器：按有序的 {@link ContextProvider} 集合产出本轮前置文本块，按
 * {@link ContextStrategy} 决定是否注入。单个来源失败被视为空来源，不中断本轮。
 *
 * <p>与消息窗口正交：本注入器只负责「前置什么」，不改变 ChatMemory 的窗口语义。</p>
 */
@Slf4j
public class ContextInjector {

	private final List<ContextProvider> providers;

	private final ContextStrategy strategy;

	public ContextInjector(List<ContextProvider> providers, ContextStrategy strategy) {
		this.providers = providers == null
				? Collections.<ContextProvider>emptyList()
				: Collections.unmodifiableList(new ArrayList<ContextProvider>(providers));
		this.strategy = strategy == null ? ContextStrategy.MEMORY_AND_ENRICHED : strategy;
	}

	public ContextStrategy getStrategy() {
		return strategy;
	}

	/** 已启用的来源（只读） */
	public List<ContextProvider> getProviders() {
		return providers;
	}

	/**
	 * 渲染本轮上下文文本：按顺序拼接各来源内容，来源无内容或抛异常均跳过；
	 * 全部无内容时返回空串。策略为 {@link ContextStrategy#MEMORY} 时不注入任何来源。
	 */
	public String render(ContextRequest request) {
		if (!strategy.injectsEnriched() || providers.isEmpty()) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		for (ContextProvider provider : providers) {
			String block = provideQuietly(provider, request);
			if (block == null || block.trim().isEmpty()) {
				continue;
			}
			if (builder.length() > 0) {
				builder.append("\n\n");
			}
			builder.append(block.trim());
		}
		return builder.toString();
	}

	private String provideQuietly(ContextProvider provider, ContextRequest request) {
		try {
			return provider.provide(request);
		} catch (Exception e) {
			log.warn("上下文来源 {} 产出失败，按空来源处理：{}",
					provider.getClass().getName(), e.getMessage());
			return null;
		}
	}

}