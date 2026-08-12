package com.xiongx9527.fw.plus.ai.askdata.model;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** 对话模型的隔离请求。 */
public final class AskDataChatRequest {
	private final AskDataProjectContext context;
	private final String question;
	private final String prompt;
	private final String modelKey;
	private final Map<String, Object> parameters;
	private final long timeoutMillis;

	public AskDataChatRequest(AskDataProjectContext context, String question) {
		this(context, question, null, Collections.<String, Object>emptyMap(), 0L);
	}

	public AskDataChatRequest(AskDataProjectContext context, String question, String modelKey,
			Map<String, Object> parameters, long timeoutMillis) {
		this(context, question, question, modelKey, parameters, timeoutMillis);
	}

	public AskDataChatRequest(AskDataProjectContext context, String question, String prompt, String modelKey,
			Map<String, Object> parameters, long timeoutMillis) {
		if (context == null || question == null || question.trim().length() == 0
				|| prompt == null || prompt.trim().length() == 0) {
			throw new IllegalArgumentException("context、question 和 prompt 不能为空");
		}
		if (timeoutMillis < 0L) {
			throw new IllegalArgumentException("timeoutMillis 不能为负数");
		}
		this.context = context;
		this.question = question.trim();
		this.prompt = prompt.trim();
		this.modelKey = modelKey == null ? null : modelKey.trim();
		this.parameters = immutableParameters(parameters);
		this.timeoutMillis = timeoutMillis;
	}

	public AskDataProjectContext getContext() { return context; }
	public String getQuestion() { return question; }
	public String getPrompt() { return prompt; }
	public String getModelKey() { return modelKey; }
	public Map<String, Object> getParameters() { return parameters; }
	public long getTimeoutMillis() { return timeoutMillis; }

	private static Map<String, Object> immutableParameters(Map<String, Object> parameters) {
		if (parameters == null || parameters.isEmpty()) {
			return Collections.emptyMap();
		}
		return Collections.unmodifiableMap(new LinkedHashMap<String, Object>(parameters));
	}
}
