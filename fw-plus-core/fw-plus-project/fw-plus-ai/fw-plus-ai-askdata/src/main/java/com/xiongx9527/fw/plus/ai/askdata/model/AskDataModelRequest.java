package com.xiongx9527.fw.plus.ai.askdata.model;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 模型路由请求。
 *
 * <p>项目与操作人上下文是路由审计和隔离边界的一部分。五个模型 key 按固定优先级保存，
 * 允许上层未配置的 key 为空白；最终 default key 缺失时由路由器拒绝请求。</p>
 */
public final class AskDataModelRequest {

	private final AskDataProjectContext projectContext;
	private final String requestModelKey;
	private final String projectKey;
	private final String datasourceKey;
	private final String agentTaskKey;
	private final String policyKey;
	private final List<String> fallbackKeys;
	private final String defaultKey;
	private final Map<String, String> policyTags;

	/**
	 * 创建模型路由请求。
	 *
	 * @param projectContext 项目与操作人隔离上下文，不能为空
	 * @param requestModelKey 调用方显式指定的模型 key，可为空白
	 * @param projectKey 项目级模型 key，可为空白
	 * @param datasourceKey 数据源级模型 key，可为空白
	 * @param agentTaskKey 智能体任务级模型 key，可为空白
	 * @param defaultKey 默认模型 key，可为空白但路由时会拒绝
	 */
	public AskDataModelRequest(AskDataProjectContext projectContext, String requestModelKey, String projectKey,
			String datasourceKey, String agentTaskKey, String defaultKey) {
		this(projectContext, requestModelKey, projectKey, datasourceKey, agentTaskKey, null,
				Collections.<String>emptyList(), defaultKey, Collections.<String, String>emptyMap());
	}

	public AskDataModelRequest(AskDataProjectContext projectContext, String requestModelKey, String projectKey,
			String datasourceKey, String agentTaskKey, String policyKey, List<String> fallbackKeys,
			String defaultKey, Map<String, String> policyTags) {
		if (projectContext == null) {
			throw new IllegalArgumentException("projectContext 不能为空");
		}
		this.projectContext = projectContext;
		this.requestModelKey = normalize(requestModelKey);
		this.projectKey = normalize(projectKey);
		this.datasourceKey = normalize(datasourceKey);
		this.agentTaskKey = normalize(agentTaskKey);
		this.policyKey = normalize(policyKey);
		this.fallbackKeys = immutableKeys(fallbackKeys);
		this.defaultKey = normalize(defaultKey);
		this.policyTags = immutableTags(policyTags);
	}

	public AskDataProjectContext getProjectContext() {
		return projectContext;
	}

	public String getRequestModelKey() {
		return requestModelKey;
	}

	public String getProjectKey() {
		return projectKey;
	}

	public String getDatasourceKey() {
		return datasourceKey;
	}

	public String getAgentTaskKey() {
		return agentTaskKey;
	}

	public String getPolicyKey() { return policyKey; }

	public List<String> getFallbackKeys() { return fallbackKeys; }

	public String getDefaultKey() {
		return defaultKey;
	}

	public Map<String, String> getPolicyTags() { return policyTags; }

	private static List<String> immutableKeys(List<String> values) {
		if (values == null || values.isEmpty()) return Collections.emptyList();
		List<String> keys = new ArrayList<String>();
		for (String value : values) {
			String key = normalize(value);
			if (key.length() > 0) keys.add(key);
		}
		return Collections.unmodifiableList(keys);
	}

	private static Map<String, String> immutableTags(Map<String, String> values) {
		if (values == null || values.isEmpty()) return Collections.emptyMap();
		Map<String, String> tags = new LinkedHashMap<String, String>();
		for (Map.Entry<String, String> entry : values.entrySet()) {
			String key = normalize(entry.getKey());
			String value = normalize(entry.getValue());
			if (key.length() == 0 || value.length() == 0) {
				throw new IllegalArgumentException("policyTags 的 key 和 value 不能为空白");
			}
			tags.put(key, value);
		}
		return Collections.unmodifiableMap(tags);
	}

	private static String normalize(String value) {
		if (value == null) {
			return "";
		}
		int beginIndex = 0;
		int endIndex = value.length();
		while (beginIndex < endIndex && isWhitespace(value.charAt(beginIndex))) {
			beginIndex++;
		}
		while (beginIndex < endIndex && isWhitespace(value.charAt(endIndex - 1))) {
			endIndex--;
		}
		return value.substring(beginIndex, endIndex);
	}

	private static boolean isWhitespace(char character) {
		return Character.isWhitespace(character) || Character.isSpaceChar(character);
	}
}
