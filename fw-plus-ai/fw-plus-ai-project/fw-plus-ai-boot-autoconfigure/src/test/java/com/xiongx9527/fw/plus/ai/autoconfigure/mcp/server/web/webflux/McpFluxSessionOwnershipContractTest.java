package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.web.webflux;

import com.ajaxjs.mcp.server.model.HttpResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.contracts.ContractRuntime;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.runtime.McpServerRuntime;
import com.xiongx9527.fw.plus.ai.mcp.server.spi.ServerStreamableHttpRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WebFlux 栈会话归属契约：streamable-http GET 在缺失或未知会话头时返回 404，
 * 不得把 null 会话透传给注册表（ConcurrentHashMap 不接受 null 键）而抛出 500。
 */
class McpFluxSessionOwnershipContractTest {

	@Test
	void shouldReturn404WhenSessionHeaderMissingInsteadOfNpe() throws Exception {
		McpServerRuntime runtime = ContractRuntime.streamableRuntime(true);
		McpFluxTransportHandler handler = new McpFluxTransportHandler(runtime, Schedulers.immediate());

		ServerRequest.Headers headers = mock(ServerRequest.Headers.class);
		ServerRequest request = mock(ServerRequest.class);
		when(request.headers()).thenReturn(headers);

		ServerResponse response = invokeGet(handler, request).block();

		assertEquals(HttpStatus.NOT_FOUND, response.statusCode());
		// 真实注册表 hasSession 用 containsKey，null 会话会抛 NPE；
		// assert：缺头时必须短路，绝不触碰注册表。
		verify((ServerStreamableHttpRegistry) runtime.getRegistry(), never()).hasSession(null);
	}

	@Test
	void shouldReturn404WhenSessionUnknown() throws Exception {
		McpServerRuntime runtime = ContractRuntime.streamableRuntime(false);
		McpFluxTransportHandler handler = new McpFluxTransportHandler(runtime, Schedulers.immediate());

		ServerRequest.Headers headers = mock(ServerRequest.Headers.class);
		when(headers.firstHeader("Mcp-Session-Id")).thenReturn("unknown-session");
		ServerRequest request = mock(ServerRequest.class);
		when(request.headers()).thenReturn(headers);

		ServerResponse response = invokeGet(handler, request).block();

		assertEquals(HttpStatus.NOT_FOUND, response.statusCode());
	}

	@Test
	void shouldSurfaceKernelStatusWhenGetRejected() throws Exception {
		McpServerRuntime runtime = ContractRuntime.streamableRuntime(true);
		ServerStreamableHttpRegistry registry = (ServerStreamableHttpRegistry) runtime.getRegistry();
		// 内核拒绝（origin 不允许 / 会话已失效等）：透出真实状态码，而非伪装成空的 200 SSE
		when(registry.openEventStream(eq("known-session"), any(PrintWriter.class), anyMap()))
				.thenReturn(HttpResult.json(HttpStatus.FORBIDDEN.value(), "rejected"));
		McpFluxTransportHandler handler = new McpFluxTransportHandler(runtime, Schedulers.immediate());

		ServerResponse response = invokeGet(handler, request("known-session")).block();

		assertEquals(HttpStatus.FORBIDDEN, response.statusCode());
		// 非 200：与 MVC 一致，显式解除事件流注册，不残留无订阅者的缓冲队列
		verify(registry, times(1)).closeEventStream(eq("known-session"), any(PrintWriter.class));
	}

	@Test
	void shouldReleaseStreamRegistrationWhenOpenEventStreamThrows() throws Exception {
		McpServerRuntime runtime = ContractRuntime.streamableRuntime(true);
		ServerStreamableHttpRegistry registry = (ServerStreamableHttpRegistry) runtime.getRegistry();
		// 归属预检与内核打开之间的窗口内会话过期等异常：流可能已注册，必须解除以免内存泄漏
		when(registry.openEventStream(eq("known-session"), any(PrintWriter.class), anyMap()))
				.thenThrow(new RuntimeException("session expired"));
		McpFluxTransportHandler handler = new McpFluxTransportHandler(runtime, Schedulers.immediate());

		AtomicReference<Throwable> error = new AtomicReference<>();
		invokeGet(handler, request("known-session")).subscribe(response -> {
		}, error::set);

		assertNotNull(error.get());
		verify(registry, times(1)).closeEventStream(eq("known-session"), any(PrintWriter.class));
	}

	@Test
	void shouldOpenEventStreamWhenGetAllowed() throws Exception {
		McpServerRuntime runtime = ContractRuntime.streamableRuntime(true);
		ServerStreamableHttpRegistry registry = (ServerStreamableHttpRegistry) runtime.getRegistry();
		when(registry.openEventStream(eq("known-session"), any(PrintWriter.class), anyMap()))
				.thenReturn(HttpResult.json(HttpStatus.OK.value(), null));
		McpFluxTransportHandler handler = new McpFluxTransportHandler(runtime, Schedulers.immediate());

		Mono<ServerResponse> mono = invokeGet(handler, request("known-session"));
		AtomicReference<ServerResponse> captured = new AtomicReference<>();
		AtomicReference<Throwable> error = new AtomicReference<>();
		mono.subscribe(captured::set, error::set);

		assertNull(error.get());
		ServerResponse response = captured.get();
		assertNotNull(response);
		assertEquals(HttpStatus.OK, response.statusCode());
		assertEquals(MediaType.TEXT_EVENT_STREAM, response.headers().getContentType());
	}

	private ServerRequest request(String session) {
		ServerRequest.Headers headers = mock(ServerRequest.Headers.class);
		when(headers.firstHeader("Mcp-Session-Id")).thenReturn(session);
		when(headers.asHttpHeaders()).thenReturn(new HttpHeaders());
		ServerRequest request = mock(ServerRequest.class);
		when(request.headers()).thenReturn(headers);
		return request;
	}

	private Mono<ServerResponse> invokeGet(McpFluxTransportHandler handler, ServerRequest request) throws Exception {
		Method method = McpFluxTransportHandler.class.getDeclaredMethod("get", ServerRequest.class);
		method.setAccessible(true);
		@SuppressWarnings("unchecked")
		Mono<ServerResponse> result = (Mono<ServerResponse>) method.invoke(handler, request);
		return result;
	}
}
