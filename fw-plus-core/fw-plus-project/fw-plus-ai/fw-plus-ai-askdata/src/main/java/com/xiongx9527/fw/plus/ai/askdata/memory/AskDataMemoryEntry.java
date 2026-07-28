package com.xiongx9527.fw.plus.ai.askdata.memory;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

/**
 * AskData 的长期记忆条目，绑定项目、操作人、数据源和语义版本。
 */
public final class AskDataMemoryEntry {

	private final AskDataProjectContext context;
	private final String datasourceKey;
	private final String semanticVersion;
	private final String content;

	public AskDataMemoryEntry(AskDataProjectContext context, String datasourceKey, String semanticVersion, String content) {
		if (context == null) {
			throw new IllegalArgumentException("context 不能为空");
		}
		this.context = context;
		this.datasourceKey = requireNonBlank(datasourceKey, "datasourceKey");
		this.semanticVersion = requireNonBlank(semanticVersion, "semanticVersion");
		this.content = requireNonBlank(content, "content");
	}

	public AskDataProjectContext getContext() {
		return context;
	}

	public String getDatasourceKey() {
		return datasourceKey;
	}

	public String getSemanticVersion() {
		return semanticVersion;
	}

	public String getContent() {
		return content;
	}

	private static String requireNonBlank(String value, String name) {
		if (value == null || value.trim().length() == 0) {
			throw new IllegalArgumentException(name + " 不能为空白");
		}
		return value.trim();
	}
}
