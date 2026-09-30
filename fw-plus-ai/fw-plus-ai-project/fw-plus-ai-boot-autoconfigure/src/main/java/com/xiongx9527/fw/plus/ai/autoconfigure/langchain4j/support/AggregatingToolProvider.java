package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support;

import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 组合工具提供者：持有多个 {@link ToolProvider}，把各家的工具规格合并成一份。
 *
 * <p>0.35 的 {@code AiServices} 只接受单个 {@code toolProvider}，本类把多个提供者
 * 包装成一个塞进去。合并规则：按声明顺序依次合并（后写的同名工具覆盖先写的）；
 * 单路抛错只记日志并跳过（一路不可用不应让整次工具装配失败）。</p>
 */
public class AggregatingToolProvider implements ToolProvider {

	private static final Logger logger = LoggerFactory.getLogger(AggregatingToolProvider.class);

	private final List<ToolProvider> delegates;

	public AggregatingToolProvider(List<ToolProvider> delegates) {
		this.delegates = delegates == null ? Collections.<ToolProvider>emptyList() : delegates;
	}

	/** 数组构造。 */
	public AggregatingToolProvider(ToolProvider... delegates) {
		this(delegates == null ? Collections.<ToolProvider>emptyList() : Arrays.asList(delegates));
	}

	@Override
	public ToolProviderResult provideTools(ToolProviderRequest request) {
		ToolProviderResult.Builder merged = ToolProviderResult.builder();
		List<ToolProvider> current = new ArrayList<>(delegates);
		for (ToolProvider delegate : current) {
			try {
				ToolProviderResult part = delegate.provideTools(request);
				if (part != null && part.tools() != null && !part.tools().isEmpty()) {
					merged.addAll(part.tools());
				}
			} catch (Exception e) {
				logger.warn("聚合工具：子提供者 {} 提供工具失败，已跳过", delegate.getClass().getName(), e);
			}
		}
		return merged.build();
	}
}
