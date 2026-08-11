package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.official;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.TinyEngineAiProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TinyEngineOfficialAiControllerTest {

	private static final String BASE64_SM4_KEY = "ASNFZ4mrze/+3LqYdlQyEA==";
	private final ObjectMapper objectMapper = new ObjectMapper();
	private HttpServer upstream;

	@AfterEach
	public void stopUpstream() {
		if (upstream != null) {
			upstream.stop(0);
		}
	}

	@Test
	public void shouldProxyNonStreamingOpenAiCompatibleRequestWithoutSensitiveControlFields() throws Exception {
		AtomicReference<String> upstreamAuthorization = new AtomicReference<String>();
		AtomicReference<String> upstreamBody = new AtomicReference<String>();
		upstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		upstream.createContext("/v1/chat/completions", new JsonHandler(upstreamAuthorization, upstreamBody, 200,
				"{\"id\":\"chatcmpl-test\",\"object\":\"chat.completion\"}"));
		upstream.start();
		MockMvc mvc = officialMvc();

		mvc.perform(post("/app-center/api/chat/completions")
						.header("Authorization", "Bearer header-key")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{"
								+ "\"baseUrl\":\"http://127.0.0.1:" + upstream.getAddress().getPort() + "\","
								+ "\"apiKey\":\"body-key\","
								+ "\"model\":\"gpt-test\","
								+ "\"stream\":false,"
								+ "\"response_format\":{\"type\":\"json_object\"},"
								+ "\"tools\":[{\"type\":\"function\"}],"
								+ "\"thinking_budget\":128,"
								+ "\"x_unknown\":\"kept\""
								+ "}"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id").value("chatcmpl-test"));

		assertEquals("Bearer body-key", upstreamAuthorization.get());
		JsonNode forwarded = objectMapper.readTree(upstreamBody.get());
		assertFalse(forwarded.has("baseUrl"));
		assertFalse(forwarded.has("apiKey"));
		assertEquals("gpt-test", forwarded.path("model").asText());
		assertEquals("json_object", forwarded.path("response_format").path("type").asText());
		assertEquals("function", forwarded.path("tools").get(0).path("type").asText());
		assertEquals(128, forwarded.path("thinking_budget").asInt());
		assertEquals("kept", forwarded.path("x_unknown").asText());
	}

	@Test
	public void shouldEncryptApiKeyWithOfficialAiTokenShape() throws Exception {
		MockMvc mvc = officialMvc();

		String body = mvc.perform(post("/app-center/api/encrypt-key")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"apiKey\":\"secret-value\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.token").exists())
				.andReturn().getResponse().getContentAsString();

		String token = objectMapper.readTree(body).path("data").path("token").asText();
		assertTrue(token.startsWith("EKEY_"));
		assertEquals("secret-value", new Sm4TinyEngineAiCredentialCipher(BASE64_SM4_KEY).decrypt(token));
	}

	@Test
	public void shouldForwardUpstreamErrorStatusBodyAndContentType() throws Exception {
		AtomicInteger index = new AtomicInteger();
		int[] upstreamStatuses = new int[] {401, 429, 500};
		int[] clientStatuses = new int[] {502, 429, 500};
		String[] responses = new String[] {
				"{\"error\":{\"message\":\"invalid key\"}}",
				"{\"error\":{\"message\":\"rate limited\"}}",
				"{\"error\":{\"message\":\"server failed\"}}"
		};
		upstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		upstream.createContext("/v1/chat/completions", exchange -> {
			int current = index.getAndIncrement();
			readAll(exchange.getRequestBody());
			byte[] responseBytes = responses[current].getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(upstreamStatuses[current], responseBytes.length);
			exchange.getResponseBody().write(responseBytes);
			exchange.close();
		});
		upstream.start();
		MockMvc mvc = officialMvc();

		for (int i = 0; i < clientStatuses.length; i++) {
			mvc.perform(post("/app-center/api/ai/chat")
							.contentType(MediaType.APPLICATION_JSON)
							.content(validChatBody("http://127.0.0.1:" + upstream.getAddress().getPort(), "secret-key")))
					.andExpect(status().is(clientStatuses[i]))
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andExpect(content().json(responses[i]));
		}
	}

	@Test
	public void shouldMapConnectionFailureToBadGatewayWithoutLeakingCredentials() throws Exception {
		MockMvc mvc = officialMvc();

		String body = mvc.perform(post("/app-center/api/chat/completions")
						.header("Authorization", "Bearer header-secret")
						.contentType(MediaType.APPLICATION_JSON)
						.content(validChatBody("http://127.0.0.1:" + unusedPort(), "body-secret")))
				.andExpect(status().isBadGateway())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.code").value("502"))
				.andReturn().getResponse().getContentAsString();

		assertFalse(body.contains("header-secret"));
		assertFalse(body.contains("body-secret"));
		assertFalse(body.contains("Authorization"));
	}

	@Test
	public void shouldMapReadTimeoutToGatewayTimeoutWithoutLeakingCredentials() throws Exception {
		upstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		upstream.createContext("/v1/chat/completions", exchange -> {
			readAll(exchange.getRequestBody());
			try {
				Thread.sleep(500L);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			byte[] responseBytes = "{\"id\":\"late\"}".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, responseBytes.length);
			exchange.getResponseBody().write(responseBytes);
			exchange.close();
		});
		upstream.start();
		TinyEngineAiProperties properties = new TinyEngineAiProperties();
		properties.getOfficial().setSm4Key(BASE64_SM4_KEY);
		properties.getOfficial().setReadTimeoutMillis(100);
		MockMvc mvc = officialMvc(properties);

		String encryptedKey = new Sm4TinyEngineAiCredentialCipher(BASE64_SM4_KEY).encrypt("timeout-secret");
		String body = mvc.perform(post("/app-center/api/chat/completions")
						.contentType(MediaType.APPLICATION_JSON)
						.content(validChatBody("http://127.0.0.1:" + upstream.getAddress().getPort(), encryptedKey)))
				.andExpect(status().isGatewayTimeout())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.code").value("504"))
				.andReturn().getResponse().getContentAsString();

		assertFalse(body.contains("timeout-secret"));
		assertFalse(body.contains(encryptedKey));
	}

	@Test
	public void shouldRejectInvalidUrlMissingModelAndMissingKeyAsClientErrorsWithoutLeakingCredentials() throws Exception {
		MockMvc mvc = officialMvc();

		String invalidUrlBody = mvc.perform(post("/app-center/api/chat/completions")
						.header("Authorization", "Bearer invalid-url-secret")
						.contentType(MediaType.APPLICATION_JSON)
						.content(validChatBody("http://bad host", "body-secret")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("400"))
				.andReturn().getResponse().getContentAsString();
		assertFalse(invalidUrlBody.contains("invalid-url-secret"));
		assertFalse(invalidUrlBody.contains("body-secret"));

		String missingModelBody = mvc.perform(post("/app-center/api/chat/completions")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"baseUrl\":\"http://127.0.0.1\",\"apiKey\":\"model-secret\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("400"))
				.andReturn().getResponse().getContentAsString();
		assertFalse(missingModelBody.contains("model-secret"));

		String missingKeyBody = mvc.perform(post("/app-center/api/chat/completions")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"baseUrl\":\"http://127.0.0.1\",\"model\":\"gpt-test\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("400"))
				.andReturn().getResponse().getContentAsString();
		assertFalse(missingKeyBody.contains("Bearer"));
		assertFalse(missingKeyBody.contains("apiKey\":\""));
	}

	@Test
	public void shouldRejectInvalidEncryptedApiKeyWithoutLeakingCiphertext() throws Exception {
		MockMvc mvc = officialMvc();
		String encryptedKey = "EKEY_invalid-ciphertext";

		String body = mvc.perform(post("/app-center/api/chat/completions")
						.contentType(MediaType.APPLICATION_JSON)
						.content(validChatBody("http://127.0.0.1", encryptedKey)))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.code").value("400"))
				.andReturn().getResponse().getContentAsString();

		assertFalse(body.contains(encryptedKey));
		assertFalse(body.contains("invalid-ciphertext"));
	}

	@Test
	public void shouldUseStreamingResponseWhenStreamFlagIsInRequestBody() throws Exception {
		TinyEngineOpenAiCompatibleGateway gateway = new TinyEngineOpenAiCompatibleGateway() {
			@Override
			public TinyEngineOfficialAiResponse execute(TinyEngineOfficialAiRequest request) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void stream(TinyEngineOfficialAiRequest request, TinyEngineSseConsumer consumer) {
				consumer.accept("data: {\"choices\":[{\"delta\":{\"content\":\"A\"}}]}\n\n"
						.getBytes(StandardCharsets.UTF_8));
			}
		};
		TinyEngineOfficialAiController controller = new TinyEngineOfficialAiController(gateway,
				Sm4TinyEngineAiCredentialCipher.from(new TinyEngineAiProperties().getOfficial()));
		Map<String, Object> request = new LinkedHashMap<String, Object>();
		request.put("baseUrl", "http://example.test");
		request.put("apiKey", "plain-key");
		request.put("model", "gpt-test");
		request.put("stream", true);

		ResponseEntity<?> response = controller.chat(request, null);

		assertEquals(MediaType.TEXT_EVENT_STREAM, response.getHeaders().getContentType());
		assertEquals("no-cache", response.getHeaders().getCacheControl());
		assertEquals("no", response.getHeaders().getFirst("X-Accel-Buffering"));
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		((org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody) response.getBody())
				.writeTo(outputStream);
		assertEquals("data: {\"choices\":[{\"delta\":{\"content\":\"A\"}}]}\n\n",
				new String(outputStream.toByteArray(), StandardCharsets.UTF_8));
	}

	private MockMvc officialMvc() {
		TinyEngineAiProperties properties = new TinyEngineAiProperties();
		properties.getOfficial().setSm4Key(BASE64_SM4_KEY);
		return officialMvc(properties);
	}

	private MockMvc officialMvc(TinyEngineAiProperties properties) {
		TinyEngineAiCredentialCipher cipher = Sm4TinyEngineAiCredentialCipher.from(properties.getOfficial());
		TinyEngineOpenAiCompatibleGateway gateway = new DefaultTinyEngineOpenAiCompatibleGateway(properties, objectMapper);
		return MockMvcBuilders.standaloneSetup(new TinyEngineOfficialAiController(gateway, cipher)).build();
	}

	private static String validChatBody(String baseUrl, String apiKey) {
		return "{"
				+ "\"baseUrl\":\"" + baseUrl + "\","
				+ "\"apiKey\":\"" + apiKey + "\","
				+ "\"model\":\"gpt-test\","
				+ "\"messages\":[{\"role\":\"user\",\"content\":\"hello\"}]"
				+ "}";
	}

	private static int unusedPort() throws IOException {
		ServerSocket socket = new ServerSocket(0);
		try {
			return socket.getLocalPort();
		} finally {
			socket.close();
		}
	}

	private static final class JsonHandler implements HttpHandler {
		private final AtomicReference<String> authorization;
		private final AtomicReference<String> body;
		private final int status;
		private final String response;

		private JsonHandler(AtomicReference<String> authorization, AtomicReference<String> body, int status,
				String response) {
			this.authorization = authorization;
			this.body = body;
			this.status = status;
			this.response = response;
		}

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
			body.set(readAll(exchange.getRequestBody()));
			byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(status, responseBytes.length);
			exchange.getResponseBody().write(responseBytes);
			exchange.close();
		}
	}

	private static String readAll(InputStream inputStream) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int read;
		while ((read = inputStream.read(buffer)) != -1) {
			outputStream.write(buffer, 0, read);
		}
		return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
	}
}
