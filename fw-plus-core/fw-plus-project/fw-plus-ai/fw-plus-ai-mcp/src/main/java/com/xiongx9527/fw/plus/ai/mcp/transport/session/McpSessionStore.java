package com.xiongx9527.fw.plus.ai.mcp.transport.session;

import java.util.List;
import java.util.Map;

/**
 * MCP 会话存储抽象接口。
 *
 * <p>提供会话数据的持久化能力，支持内存和 Redis 等多种后端实现。
 * 会话数据包含事件序列号、事件缓冲和最后访问时间。
 *
 * @author xiongxiao
 * @date 2026-09-22
 */
public interface McpSessionStore {

	/**
	 * 保存或更新会话数据。
	 *
	 * @param sessionId 会话标识
	 * @param sessionData 会话数据（包含事件序列号、事件缓冲、最后访问时间）
	 */
	void save(String sessionId, McpSessionData sessionData);

	/**
	 * 加载会话数据。
	 *
	 * @param sessionId 会话标识
	 * @return 会话数据，不存在时返回 {@code null}
	 */
	McpSessionData load(String sessionId);

	/**
	 * 删除会话数据。
	 *
	 * @param sessionId 会话标识
	 */
	void remove(String sessionId);

	/**
	 * 更新会话最后访问时间（续期）。
	 *
	 * @param sessionId 会话标识
	 */
	void touch(String sessionId);
}
