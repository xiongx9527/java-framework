package com.xiongx9527.fw.plus.ai.mcp.transport.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.mcp.protocol.McpJsonRpcResponse;
import com.xiongx9527.fw.plus.ai.mcp.server.McpAsyncServerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * WebSocket 传输单测
 *
 * @author xiongxiao
 * @date 2026-09-22
 */
@ExtendWith(MockitoExtension.class)
public class McpWebSocketTransportTest {

	@Mock
	private McpAsyncServerImpl server;

	@Mock
	private McpWebSocketServerTransportProvider transportProvider;

	private ObjectMapper objectMapper;
	private McpWebSocketTransport transport;

	@BeforeEach
	public void setUp() throws Exception {
		objectMapper = new ObjectMapper();
		transport = new McpWebSocketTransport("test", "/mcp/ws", 1800L, objectMapper);
		transport.bindServer(server);
		transport.setTransportProvider(transportProvider);
	}

	@Test
	public void testHandleMessage_initialize() throws Exception {
		String requestBody = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}";
		McpJsonRpcResponse mockResponse = McpJsonRpcResponse.success(1, java.util.Collections.singletonMap("protocolVersion", "2025-11-25"));
		when(server.handleRequest(any(com.xiongx9527.fw.plus.ai.mcp.protocol.McpJsonRpcRequest.class), any())).thenReturn(mockResponse);

		String response = transport.handleMessage(null, requestBody);
		assertNotNull(response);
		assertTrue(response.contains("protocolVersion"));
	}

	@Test
	public void testHandleMessage_invalidJson() throws Exception {
		String requestBody = "invalid json";
		String response = transport.handleMessage(null, requestBody);
		assertNull(response);
	}

	@Test
	public void testSendNotification() throws Exception {
		transport.sendNotification("test/method", java.util.Collections.singletonMap("key", "value"));
		verify(transportProvider, timeout(1000)).sendRaw(any(String.class));
	}

	@Test
	public void testSendNotification_noProvider() throws Exception {
		transport.setTransportProvider(null);
		transport.sendNotification("test/method", java.util.Collections.singletonMap("key", "value"));
		verifyNoInteractions(transportProvider);
	}
}
