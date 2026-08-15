package com.xiongx9527.fw.plus.ai.askdata.context;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 厂商无关的上下文压缩请求。
 *
 * <p>{@code redactedContext} 只能包含允许发送给所选模型的脱敏内容。结构化字段保存恢复位置、授权版本、
 * 模型尝试等安全事实，压缩器必须原样返回，禁止让模型改写。</p>
 */
public final class AskDataContextCompressionRequest {
	private final AskDataProjectContext context;
	private final String redactedContext;
	private final Map<String, String> structuredFields;
	private final long timeoutMillis;
	public AskDataContextCompressionRequest(AskDataProjectContext context, String redactedContext,
			Map<String, String> structuredFields, long timeoutMillis) {
		if (context == null || redactedContext == null || timeoutMillis < 0L) {
			throw new IllegalArgumentException("context、redactedContext 和 timeoutMillis 必须有效");
		}
		this.context = context;
		this.redactedContext = redactedContext;
		this.structuredFields = structuredFields == null ? Collections.<String, String>emptyMap()
				: Collections.unmodifiableMap(new LinkedHashMap<String, String>(structuredFields));
		this.timeoutMillis = timeoutMillis;
	}
	public AskDataProjectContext getContext() { return context; }
	public String getRedactedContext() { return redactedContext; }
	public Map<String, String> getStructuredFields() { return structuredFields; }
	public long getTimeoutMillis() { return timeoutMillis; }
}
