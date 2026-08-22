package com.xiongx9527.fw.plus.ai.askdata.observability;

import java.util.UUID;

/**
 * AskData 运行事件的不可变关联标识。
 *
 * <p>关联对象只保存稳定标识，不保存问题、Prompt、SQL、参数值或结果正文。观测适配器可据此重建
 * 请求、子问题、图节点、Agent 和跨实例恢复之间的父子关系。</p>
 */
public final class AskDataEventCorrelation {

	private final String eventId;
	private final String requestId;
	private final String subQuestionId;
	private final String conversationId;
	private final String graphNodeId;
	private final String agentKey;
	private final String instanceId;
	private final String parentEventId;

	private AskDataEventCorrelation(Builder builder) {
		this.eventId = normalize(builder.eventId) == null ? UUID.randomUUID().toString() : normalize(builder.eventId);
		this.requestId = requireNonBlank(builder.requestId, "requestId");
		this.conversationId = requireNonBlank(builder.conversationId, "conversationId");
		this.subQuestionId = normalize(builder.subQuestionId);
		this.graphNodeId = normalize(builder.graphNodeId);
		this.agentKey = normalize(builder.agentKey);
		this.instanceId = normalize(builder.instanceId);
		this.parentEventId = normalize(builder.parentEventId);
	}

	public static Builder builder(String requestId, String conversationId) {
		return new Builder(requestId, conversationId);
	}

	public String getEventId() { return eventId; }
	public String getRequestId() { return requestId; }
	public String getSubQuestionId() { return subQuestionId; }
	public String getConversationId() { return conversationId; }
	public String getGraphNodeId() { return graphNodeId; }
	public String getAgentKey() { return agentKey; }
	public String getInstanceId() { return instanceId; }
	public String getParentEventId() { return parentEventId; }

	public static final class Builder {

		private String eventId;
		private final String requestId;
		private final String conversationId;
		private String subQuestionId;
		private String graphNodeId;
		private String agentKey;
		private String instanceId;
		private String parentEventId;

		private Builder(String requestId, String conversationId) {
			this.requestId = requestId;
			this.conversationId = conversationId;
		}

		public Builder eventId(String eventId) { this.eventId = eventId; return this; }
		public Builder subQuestionId(String subQuestionId) { this.subQuestionId = subQuestionId; return this; }
		public Builder graphNodeId(String graphNodeId) { this.graphNodeId = graphNodeId; return this; }
		public Builder agentKey(String agentKey) { this.agentKey = agentKey; return this; }
		public Builder instanceId(String instanceId) { this.instanceId = instanceId; return this; }
		public Builder parentEventId(String parentEventId) { this.parentEventId = parentEventId; return this; }

		public AskDataEventCorrelation build() { return new AskDataEventCorrelation(this); }
	}

	private static String requireNonBlank(String value, String name) {
		String normalized = normalize(value);
		if (normalized == null) {
			throw new IllegalArgumentException(name + " 不能为空");
		}
		return normalized;
	}

	private static String normalize(String value) {
		return value == null || value.trim().length() == 0 ? null : value.trim();
	}
}
