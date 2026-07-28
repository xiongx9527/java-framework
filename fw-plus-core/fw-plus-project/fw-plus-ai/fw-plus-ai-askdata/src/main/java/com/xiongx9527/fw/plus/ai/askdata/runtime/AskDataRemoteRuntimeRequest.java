package com.xiongx9527.fw.plus.ai.askdata.runtime;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

/**
 * 发往 Java 17 独立运行时适配服务的不可变请求。
 *
 * <p>项目和操作人标识维持 Core 隔离边界；会话、请求和 audit correlation ID 用于跨服务
 * 审计关联。该契约不依赖 HTTP、Spring AI 或 AgentScope，以允许适配服务独立升级。</p>
 */
public final class AskDataRemoteRuntimeRequest {

	private final AskDataProjectContext projectContext;
	private final String conversationId;
	private final String requestId;
	private final String capabilityKey;
	private final String auditCorrelationId;
	private final String question;
	private final String protocolVersion;
	private final String authenticationSubject;
	private final String idempotencyKey;

	public AskDataRemoteRuntimeRequest(AskDataProjectContext projectContext, String conversationId, String requestId,
			String capabilityKey, String auditCorrelationId, String question) {
		this(projectContext, conversationId, requestId, capabilityKey, auditCorrelationId, question, "v1", "core", requestId);
	}

	public AskDataRemoteRuntimeRequest(AskDataProjectContext projectContext, String conversationId, String requestId,
			String capabilityKey, String auditCorrelationId, String question, String protocolVersion,
			String authenticationSubject, String idempotencyKey) {
		if (projectContext == null) {
			throw new IllegalArgumentException("projectContext 不能为空");
		}
		this.projectContext = projectContext;
		this.conversationId = requireNonBlank(conversationId, "conversationId");
		this.requestId = requireNonBlank(requestId, "requestId");
		this.capabilityKey = requireNonBlank(capabilityKey, "capabilityKey");
		this.auditCorrelationId = requireNonBlank(auditCorrelationId, "auditCorrelationId");
		this.question = requireNonBlank(question, "question");
		this.protocolVersion = requireNonBlank(protocolVersion, "protocolVersion");
		this.authenticationSubject = requireNonBlank(authenticationSubject, "authenticationSubject");
		this.idempotencyKey = requireNonBlank(idempotencyKey, "idempotencyKey");
	}

	public String getProjectId() {
		return projectContext.getProjectId();
	}

	public String getOperatorId() {
		return projectContext.getOperatorId();
	}

	public String getConversationId() {
		return conversationId;
	}

	public String getRequestId() {
		return requestId;
	}

	public String getCapabilityKey() {
		return capabilityKey;
	}

	public String getAuditCorrelationId() {
		return auditCorrelationId;
	}

	public String getQuestion() {
		return question;
	}

	public String getProtocolVersion() { return protocolVersion; }
	public String getAuthenticationSubject() { return authenticationSubject; }
	public String getIdempotencyKey() { return idempotencyKey; }

	private static String requireNonBlank(String value, String fieldName) {
		if (value == null || value.trim().length() == 0) {
			throw new IllegalArgumentException(fieldName + " 不能为空白");
		}
		return value.trim();
	}
}
