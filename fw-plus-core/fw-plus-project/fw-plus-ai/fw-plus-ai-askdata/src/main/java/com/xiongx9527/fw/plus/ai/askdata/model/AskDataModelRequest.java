package com.xiongx9527.fw.plus.ai.askdata.model;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

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
	private final String defaultKey;

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
		if (projectContext == null) {
			throw new IllegalArgumentException("projectContext 不能为空");
		}
		this.projectContext = projectContext;
		this.requestModelKey = normalize(requestModelKey);
		this.projectKey = normalize(projectKey);
		this.datasourceKey = normalize(datasourceKey);
		this.agentTaskKey = normalize(agentTaskKey);
		this.defaultKey = normalize(defaultKey);
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

	public String getDefaultKey() {
		return defaultKey;
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
