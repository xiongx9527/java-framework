package com.xiongx9527.fw.plus.ai.mcp.transport.reactive;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.mcp.server.McpAsyncServerImpl;
import com.xiongx9527.fw.plus.ai.mcp.support.McpTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring WebFlux（Reactive）Streamable HTTP 端点集成测试
 *
 * <p>与 {@link com.xiongx9527.fw.plus.ai.mcp.transport.servlet.McpServletStreamableHttpIntegrationTest}
 * 用例一一对应，验证响应式端点在真实 Netty HTTP 上下文下的协议行为一致（spec 5.3 / 9.2）。
 *
 * <p>注意：测试类路径同时存在 spring-webmvc 与 spring-webflux，Boot 默认会判定为 Servlet 应用，
 * 因此通过 {@code spring.main.web-application-type=reactive} 强制使用响应式容器。
 *
 * @author xiongxiao
 * @date 2025-09-21
 */
@SpringBootTest(classes = McpReactiveStreamableHttpIntegrationTest.TestApplication.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "spring.main.web-application-type=reactive")
public class McpReactiveStreamableHttpIntegrationTest {

	private static final String LIST_CHANGED_NOTIFICATION = "notifications/tools/list_changed";

	private static final Duration TIMEOUT = Duration.ofSeconds(10);

	private static final ParameterizedTypeReference<ServerSentEvent<String>> SSE_TYPE =
			new ParameterizedTypeReference<ServerSentEvent<String>>() {
			};

	/**
	 * 复用一个 reactor-netty 客户端：单测内多次请求共享事件循环。
	 */
	private static final HttpClient HTTP_CLIENT = HttpClient.create();

	@LocalServerPort
	private int port;

	@Autowired
	private McpAsyncServerImpl server;

	private WebClient client;

	@SpringBootConfiguration
	@EnableAutoConfiguration
	static class TestApplication {

		@Bean
		public McpReactiveStreamableTransport mcpReactiveStreamableTransport(ObjectMapper objectMapper) {
			return new McpReactiveStreamableTransport("default", McpTestSupport.PATH, 30_000L, objectMapper);
		}

		@Bean
		public McpAsyncServerImpl mcpServer(McpReactiveStreamableTransport transport) throws Exception {
			return McpTestSupport.buildServer(transport);
		}

		@Bean
		public McpReactiveEndpoint mcpReactiveEndpoint(McpReactiveStreamableTransport transport) {
			return new McpReactiveEndpoint(transport);
		}

		@Bean
		public McpReactiveEndpointRegistrar mcpReactiveEndpointRegistrar(
				RequestMappingHandlerMapping requestMappingHandlerMapping, McpReactiveEndpoint endpoint) {
			return new McpReactiveEndpointRegistrar(requestMappingHandlerMapping,
					Collections.singletonList(endpoint));
		}
	}

	@BeforeEach
	public void setUpClient() {
		// 使用 127.0.0.1 而非 localhost：reactor-netty 默认走自带 DNS 解析器，
		// 在无 DNS 的环境中无法解析 localhost，字面 IP 则不触发解析。
		this.client = WebClient.builder()
				.clientConnector(new ReactorClientHttpConnector(HTTP_CLIENT))
				.baseUrl("http://127.0.0.1:" + port)
				.build();
	}

	// ==================== 握手与会话 ====================

	@Test
	@DisplayName("initialize 返回会话 ID，并回显协商后的协议版本")
	public void shouldInitializeAndReturnSessionId() {
		ResponseEntity<String> response = post(
				McpTestSupport.rpc(1, "initialize", McpTestSupport.initializeParams(McpTestSupport.PROTOCOL_VERSION)),
				null);

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		String sessionId = response.getHeaders().getFirst(McpTestSupport.SESSION_ID_HEADER);
		assertThat(sessionId).isNotBlank();

		JsonNode result = McpTestSupport.json(response.getBody()).path("result");
		assertThat(result.path("protocolVersion").asText()).isEqualTo(McpTestSupport.PROTOCOL_VERSION);
		assertThat(result.path("serverInfo").path("name").asText()).isEqualTo("fw-plus-mcp-it");
		assertThat(result.path("capabilities").has("tools")).isTrue();

		// notifications/initialized 为通知，HTTP 202 且无响应体
		ResponseEntity<String> notification = post(McpTestSupport.rpc(null, "notifications/initialized", null),
				sessionId);
		assertThat(notification.getStatusCode().value()).isEqualTo(202);
		assertThat(notification.getBody()).isNullOrEmpty();
	}

