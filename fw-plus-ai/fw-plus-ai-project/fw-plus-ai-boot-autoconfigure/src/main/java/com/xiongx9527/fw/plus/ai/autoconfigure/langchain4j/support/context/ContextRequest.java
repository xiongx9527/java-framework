package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context;

/**
 * 本轮请求的全部信息，供各 {@link ContextProvider} 取用。
 *
 * <p>字段均可能为 {@code null}（如只检索、无会话场景）；provider 需自行判空。</p>
 */
public final class ContextRequest {

	/** 5 段生效记忆键（会话级） */
	private final String effectiveMemoryId;

	/** 摘要锚点（会话键） */
	private final String sessionKey;

	/** 用户维度（段[3]），事实检索跨会话作用域 */
	private final String userId;

	/** 本轮用户输入（事实检索 query） */
	private final String latestUserMessage;

	public ContextRequest(String effectiveMemoryId, String sessionKey, String userId, String latestUserMessage) {
		this.effectiveMemoryId = effectiveMemoryId;
		this.sessionKey = sessionKey;
		this.userId = userId;
		this.latestUserMessage = latestUserMessage;
	}

	public String getEffectiveMemoryId() {
		return effectiveMemoryId;
	}

	public String getSessionKey() {
		return sessionKey;
	}

	public String getUserId() {
		return userId;
	}

	public String getLatestUserMessage() {
		return latestUserMessage;
	}

}