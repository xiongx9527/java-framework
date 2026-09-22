package com.xiongx9527.fw.plus.ai.mcp.transport;

/**
 * MCP Streamable HTTP 传输（单实例端点）
 *
 * <p>一个实例对应一个端点路径与一套独立会话空间（spec 5：端点基址可配置，支持多实例）。
 * 具体实现见 {@code transport.servlet}（Spring MVC + SseEmitter）与
 * {@code transport.reactive}（Spring WebFlux + Flux&lt;ServerSentEvent&gt;）。
 *
 * <p>会话标识头严格使用 {@code Mcp-Session-Id}。
 *
 * <p>聚合三类能力：{@link McpTransportLifecycle}（server 绑定）、
 * {@link McpStreamableServerTransportProvider}（JSON-RPC 分发与通知下发）
 * 以及本接口定义的会话管理。
 *
 * @author xiongxiao
 * @date 2025-09-21
 */
public interface McpStreamableTransport extends McpTransportLifecycle, McpStreamableServerTransportProvider {

	/**
	 * 实例名称
	 */
	String getName();

	/**
	 * 端点路径（以 / 开头）
	 */
	String getPath();

	/**
	 * 创建一个新会话并返回会话 ID
	 *
	 * <p>用于 POST initialize 且未携带 {@code Mcp-Session-Id} 时。
	 */
	String createSession();

	/**
	 * 会话是否存在（未被 DELETE 关闭、未被清理）
	 */
	boolean hasSession(String sessionId);

	/**
	 * 关闭会话并释放资源（SSE sink、订阅关系）
	 */
	void closeSession(String sessionId);

	/**
	 * 关闭全部会话（应用停止时）
	 */
	void closeAll();
}
