package com.xiongx9527.fw.plus.ai.mcp.transport;

import com.xiongx9527.fw.plus.ai.mcp.protocol.McpJsonRpcRequest;
import com.xiongx9527.fw.plus.ai.mcp.protocol.McpJsonRpcResponse;

import java.util.Collection;

/**
 * MCP Streamable HTTP 服务器传输提供者接口
 *
 * <p>框架无关抽象，具体实现（Servlet/WebFlux）在 AI 层。
 */
public interface McpStreamableServerTransportProvider {

    void setSessionFactory(SessionFactory sessionFactory);

    McpJsonRpcResponse handlePost(String sessionId, McpJsonRpcRequest request);

    Object handleGet(String sessionId);  // SSE stream - implementation specific type

    void handleDelete(String sessionId);

    void notifyClients(String method, Object params);

    /**
     * 向指定会话的 SSE 流发送通知
     *
     * <p>spec 5.1：服务端通知经该会话 SSE 流下发。
     * 与 {@link #notifyClients(String, Object)}（广播）互补。
     *
     * @param sessionId 目标会话 ID
     * @param method JSON-RPC 通知方法名（如 "notifications/tools/list_changed"）
     * @param params 通知参数
     */
    void notifyClient(String sessionId, String method, Object params);

    void start() throws Exception;

    void stop() throws Exception;

    Collection<String> protocolVersions();

    interface SessionFactory {
        McpStreamableServerSession createSession(String sessionId);
    }
}
