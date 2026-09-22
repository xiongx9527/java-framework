package com.xiongx9527.fw.plus.ai.mcp.transport.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.mcp.transport.websocket.McpWebSocketServerTransportProvider;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

/**
 * Spring MVC WebSocket endpoint for MCP JSON-RPC transport.
 *
 * <p>Delegates all protocol handling to {@link McpWebSocketTransport};
 * this class only bridges the Spring WebSocket session lifecycle to the
 * transport layer via {@link McpWebSocketServerTransportProvider}.
 *
 * @author xiongxiao
 * @date 2026-09-22
 */
public class McpWebSocketEndpoint extends AbstractWebSocketHandler {

    private final McpWebSocketTransport transport;
    private final ObjectMapper mapper = new ObjectMapper();

    public McpWebSocketEndpoint(McpWebSocketTransport transport) {
        this.transport = transport;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        transport.setTransportProvider(new SessionProvider(session, mapper));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String body = message.getPayload();
        com.xiongx9527.fw.plus.ai.mcp.protocol.McpJsonRpcRequest request = transport.parseRequest(body);
        if (request == null) {
            session.sendMessage(new TextMessage(
                    "{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32700,\"message\":\"Parse error\"}}"));
            return;
        }
        String responseJson = transport.handleMessage(null, body);
        if (responseJson != null && session.isOpen()) {
            session.sendMessage(new TextMessage(responseJson));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        transport.setTransportProvider(null);
    }

    /**
     * Adapter from {@link WebSocketSession} to {@link McpWebSocketServerTransportProvider}.
     */
    static class SessionProvider implements McpWebSocketServerTransportProvider {

        private final WebSocketSession session;
        private final ObjectMapper mapper;

        SessionProvider(WebSocketSession session, ObjectMapper mapper) {
            this.session = session;
            this.mapper = mapper;
        }

        @Override
        public void sendResponse(com.xiongx9527.fw.plus.ai.mcp.protocol.McpJsonRpcResponse response) throws Exception {
            if (session.isOpen()) {
                String json = mapper.writeValueAsString(response);
                session.sendMessage(new TextMessage(json));
            }
        }

        @Override
        public void sendRaw(String json) throws Exception {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(json));
            }
        }

        @Override
        public void close() throws Exception {
            if (session.isOpen()) {
                session.close();
            }
        }
    }
}
