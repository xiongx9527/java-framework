package com.xiongx9527.fw.plus.ai.askdata.tool;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import java.util.Collections; import java.util.LinkedHashMap; import java.util.Map;

/** 带隔离上下文的工具请求。 */
public final class AskDataToolRequest {
	private final AskDataProjectContext context; private final String toolKey; private final Map<String, String> arguments;
	public AskDataToolRequest(AskDataProjectContext context, String toolKey, Map<String, String> arguments) {
		if (context == null || toolKey == null || toolKey.trim().length() == 0) throw new IllegalArgumentException("context 和 toolKey 不能为空");
		this.context = context; this.toolKey = toolKey.trim(); this.arguments = Collections.unmodifiableMap(new LinkedHashMap<String, String>(arguments == null ? Collections.<String, String>emptyMap() : arguments));
	}
	public AskDataProjectContext getContext() { return context; } public String getToolKey() { return toolKey; } public Map<String, String> getArguments() { return arguments; }
}
