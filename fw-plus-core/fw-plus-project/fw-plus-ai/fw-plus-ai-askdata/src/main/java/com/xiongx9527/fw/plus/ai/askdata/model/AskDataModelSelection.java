package com.xiongx9527.fw.plus.ai.askdata.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 一次 Chat 或 Embedding 调用的厂商中立模型选择输入。
 *
 * <p>各层级 key 表示真实业务配置来源；普通候选必须放入 {@code fallbackModelKeys}，不得伪装成
 * 项目、数据源或 Agent 路由。策略标签只作为可替换 Router 的输入，不包含 Provider 凭据。</p>
 */
public final class AskDataModelSelection {

	private static final AskDataModelSelection EMPTY = builder().build();

	private final String requestModelKey;
	private final String projectModelKey;
	private final String datasourceModelKey;
	private final String agentTaskModelKey;
	private final String policyModelKey;
	private final List<String> fallbackModelKeys;
	private final Map<String, String> policyTags;

	private AskDataModelSelection(Builder builder) {
		this.requestModelKey = normalize(builder.requestModelKey);
		this.projectModelKey = normalize(builder.projectModelKey);
		this.datasourceModelKey = normalize(builder.datasourceModelKey);
		this.agentTaskModelKey = normalize(builder.agentTaskModelKey);
		this.policyModelKey = normalize(builder.policyModelKey);
		this.fallbackModelKeys = immutableKeys(builder.fallbackModelKeys);
		this.policyTags = immutableTags(builder.policyTags);
	}

	public static AskDataModelSelection empty() { return EMPTY; }
	public static Builder builder() { return new Builder(); }

	public String getRequestModelKey() { return requestModelKey; }
	public String getProjectModelKey() { return projectModelKey; }
	public String getDatasourceModelKey() { return datasourceModelKey; }
	public String getAgentTaskModelKey() { return agentTaskModelKey; }
	public String getPolicyModelKey() { return policyModelKey; }
	public List<String> getFallbackModelKeys() { return fallbackModelKeys; }
	public Map<String, String> getPolicyTags() { return policyTags; }

	public boolean isEmpty() {
		return requestModelKey == null && projectModelKey == null && datasourceModelKey == null
				&& agentTaskModelKey == null && policyModelKey == null && fallbackModelKeys.isEmpty()
				&& policyTags.isEmpty();
	}

	private static List<String> immutableKeys(List<String> values) {
		if (values == null || values.isEmpty()) return Collections.emptyList();
		List<String> copy = new ArrayList<String>();
		for (String value : values) {
			String normalized = normalize(value);
			if (normalized != null && !copy.contains(normalized)) copy.add(normalized);
		}
		return Collections.unmodifiableList(copy);
	}

	private static Map<String, String> immutableTags(Map<String, String> values) {
		if (values == null || values.isEmpty()) return Collections.emptyMap();
		Map<String, String> copy = new LinkedHashMap<String, String>();
		for (Map.Entry<String, String> entry : values.entrySet()) {
			String key = normalize(entry.getKey());
			String value = normalize(entry.getValue());
			if (key == null || value == null) {
				throw new IllegalArgumentException("policyTags 的 key 和 value 不能为空白");
			}
			copy.put(key, value);
		}
		return Collections.unmodifiableMap(copy);
	}

	private static String normalize(String value) {
		return value == null || value.trim().length() == 0 ? null : value.trim();
	}

	public static final class Builder {
		private String requestModelKey;
		private String projectModelKey;
		private String datasourceModelKey;
		private String agentTaskModelKey;
		private String policyModelKey;
		private List<String> fallbackModelKeys = Collections.emptyList();
		private Map<String, String> policyTags = Collections.emptyMap();

		public Builder requestModelKey(String value) { this.requestModelKey = value; return this; }
		public Builder projectModelKey(String value) { this.projectModelKey = value; return this; }
		public Builder datasourceModelKey(String value) { this.datasourceModelKey = value; return this; }
		public Builder agentTaskModelKey(String value) { this.agentTaskModelKey = value; return this; }
		public Builder policyModelKey(String value) { this.policyModelKey = value; return this; }
		public Builder fallbackModelKeys(List<String> value) { this.fallbackModelKeys = value; return this; }
		public Builder policyTags(Map<String, String> value) { this.policyTags = value; return this; }
		public AskDataModelSelection build() { return new AskDataModelSelection(this); }
	}
}
