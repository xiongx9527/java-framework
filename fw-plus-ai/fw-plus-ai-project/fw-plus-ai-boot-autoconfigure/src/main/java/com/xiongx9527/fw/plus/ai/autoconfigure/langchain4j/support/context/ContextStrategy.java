package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context;

/**
 * 上下文组合策略：决定模型输入里保留哪些部分。与消息窗口正交——
 * 窗口本身由 ChatMemory 决定，本枚举只控制是否注入附加来源。
 */
public enum ContextStrategy {

	/** 仅最近消息窗口，不注入任何附加来源 */
	MEMORY,

	/** 仅增强来源（摘要等），等价于「窗口缩小 + 附加来源」的增强形态 */
	ENRICHED,

	/** 最近消息窗口 + 全部已启用附加来源（默认） */
	MEMORY_AND_ENRICHED;

	/** 是否注入附加来源（摘要 / 事实） */
	public boolean injectsEnriched() {
		return this != MEMORY;
	}

	/** 是否保留最近消息窗口原文 */
	public boolean keepsMemory() {
		return this != ENRICHED;
	}

	/** 按配置名解析：{@code memory} / {@code enriched} / {@code memory-and-enriched}；空或未知回退默认 */
	public static ContextStrategy fromConfig(String name) {
		if (name == null || name.trim().isEmpty()) {
			return MEMORY_AND_ENRICHED;
		}
		String normalized = name.trim().toLowerCase().replace('_', '-');
		if ("memory".equals(normalized)) {
			return MEMORY;
		}
		if ("enriched".equals(normalized)) {
			return ENRICHED;
		}
		return MEMORY_AND_ENRICHED;
	}

}