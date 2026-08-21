package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.observability;

import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEvent;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEventCorrelation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 外部观测传输使用的已脱敏记录。
 *
 * <p>记录只包含关联标识、事件类别和模型计量元数据，不包含原始问题、Prompt、SQL、参数、记忆正文或凭据。</p>
 */
public final class AskDataObservationRecord {

	public enum Kind { TRACE, SPAN, GENERATION }

	private final Kind kind;
	private final String projectId;
	private final String operatorId;
	private final String requestId;
	private final String conversationId;
	private final String eventId;
	private final String parentEventId;
	private final String eventType;
	private final Map<String, String> metadata;

	private AskDataObservationRecord(Kind kind, String projectId, String operatorId, String requestId,
			String conversationId, String eventId, String parentEventId, String eventType,
			Map<String, String> metadata) {
		this.kind = kind;
		this.projectId = projectId;
		this.operatorId = operatorId;
		this.requestId = requestId;
		this.conversationId = conversationId;
		this.eventId = eventId;
		this.parentEventId = parentEventId;
		this.eventType = eventType;
		this.metadata = Collections.unmodifiableMap(new LinkedHashMap<String, String>(metadata));
	}

	public static AskDataObservationRecord from(AskDataEvent event) {
		if (event == null || event.getCorrelation() == null) {
			throw new IllegalArgumentException("event 及其 correlation 不能为空");
		}
		AskDataEventCorrelation correlation = event.getCorrelation();
		Kind kind = event.getModelKey() == null ? event.getType() == AskDataEvent.Type.CONVERSATION_STARTED
				? Kind.TRACE : Kind.SPAN : Kind.GENERATION;
		Map<String, String> metadata = new LinkedHashMap<String, String>();
		put(metadata, "subQuestionId", correlation.getSubQuestionId());
		put(metadata, "graphNodeId", correlation.getGraphNodeId());
		put(metadata, "agentKey", correlation.getAgentKey());
		put(metadata, "instanceId", correlation.getInstanceId());
		put(metadata, "modelType", event.getModelType() == null ? null : event.getModelType().name());
		put(metadata, "modelKey", event.getModelKey());
		put(metadata, "actualModelKey", event.getActualModelKey());
		put(metadata, "modelState", event.getModelState() == null ? null : event.getModelState().name());
		put(metadata, "modelFailureCode", event.getModelFailureCode());
		put(metadata, "modelSwitchReason", event.getModelSwitchReason());
		put(metadata, "modelRoutePolicy", event.getModelRoutePolicy() == null
				? null : event.getModelRoutePolicy().name());
		put(metadata, "modelRouteReason", event.getModelRouteReason());
		if (event.getModelUsage() != null && !event.getModelUsage().isUnknown()) {
			put(metadata, "inputTokens", String.valueOf(event.getModelUsage().getInputTokens()));
			put(metadata, "outputTokens", String.valueOf(event.getModelUsage().getOutputTokens()));
			put(metadata, "totalTokens", String.valueOf(event.getModelUsage().getTotalTokens()));
			put(metadata, "cost", event.getModelUsage().getCost() == null
					? null : event.getModelUsage().getCost().toPlainString());
		}
		put(metadata, "authorizationVersion", event.getAuthorizationVersion());
		put(metadata, "capabilityKey", event.getCapabilityKey());
		put(metadata, "purposeKey", event.getPurposeKey());
		return new AskDataObservationRecord(kind, event.getContext().getProjectId(),
				event.getContext().getOperatorId(), correlation.getRequestId(), correlation.getConversationId(),
				correlation.getEventId(), correlation.getParentEventId(), event.getType().name(), metadata);
	}

	public Kind getKind() { return kind; }
	public String getProjectId() { return projectId; }
	public String getOperatorId() { return operatorId; }
	public String getRequestId() { return requestId; }
	public String getConversationId() { return conversationId; }
	public String getEventId() { return eventId; }
	public String getParentEventId() { return parentEventId; }
	public String getEventType() { return eventType; }
	public Map<String, String> getMetadata() { return metadata; }

	private static void put(Map<String, String> metadata, String key, String value) {
		if (value != null && value.trim().length() > 0) {
			metadata.put(key, value);
		}
	}
}
