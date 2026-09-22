package com.xiongx9527.fw.plus.ai.mcp.spi;

import java.util.Map;

/**
 * MCP 提供者执行上下文
 *
 * <p>包含当前会话的 sessionId 和其他元数据。
 * 实现 McpToolProvider/McpResourceProvider 等接口时，
 * 可通过此上下文获取会话信息。
 *
 * @author xiongxiao
 * @date 2025-09-20
 */
public class McpProviderContext {
	private final String sessionId;
	private final Map<String, Object> sessionData;

	public McpProviderContext(String sessionId) {
		this(sessionId, null);
	}

	/**
	 * 创建无会话上下文（用于测试或无会话语境）
	 */
	public static McpProviderContext context() {
		return new McpProviderContext(null, null);
	}

	public McpProviderContext(String sessionId, Map<String, Object> sessionData) {
		this.sessionId = sessionId;
		this.sessionData = sessionData;
	}

	/**
	 * 获取会话 ID
	 */
	public String sessionId() {
		return sessionId;
	}

	/**
	 * 获取会话数据
	 */
	public Map<String, Object> sessionData() {
		return sessionData;
	}
}
