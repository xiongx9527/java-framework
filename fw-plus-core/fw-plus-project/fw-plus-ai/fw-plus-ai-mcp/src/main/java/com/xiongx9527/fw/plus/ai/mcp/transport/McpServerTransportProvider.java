package com.xiongx9527.fw.plus.ai.mcp.transport;

import com.xiongx9527.fw.plus.ai.mcp.protocol.McpJsonRpcRequest;
import com.xiongx9527.fw.plus.ai.mcp.protocol.McpJsonRpcResponse;

import java.util.Collection;

/**
 * MCP 服务器传输提供者接口（Classic 模式）
 *
 * <p>用于 STDIO 或 Legacy HTTP SSE 等单连接场景。
 *
 * @author xiongxiao
 * @date 2025-09-20
 */
public interface McpServerTransportProvider {

	/**
	 * 设置会话工厂，在服务器创建时调用
	 */
	void setSessionFactory(SessionFactory sessionFactory);

	/**
	 * 启动传输监听
	 */
	void start() throws Exception;

	/**
	 * 停止传输监听
	 */
	void stop() throws Exception;

	/**
	 * 获取支持的协议版本
	 */
	Collection<String> protocolVersions();

	/**
	 * 会话工厂接口
	 */
	interface SessionFactory {
		/**
		 * 创建新会话
		 */
		Session createSession(McpServerTransport transport);
	}

	/**
	 * 会话接口
	 */
	interface Session {
		/**
		 * 发送通知给客户端
		 */
		void sendNotification(String method, Object params) throws Exception;

		/**
		 * 关闭会话
		 */
		void close() throws Exception;
	}
}
