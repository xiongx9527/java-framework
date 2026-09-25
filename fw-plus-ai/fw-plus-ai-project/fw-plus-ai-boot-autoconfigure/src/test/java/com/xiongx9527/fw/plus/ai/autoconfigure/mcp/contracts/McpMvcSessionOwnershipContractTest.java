package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.contracts;

import com.ajaxjs.mcp.server.model.HttpResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.runtime.McpServerRuntime;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.web.McpSseController;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.web.McpStreamableHttpController;
import com.xiongx9527.fw.plus.ai.mcp.spi.ServerSseRegistry;
import com.xiongx9527.fw.plus.ai.mcp.spi.ServerStreamableHttpRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MVC 栈会话归属契约：未知会话在进入内核前即被拒绝（404），已知会话放行进内核。
 */
class McpMvcSessionOwnershipContractTest {

	@Test
	void shouldRejectUnknownSseUuidBeforeKernel() throws IOException {
		McpServerRuntime runtime = ContractRuntime.sseRuntime(false);
		McpSseController controller = new McpSseController(runtime);

		MockHttpServletResponse response = new MockHttpServletResponse();
		controller.handleMessage("{}", "unknown-uuid", new MockHttpServletRequest(), response);

		// 内核不被触碰
		verify((ServerSseRegistry) runtime.getRegistry(), never()).handle(any(), any());
	}

	@Test
	void shouldAcceptKnownSseUuidAndEnterKernel() throws IOException {
		McpServerRuntime runtime = ContractRuntime.sseRuntime(true);
		McpSseController controller = new McpSseController(runtime);

		MockHttpServletResponse response = new MockHttpServletResponse();
		controller.handleMessage("{}", "known-uuid", new MockHttpServletRequest(), response);

		verify((ServerSseRegistry) runtime.getRegistry()).handle("known-uuid", "{}");
	}

	@Test
	void shouldRejectUnknownStreamableSessionBeforeKernel() throws IOException {
		McpServerRuntime runtime = ContractRuntime.streamableRuntime(false);
		McpStreamableHttpController controller = new McpStreamableHttpController(runtime);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Mcp-Session-Id", "unknown-session");
		controller.post("{}", request, new MockHttpServletResponse());

		verify((ServerStreamableHttpRegistry) runtime.getRegistry(), never()).post(any(), anyMap());
	}

	@Test
	void shouldAcceptKnownStreamableSessionAndEnterKernel() throws IOException {
		McpServerRuntime runtime = ContractRuntime.streamableRuntime(true);
		McpStreamableHttpController controller = new McpStreamableHttpController(runtime);
		whenPost(runtime);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Mcp-Session-Id", "known-session");
		controller.post("{}", request, new MockHttpServletResponse());

		verify((ServerStreamableHttpRegistry) runtime.getRegistry()).post(eq("{}"), anyMap());
	}

	@Test
	void shouldAllowInitialStreamableRequestWithoutSessionHeader() throws IOException {
		McpServerRuntime runtime = ContractRuntime.streamableRuntime(false);
		McpStreamableHttpController controller = new McpStreamableHttpController(runtime);
		whenPost(runtime);

		// 未携带会话头的初始化请求不得被归属校验拒绝，直接进内核
		controller.post("{}", new MockHttpServletRequest(), new MockHttpServletResponse());

		verify((ServerStreamableHttpRegistry) runtime.getRegistry()).post(eq("{}"), anyMap());
	}

	private void whenPost(McpServerRuntime runtime) {
		when(((ServerStreamableHttpRegistry) runtime.getRegistry()).post(eq("{}"), anyMap()))
				.thenReturn(HttpResult.json(200, "{}"));
	}
}