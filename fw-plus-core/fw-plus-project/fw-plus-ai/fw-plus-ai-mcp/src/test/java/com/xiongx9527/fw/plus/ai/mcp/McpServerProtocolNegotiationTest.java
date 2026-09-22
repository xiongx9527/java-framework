package com.xiongx9527.fw.plus.ai.mcp;

import com.xiongx9527.fw.plus.ai.mcp.protocol.McpJsonRpcRequest;
import com.xiongx9527.fw.plus.ai.mcp.protocol.McpJsonRpcResponse;
import com.xiongx9527.fw.plus.ai.mcp.server.McpAsyncServerImpl;
import com.xiongx9527.fw.plus.ai.mcp.server.McpServerBuilder;
import com.xiongx9527.fw.plus.ai.mcp.server.McpServerCapabilities;
import com.xiongx9527.fw.plus.ai.mcp.server.McpServerInfo;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 版本协商单元测试
 *
 * <p>spec 3：服务端须接受 2025-11-25 / 2025-06-18，回显客户端请求版本；
 * 其他版本返回协议错误（不创建会话）。
 */
public class McpServerProtocolNegotiationTest {

	private static final String SESSION_ID = "test-session-1";

	private McpAsyncServerImpl buildServer() throws Exception {
		McpServerBuilder builder = new McpServerBuilder(new NoopTransportProvider());
		builder.serverInfo(new McpServerInfo("test-server", "0.0.1", "Test"));
		builder.capabilities(McpServerCapabilities.builder()
				.tools(true)
				.resources(true)
				.prompts(true)
				.completions(true)
				.logging(true)
				.build());
		McpAsyncServerImpl server = builder.build();
		server.start();
		return server;
	}

	private McpJsonRpcRequest initializeRequest(String protocolVersion) {
		McpJsonRpcRequest request = new McpJsonRpcRequest();
		request.setJsonrpc("2.0");
		request.setId("req-1");
		request.setMethod("initialize");
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("protocolVersion", protocolVersion);
		params.put("clientInfo", new LinkedHashMap<>());
		request.setParams(params);
		return request;
	}

	@Test
	public void shouldAccept2025_11_25AndEcho() throws Exception {
		McpAsyncServerImpl server = buildServer();
		try {
			McpJsonRpcResponse response = server.handleRequest(initializeRequest("2025-11-25"), SESSION_ID);
			assertThat(response).isNotNull();
			assertThat(response.getError()).isNull();
			@SuppressWarnings("unchecked")
			Map<String, Object> result = (Map<String, Object>) response.getResult();
			assertThat(result.get("protocolVersion")).isEqualTo("2025-11-25");
			assertThat(result.get("capabilities")).isInstanceOf(Map.class);
			assertThat(result.get("serverInfo")).isInstanceOf(Map.class);
		} finally {
			server.stop();
		}
	}

	@Test
	public void shouldAccept2025_06_18AndEcho() throws Exception {
		McpAsyncServerImpl server = buildServer();
		try {
			McpJsonRpcResponse response = server.handleRequest(initializeRequest("2025-06-18"), SESSION_ID);
			assertThat(response).isNotNull();
			assertThat(response.getError()).isNull();
			@SuppressWarnings("unchecked")
			Map<String, Object> result = (Map<String, Object>) response.getResult();
			assertThat(result.get("protocolVersion")).isEqualTo("2025-06-18");
		} finally {
			server.stop();
		}
	}

	@Test
	public void shouldRejectUnsupportedVersion() throws Exception {
		McpAsyncServerImpl server = buildServer();
		try {
			McpJsonRpcResponse response = server.handleRequest(initializeRequest("2024-11-05"), SESSION_ID);
			assertThat(response).isNotNull();
			assertThat(response.getError()).isNotNull();
			assertThat(response.getError().getCode()).isEqualTo(-32600);
		} finally {
			server.stop();
		}
	}

	@Test
	public void shouldHandleNotificationsInitializedAndPing() throws Exception {
		McpAsyncServerImpl server = buildServer();
		try {
			// initialize -> notifications/initialized -> ping
			server.handleRequest(initializeRequest("2025-11-25"), SESSION_ID);
			McpJsonRpcRequest init = new McpJsonRpcRequest();
			init.setJsonrpc("2.0");
			init.setId(null); // 通知：无 id
			init.setMethod("notifications/initialized");
			// 通知应返回 null 响应
			McpJsonRpcResponse initResp = server.handleRequest(init, SESSION_ID);
			assertThat(initResp).isNull();
			// ping
			McpJsonRpcRequest ping = new McpJsonRpcRequest();
			ping.setJsonrpc("2.0");
			ping.setId(7);
			ping.setMethod("ping");
			McpJsonRpcResponse pingResp = server.handleRequest(ping, SESSION_ID);
			assertThat(pingResp).isNotNull();
			assertThat(pingResp.getError()).isNull();
		} finally {
			server.stop();
		}
	}

	/**
	 * 测试用 transport provider：noop 实现
	 */
	private static class NoopTransportProvider
			implements com.xiongx9527.fw.plus.ai.mcp.transport.McpStreamableServerTransportProvider {
		@Override
		public void setSessionFactory(SessionFactory sessionFactory) {
		}

		@Override
		public McpJsonRpcResponse handlePost(String sessionId, McpJsonRpcRequest request) {
			return null;
		}

		@Override
		public Object handleGet(String sessionId) {
			return null;
		}

		@Override
		public void handleDelete(String sessionId) {
		}

		@Override
		public void notifyClients(String method, Object params) {
		}

		@Override
		public void notifyClient(String sessionId, String method, Object params) {
		}

		@Override
		public void start() {
		}

		@Override
		public void stop() {
		}

		@Override
		public java.util.Collection<String> protocolVersions() {
			return java.util.Arrays.asList("2025-11-25", "2025-06-18");
		}
	}
}
