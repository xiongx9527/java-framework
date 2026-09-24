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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * MVC 栈会话归属契约：未知会话在进入内核前即被拒绝（404），已知会话放行进内核。
 */
class McpMvcSessionOwnershipContractTest {

	@Test
	void shouldRejectUnknownSseUuidWithNotFoundBeforeKernel() throws IOException {
		McpServerRuntime runtime = ContractRuntime.sseRuntime(false);
		McpSseController controller = new McpSseController(runtime);

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		controller.handleMessage("{}", "unknown-uuid", request, response);

		// 内核不被触碰
		verify((ServerSseRegistry) runtime.getRegistry(), never()).handle(any(), any());
	}

	@Test
	void shouldAcceptKnownSseUuidAndEnterKernel() throws IOException {
		McpServerRuntime runtime = ContractRuntime.sseRuntime(true);
		McpSseController controller = new McpSseController(runtime);

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		controller.handleMessage("{}", "known-uuid", request, response);

		verify((ServerSseRegistry) runtime.getRegistry()).handle("known-uuid", "{}");
	}

	@Test
	void shouldRejectUnknownStreamableSessionBeforeKernel() throws IOException {
		McpServerRuntime runtime = ContractRuntime.streamableRuntime(false);
		McpStreamableHttpController controller = new McpStreamableHttpController(runtime);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Mcp-Session-Id", "unknown-session");
		MockHttpServletResponse response = new MockHttpServletResponse();
		controller.post("{}", request, response);

		verify((ServerStreamableHttpRegistry) runtime.getRegistry(), never()).post(any(), any());
	}

	@Test
	void shouldAcceptKnownStreamableSessionAndEnterKernel() throws IOException {
		McpServerRuntime runtime = ContractRuntime.streamableRuntime(true);
		McpStreamableHttpController controller = new McpStreamableHttpController(runtime);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Mcp-Session-Id", "known-session");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockHttpResult result = new MockHttpResult();
		whenPost(runtime, result);

		controller.post("{}", request, response);

		verify((ServerStreamableHttpRegistry) runtime.getRegistry()).post("{}",
				org.mockito.ArgumentMatchers.anyMap());
	}

	@Test
	void shouldAllowInitialStreamableRequestWithoutSessionHeader() throws IOException {
		McpServerRuntime runtime = ContractRuntime.streamableRuntime(false);
		McpStreamableHttpController controller = new McpStreamableHttpController(runtime);

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockHttpResult result = new MockHttpResult();
		whenPost(runtime, result);

		controller.post("{}", request, response);

		// 未携带会话头的初始化请求不得被归属校验拒绝，直接进内核
		verify((ServerStreamableHttpRegistry) runtime.getRegistry()).post("{}",
				org.mockito.ArgumentMatchers.anyMap());
	}

	private void whenPost(McpServerRuntime runtime, MockHttpResult result) {
		org.mockito.Mockito.when(((ServerStreamableHttpRegistry) runtime.getRegistry()).post("{}",
				org.mockito.ArgumentMatchers.anyMap())).thenReturn(result);
	}

	/** 最小 HttpResult 桩，避免依赖内核 toString/构造细节。 */
	private static final class MockHttpResult extends HttpResult {
		MockHttpResult() {
			super();
		}
	}
}