	@Test
	@DisplayName("不支持的协议版本返回 -32600 且不创建会话")
	public void shouldRejectUnsupportedProtocolVersion() {
		ResponseEntity<String> response = post(
				McpTestSupport.rpc(1, "initialize", McpTestSupport.initializeParams("2024-11-05")), null);

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getHeaders().getFirst(McpTestSupport.SESSION_ID_HEADER)).isNull();
		assertThat(McpTestSupport.json(response.getBody()).path("error").path("code").asInt()).isEqualTo(-32600);
	}

	@Test
	@DisplayName("非 initialize 请求缺少 Mcp-Session-Id 返回 400，未知会话返回 404")
	public void shouldRejectMissingOrUnknownSession() {
		assertThat(post(McpTestSupport.rpc(1, "tools/list", null), null).getStatusCode().value()).isEqualTo(400);
		assertThat(post(McpTestSupport.rpc(1, "tools/list", null), "unknown-session").getStatusCode().value())
				.isEqualTo(404);
	}

	@Test
	@DisplayName("DELETE 结束会话后，原会话请求返回 404")
	public void shouldCloseSessionOnDelete() {
		String sessionId = initialize();

		ResponseEntity<String> deleted = exchange(HttpMethod.DELETE, sessionId);
		assertThat(deleted.getStatusCode().value()).isEqualTo(204);
		assertThat(post(McpTestSupport.rpc(1, "tools/list", null), sessionId).getStatusCode().value()).isEqualTo(404);
		assertThat(exchange(HttpMethod.DELETE, sessionId).getStatusCode().value()).isEqualTo(404);
	}

	// ==================== 工具 / 资源 / 提示词 ====================

	@Test
	@DisplayName("tools/list 与 tools/call 正常往返；工具异常以 isError 结果返回")
	public void shouldListAndCallTools() {
		String sessionId = initialize();

		JsonNode tools = McpTestSupport.json(post(McpTestSupport.rpc(1, "tools/list", null), sessionId).getBody())
				.path("result").path("tools");
		assertThat(tools.isArray()).isTrue();
		assertThat(tools.findValuesAsText("name")).contains(McpTestSupport.ECHO_TOOL, McpTestSupport.BOOM_TOOL);

		JsonNode call = McpTestSupport.json(post(
				McpTestSupport.rpc(2, "tools/call",
						McpTestSupport.params("name", McpTestSupport.ECHO_TOOL,
								"arguments", McpTestSupport.params("message", "hi"))),
				sessionId).getBody()).path("result");
		assertThat(call.path("content").get(0).path("text").asText()).isEqualTo("echo: hi");
		assertThat(call.path("isError").asBoolean()).isFalse();

		// 工具业务异常：JSON-RPC 层正常，结果标记 isError
		JsonNode failed = McpTestSupport.json(post(
				McpTestSupport.rpc(3, "tools/call",
						McpTestSupport.params("name", McpTestSupport.BOOM_TOOL,
								"arguments", McpTestSupport.params())),
				sessionId).getBody()).path("result");
		assertThat(failed.path("isError").asBoolean()).isTrue();

		// 未知工具：method not found
		JsonNode unknown = McpTestSupport.json(post(
				McpTestSupport.rpc(4, "tools/call",
						McpTestSupport.params("name", "not-exists", "arguments", McpTestSupport.params())),
				sessionId).getBody());
		assertThat(unknown.path("error").path("code").asInt()).isEqualTo(-32601);
	}

	@Test
	@DisplayName("resources/list、resources/templates/list 与 resources/read 可用（含模板匹配）")
	public void shouldExposeResources() {
		String sessionId = initialize();

		JsonNode resources = McpTestSupport.json(post(McpTestSupport.rpc(1, "resources/list", null), sessionId)
				.getBody()).path("result").path("resources");
		assertThat(resources.findValuesAsText("uri")).contains(McpTestSupport.GREETING_RESOURCE_URI);

		JsonNode templates = McpTestSupport.json(post(McpTestSupport.rpc(2, "resources/templates/list", null),
				sessionId).getBody()).path("result").path("resourceTemplates");
		assertThat(templates.findValuesAsText("uriTemplate")).contains("test://file/{name}");

		JsonNode content = McpTestSupport.json(post(
				McpTestSupport.rpc(3, "resources/read",
						McpTestSupport.params("uri", McpTestSupport.TEMPLATE_RESOURCE_URI)),
				sessionId).getBody()).path("result").path("contents").get(0);
		assertThat(content.path("uri").asText()).isEqualTo(McpTestSupport.TEMPLATE_RESOURCE_URI);
		assertThat(content.path("text").asText()).contains("resource content of");

		// 订阅/取消订阅
		assertThat(McpTestSupport.json(post(
				McpTestSupport.rpc(4, "resources/subscribe",
						McpTestSupport.params("uri", McpTestSupport.GREETING_RESOURCE_URI)),
				sessionId).getBody()).has("result")).isTrue();
		assertThat(McpTestSupport.json(post(
				McpTestSupport.rpc(5, "resources/unsubscribe",
						McpTestSupport.params("uri", McpTestSupport.GREETING_RESOURCE_URI)),
				sessionId).getBody()).has("result")).isTrue();
	}

	@Test
	@DisplayName("prompts/list 与 prompts/get 可用，缺失必填参数返回 -32602")
	public void shouldExposePrompts() {
		String sessionId = initialize();

		JsonNode prompts = McpTestSupport.json(post(McpTestSupport.rpc(1, "prompts/list", null), sessionId)
				.getBody()).path("result").path("prompts");
		assertThat(prompts.findValuesAsText("name")).contains(McpTestSupport.GREETING_PROMPT);

		JsonNode prompt = McpTestSupport.json(post(
				McpTestSupport.rpc(2, "prompts/get",
						McpTestSupport.params("name", McpTestSupport.GREETING_PROMPT,
								"arguments", McpTestSupport.params("name", "9527"))),
				sessionId).getBody()).path("result");
		assertThat(prompt.path("messages").get(0).path("content").path("text").asText()).isEqualTo("hello 9527");

		JsonNode missingArgument = McpTestSupport.json(post(
				McpTestSupport.rpc(3, "prompts/get",
						McpTestSupport.params("name", McpTestSupport.GREETING_PROMPT,
								"arguments", McpTestSupport.params())),
				sessionId).getBody());
		assertThat(missingArgument.path("error").path("code").asInt()).isEqualTo(-32602);
	}

	// ==================== 错误与安全 ====================

	@Test
	@DisplayName("非法 JSON 返回 -32700，未知方法返回 -32601")
	public void shouldReportProtocolErrors() {
		ResponseEntity<String> parseError = post("{not-a-json", null);
		assertThat(parseError.getStatusCode().value()).isEqualTo(200);
		assertThat(McpTestSupport.json(parseError.getBody()).path("error").path("code").asInt()).isEqualTo(-32700);

		String sessionId = initialize();
		JsonNode methodNotFound = McpTestSupport.json(
				post(McpTestSupport.rpc(1, "unknown/method", null), sessionId).getBody());
		assertThat(methodNotFound.path("error").path("code").asInt()).isEqualTo(-32601);
	}

	@Test
	@DisplayName("跨域 Origin 被拒绝（403）")
	public void shouldRejectInvalidOrigin() {
		ResponseEntity<String> response = client.post()
				.uri(McpTestSupport.PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.ORIGIN, "http://evil.example.com")
				.bodyValue(McpTestSupport.rpc(1, "initialize",
						McpTestSupport.initializeParams(McpTestSupport.PROTOCOL_VERSION)))
				.exchangeToMono(this::toEntity)
				.block(TIMEOUT);

		assertThat(response).isNotNull();
		assertThat(response.getStatusCode().value()).isEqualTo(403);
	}

	// ==================== SSE ====================

	@Test
	@DisplayName("GET 建立 SSE 通道并推送 tools/list_changed 通知")
	public void shouldPushNotificationOverSse() {
		String sessionId = initialize();

		// 无 SSE 通道时通知进入事件缓冲，客户端携带 Last-Event-ID 恢复即可收到
		server.notifyToolsListChanged();
		// 无会话的 GET 返回 400
		assertThat(exchange(HttpMethod.GET, null).getStatusCode().value()).isEqualTo(400);
		// 未知会话的 GET 返回 404
		assertThat(exchange(HttpMethod.GET, "unknown-session").getStatusCode().value()).isEqualTo(404);

		String received = readSseEvent(sessionId, "0", TIMEOUT);
		assertThat(received).contains(LIST_CHANGED_NOTIFICATION);
	}

	// ==================== 辅助方法 ====================

	private String initialize() {
		ResponseEntity<String> response = post(
				McpTestSupport.rpc(1, "initialize", McpTestSupport.initializeParams(McpTestSupport.PROTOCOL_VERSION)),
				null);
		String sessionId = response.getHeaders().getFirst(McpTestSupport.SESSION_ID_HEADER);
		assertThat(sessionId).isNotBlank();
		return sessionId;
	}

	private ResponseEntity<String> post(String body, String sessionId) {
		WebClient.RequestBodySpec spec = client.post()
				.uri(McpTestSupport.PATH)
				.contentType(MediaType.APPLICATION_JSON);
		if (sessionId != null) {
			spec = spec.header(McpTestSupport.SESSION_ID_HEADER, sessionId);
		}
		return spec.bodyValue(body).exchangeToMono(this::toEntity).block(TIMEOUT);
	}

	private ResponseEntity<String> exchange(HttpMethod method, String sessionId) {
		WebClient.RequestHeadersSpec<?> spec = client.method(method).uri(McpTestSupport.PATH);
		if (sessionId != null) {
			spec = spec.header(McpTestSupport.SESSION_ID_HEADER, sessionId);
		}
		return spec.exchangeToMono(this::toEntity).block(TIMEOUT);
	}

	private Mono<ResponseEntity<String>> toEntity(ClientResponse response) {
		HttpHeaders headers = new HttpHeaders();
		headers.putAll(response.headers().asHttpHeaders());
		HttpStatus status = response.statusCode();
		return response.bodyToMono(String.class)
				.defaultIfEmpty("")
				.map(body -> new ResponseEntity<>(body, headers, status));
	}

	/**
	 * 订阅 SSE 事件流，读到目标通知即返回
	 */
	private String readSseEvent(String sessionId, String lastEventId, Duration timeout) {
		StringBuilder received = new StringBuilder();
		client.get()
				.uri(McpTestSupport.PATH)
				.accept(MediaType.TEXT_EVENT_STREAM)
				.header(McpTestSupport.SESSION_ID_HEADER, sessionId)
				.header("Last-Event-ID", lastEventId)
				.exchangeToFlux(response -> {
					assertThat(response.statusCode().value()).isEqualTo(200);
					assertThat(response.headers().contentType()).isNotNull();
					assertThat(response.headers().contentType().toString())
							.contains(MediaType.TEXT_EVENT_STREAM_VALUE);
					return response.bodyToFlux(SSE_TYPE);
				})
				.map(event -> event.data() == null ? "" : event.data())
				.takeUntil(data -> data.contains(LIST_CHANGED_NOTIFICATION))
				.doOnNext(data -> received.append(data).append('\n'))
				.timeout(timeout)
				.onErrorResume(TimeoutException.class, error -> Flux.empty())
				.blockLast();
		return received.toString();
	}
}
