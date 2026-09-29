package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.web.webmvc;

import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.contracts.ContractRuntime;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.contracts.RejectAuthService;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.runtime.McpServerRuntime;
import com.xiongx9527.fw.plus.ai.mcp.server.spi.ServerSseRegistry;
import com.xiongx9527.fw.plus.ai.mcp.server.spi.ServerStreamableHttpRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MVC 栈认证入口契约：认证关闭（放行主体）进入内核；开启但 token 无效（匿名主体）
 * 在入口即被拒，绝不进入业务分发。
 */
class McpAuthEntryMvcTest {

	@Test
	void shouldEnterKernelWhenAuthDisabled() throws IOException {
		McpServerRuntime runtime = ContractRuntime.sseRuntime(true);
		McpSseController controller = new McpSseController(runtime);
		MockHttpServletResponse response = new MockHttpServletResponse();

		controller.handleMessage("{}", "known-uuid", new MockHttpServletRequest(), response);

		verify((ServerSseRegistry) runtime.getRegistry()).handle("known-uuid", "{}");
	}

	@Test
	void shouldRejectAnonymousBeforeKernelOnSse() throws IOException {
		McpServerRuntime runtime = ContractRuntime.sseRuntime(true, new RejectAuthService());
		McpSseController controller = new McpSseController(runtime);
		MockHttpServletResponse response = new MockHttpServletResponse();

		controller.handleMessage("{}", "known-uuid", new MockHttpServletRequest(), response);

		assertThat(response.getStatus()).isEqualTo(401);
		verify((ServerSseRegistry) runtime.getRegistry(), never()).handle(any(), any());
	}

	@Test
	void shouldEnterKernelOnStreamableWhenAuthDisabled() throws IOException {
		McpServerRuntime runtime = ContractRuntime.streamableRuntime(true);
		ServerStreamableHttpRegistry registry = (ServerStreamableHttpRegistry) runtime.getRegistry();
		when(registry.post(any(), anyMap())).thenReturn(com.ajaxjs.mcp.server.model.HttpResult.json(200, null));
		McpStreamableHttpController controller = new McpStreamableHttpController(runtime);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Mcp-Session-Id", "known-session");

		controller.post("{}", request, new MockHttpServletResponse());

		verify((ServerStreamableHttpRegistry) runtime.getRegistry()).post(any(), anyMap());
	}

	@Test
	void shouldRejectAnonymousBeforeKernelOnStreamable() throws IOException {
		McpServerRuntime runtime = ContractRuntime.streamableRuntime(true, new RejectAuthService());
		McpStreamableHttpController controller = new McpStreamableHttpController(runtime);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Mcp-Session-Id", "known-session");
		MockHttpServletResponse response = new MockHttpServletResponse();

		controller.post("{}", request, response);

		assertThat(response.getStatus()).isEqualTo(401);
		verify((ServerStreamableHttpRegistry) runtime.getRegistry(), never()).post(any(), anyMap());
	}
}
