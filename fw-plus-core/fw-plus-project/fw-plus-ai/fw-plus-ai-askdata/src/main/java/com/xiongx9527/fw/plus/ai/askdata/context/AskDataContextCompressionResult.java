package com.xiongx9527.fw.plus.ai.askdata.context;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** 不可变的上下文压缩结果。 */
public final class AskDataContextCompressionResult {
	private final String summary;
	private final String actualModelKey;
	private final Map<String, String> structuredFields;
	public AskDataContextCompressionResult(String summary, String actualModelKey,
			Map<String, String> structuredFields) {
		if (summary == null || summary.trim().length() == 0) {
			throw new IllegalArgumentException("summary 不能为空白");
		}
		this.summary = summary.trim();
		this.actualModelKey = actualModelKey;
		this.structuredFields = structuredFields == null ? Collections.<String, String>emptyMap()
				: Collections.unmodifiableMap(new LinkedHashMap<String, String>(structuredFields));
	}
	public String getSummary() { return summary; }
	public String getActualModelKey() { return actualModelKey; }
	public Map<String, String> getStructuredFields() { return structuredFields; }
}
