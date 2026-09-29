package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.web.webflux;

import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.contracts.ContractRuntime;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.contracts.RejectAuthService;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.runtime.McpServerRuntime;
import com.xiongx9527.fw.plus.ai.mcp.server.spi.ServerSseRegistry;
import com.xiongx9527.fw.plus.ai.mcp.server.spi.ServerStreamableHttpRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WebFlux 栈认证入口契约：与 MVC 一致——放行主体进入内核，匿名主体在入口即被拒。
 */
class McpAuthEntryFluxTest {

	@Test
	void shouldRejectAnonymousBeforeKernelOnPost() throws Exception {
		McpServerRuntime runtime = ContractRuntime.streamableRuntime(true, new RejectAuthService());
		McpFluxTransportHandler handler = new McpFluxTransportHandler(runtime, Schedulers.immediate());

		ServerResponse response = invoke(handler, "post", request("known-session")).block();

		assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode());
		verify((ServerStreamableHttpRegistry) runtime.getRegistry(), never()).post(any(), anyMap());
	}

	@Test
	void shouldRejectAnonymousBeforeKernelOnSseMessage() throws Exception {
		McpServerRuntime runtime = ContractRuntime.sseRuntime(true, new RejectAuthService());
		McpFluxTransportHandler handler = new McpFluxTransportHandler(runtime, Schedulers.immediate());

		ServerResponse response = invoke(handler, "handleMessage", messageRequest("known-uuid")).block();

		assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode());
		verify((ServerSseRegistry) runtime.getRegistry(), never()).handle(any(), any());
	}

	@Test
	void shouldEnterKernelOnPostWhenAuthDisabled() throws Exception {
		McpServerRuntime runtime = ContractRuntime.streamableRuntime(true);
		McpFluxTransportHandler handler = new McpFluxTransportHandler(runtime, Schedulers.immediate());

		invoke(handler, "post", request("known-session")).block();

		verify((ServerStreamableHttpRegistry) runtime.getRegistry()).post(any(), anyMap());
	}

	private ServerRequest request(String session) {
		ServerRequest.Headers headers = headers(session);
		ServerRequest request = mock(ServerRequest.class);
		when(request.headers()).thenReturn(headers);
		when(request.bodyToMono(String.class)).thenReturn(Mono.just("{}"));
		return request;
	}

	private ServerRequest messageRequest(String uuid) {
		ServerRequest.Headers headers = headers(null);
		ServerRequest request = mock(ServerRequest.class);
		when(request.headers()).thenReturn(headers);
		when(request.pathVariable("uuid")).thenReturn(uuid);
		when(request.bodyToMono(String.class)).thenReturn(Mono.just("{}"));
		return request;
	}

	private ServerRequest.Headers headers(String session) {
		ServerRequest.Headers headers = mock(ServerRequest.Headers.class);
		when(headers.firstHeader("Mcp-Session-Id")).thenReturn(session);
		when(headers.firstHeader("Origin")).thenReturn(null);
		when(headers.asHttpHeaders()).thenReturn(new HttpHeaders());
		return headers;
	}

	private Mono<ServerResponse> invoke(McpFluxTransportHandler handler, String methodName, ServerRequest request)
			throws Exception {
		Method method = McpFluxTransportHandler.class.getDeclaredMethod(methodName, ServerRequest.class);
		method.setAccessible(true);
		@SuppressWarnings("unchecked")
		Mono<ServerResponse> result = (Mono<ServerResponse>) method.invoke(handler, request);
		return result;
	}
}
