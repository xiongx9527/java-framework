package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageJsonCodec;
import dev.langchain4j.data.message.GsonChatMessageJsonCodec;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link Mem0ChatMemoryStore} 对 mem0 兼容 REST 数据面的读写行为：
 * 以 JDK {@code com.sun.net.httpserver} 起本地 mock，模拟
 * {@code GET/POST /memories}、{@code PUT/DELETE /memories/{id}}（无尾斜杠，实测契约）
 * 与 mem0 的精确哈希去重（同范围同文本不新建）。
 */
class Mem0ChatMemoryStoreTest {

	private static final Gson GSON = new Gson();

	private static final ChatMessageJsonCodec CODEC = new GsonChatMessageJsonCodec();

	private static final String MEM_ID = "t1:crm:bot:u1";

	private HttpServer server;

	private String baseUrl;

	/** id → 记忆条目（含 memory / scope / metadata） */
	private final Map<String, JsonObject> items = new LinkedHashMap<>();

	private final List<String> addBodies = new ArrayList<>();

	private final List<String> getQueries = new ArrayList<>();

	private final List<String> apiKeyHeaders = new ArrayList<>();

	private final List<String> authorizationHeaders = new ArrayList<>();

	private final AtomicInteger putCount = new AtomicInteger();

	private final AtomicInteger deleteCount = new AtomicInteger();

	private final AtomicInteger idSequence = new AtomicInteger();

	private volatile boolean failing;

	@BeforeEach
	void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", this::handle);
		server.start();
		baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
	}

	@AfterEach
	void stopServer() {
		server.stop(0);
	}

	// -------------------------------------------------- 场景测试

	@Test
	void writeThenReadRoundTripPreservesOrderAndContent() {
		Mem0ChatMemoryStore store = newStore();
		List<ChatMessage> messages = Arrays.asList(
				SystemMessage.from("你是助手"),
				UserMessage.from("杭州天气怎么样"),
				AiMessage.from("杭州今天晴，25 度"));

		store.updateMessages(MEM_ID, messages);

		assertThat(jsonOf(store.getMessages(MEM_ID))).isEqualTo(jsonOf(messages));
	}

	@Test
	void readQueryFiltersByScope() throws Exception {
		Mem0ChatMemoryStore store = newStore();
		store.updateMessages(MEM_ID, Arrays.asList(UserMessage.from("你好")));

		String encodedRunId = java.net.URLEncoder.encode(MEM_ID, "UTF-8");
		assertThat(getQueries.get(0))
				.contains("user_id=u1")
				.contains("agent_id=bot")
				.contains("run_id=" + encodedRunId)
				// 服务端不支持 limit/offset，以 top_k 单次拉取
				.contains("top_k=" + 1000)
				.doesNotContain("limit=")
				.doesNotContain("offset=");
	}

	@Test
	void apiKeySentAsXApiKeyHeaderOnly() {
		// 实测：仅 X-API-Key 生效，Bearer/双头均 401 —— 只发 X-API-Key
		Mem0ChatMemoryStore store = new Mem0ChatMemoryStore(() -> "t", "app", baseUrl, "secret-1",
				Duration.ofSeconds(2), Duration.ofSeconds(5));
		store.updateMessages(MEM_ID, Arrays.asList(UserMessage.from("你好")));

		assertThat(apiKeyHeaders).isNotEmpty().containsOnly("secret-1");
		assertThat(authorizationHeaders).containsOnly((String) null);
	}

	@Test
	void addBodyCarriesFourScopesAndMetadata() {
		Mem0ChatMemoryStore store = newStore();
		store.updateMessages(MEM_ID, Arrays.asList(UserMessage.from("你好")));

		JsonObject body = JsonParser.parseString(addBodies.get(0)).getAsJsonObject();
		assertThat(body.get("user_id").getAsString()).isEqualTo("u1");
		assertThat(body.get("agent_id").getAsString()).isEqualTo("bot");
		assertThat(body.get("app_id").getAsString()).isEqualTo("crm");
		assertThat(body.get("run_id").getAsString()).isEqualTo(MEM_ID);
		assertThat(body.get("infer").getAsBoolean()).isFalse();
		JsonObject metadata = body.getAsJsonObject("metadata");
		assertThat(metadata.get("tenant_id").getAsString()).isEqualTo("t1");
		assertThat(metadata.get("app_id").getAsString()).isEqualTo("crm");
		assertThat(metadata.get("message_seq").getAsInt()).isEqualTo(0);
		assertThat(metadata.get("message_type").getAsString()).isEqualTo("USER");
	}

	@Test
	void consecutiveDuplicateMessagesSurviveDedup() {
		// mock 端按精确文本去重：seq 编入正文保证同会话内文本唯一，重复消息不被吞
		Mem0ChatMemoryStore store = newStore();
		List<ChatMessage> messages = Arrays.asList(
				UserMessage.from("好的"),
				UserMessage.from("好的"),
				AiMessage.from("收到"),
				UserMessage.from("好的"));

		store.updateMessages(MEM_ID, messages);

		assertThat(jsonOf(store.getMessages(MEM_ID))).isEqualTo(jsonOf(messages));
	}

	@Test
	void overwriteUpdatesInPlaceAndKeepsHistory() {
		Mem0ChatMemoryStore store = newStore();
		store.updateMessages(MEM_ID, Arrays.asList(
				UserMessage.from("第一条"),
				AiMessage.from("第二条"),
				UserMessage.from("第三条"),
				AiMessage.from("第四条")));
		int putsBefore = putCount.get();
		int deletesBefore = deleteCount.get();

		// 窗口收缩为 2 条，且第 0 条内容变化
		store.updateMessages(MEM_ID, Arrays.asList(
				UserMessage.from("第一条（改）"),
				AiMessage.from("第二条")));

		assertThat(putCount.get()).isGreaterThan(putsBefore);              // 原位 PUT 更新
		assertThat(deleteCount.get()).isEqualTo(deletesBefore);            // append-only：超窗历史不删
		// 读回 = 覆盖后的窗口 + 保留的超窗历史（全量历史库语义）
		assertThat(jsonOf(store.getMessages(MEM_ID))).isEqualTo(jsonOf(Arrays.asList(
				UserMessage.from("第一条（改）"),
				AiMessage.from("第二条"),
				UserMessage.from("第三条"),
				AiMessage.from("第四条"))));
	}

	@Test
	void unchangedMessagesDoNotWrite() {
		Mem0ChatMemoryStore store = newStore();
		List<ChatMessage> messages = Arrays.asList(UserMessage.from("你好"), AiMessage.from("嗨"));
		store.updateMessages(MEM_ID, messages);
		int putsBefore = putCount.get();
		int addsBefore = addBodies.size();

		store.updateMessages(MEM_ID, messages);

		assertThat(putCount.get()).isEqualTo(putsBefore);
		assertThat(addBodies.size()).isEqualTo(addsBefore);
	}

	@Test
	void deleteMessagesRemovesAll() {
		Mem0ChatMemoryStore store = newStore();
		store.updateMessages(MEM_ID, Arrays.asList(UserMessage.from("你好"), AiMessage.from("嗨")));

		store.deleteMessages(MEM_ID);

		assertThat(store.getMessages(MEM_ID)).isEmpty();
		assertThat(items).isEmpty();
	}

	@Test
	void shortMemoryIdFallsBackToDefaults() {
		// 段数<4：user/agent 回退常量 default、app 回退配置、租户回退解析器
		Mem0ChatMemoryStore store = newStore();
		store.updateMessages("plain-conv", Arrays.asList(UserMessage.from("你好")));

		JsonObject body = JsonParser.parseString(addBodies.get(0)).getAsJsonObject();
		assertThat(body.get("user_id").getAsString()).isEqualTo("default");
		assertThat(body.get("agent_id").getAsString()).isEqualTo("default");
		assertThat(body.get("app_id").getAsString()).isEqualTo("app-cfg");
		assertThat(body.getAsJsonObject("metadata").get("tenant_id").getAsString()).isEqualTo("resolver-t");
		assertThat(getQueries.get(0))
				.contains("user_id=default")
				.contains("agent_id=default")
				.contains("run_id=plain-conv");
	}

	@Test
	void httpFailureFailsFast() {
		Mem0ChatMemoryStore store = newStore();
		failing = true;

		assertThatThrownBy(() -> store.getMessages(MEM_ID))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("mem0 请求失败");
	}

	@Test
	void invalidBaseUrlRejected() {
		assertThatThrownBy(() -> new Mem0ChatMemoryStore(() -> "t", "app", " ", "", Duration.ofSeconds(1),
				Duration.ofSeconds(1))).isInstanceOf(IllegalArgumentException.class);
	}

	// -------------------------------------------------- 基础设施

	private Mem0ChatMemoryStore newStore() {
		return new Mem0ChatMemoryStore(() -> "resolver-t", "app-cfg", baseUrl, "",
				Duration.ofSeconds(2), Duration.ofSeconds(5));
	}

	private static String jsonOf(List<ChatMessage> messages) {
		return CODEC.messagesToJson(messages);
	}

	private void handle(HttpExchange exchange) throws IOException {
		try {
			if (failing) {
				respond(exchange, 500, "{\"error\":\"boom\"}");
				return;
			}
			String path = exchange.getRequestURI().getPath();
			String method = exchange.getRequestMethod();
			apiKeyHeaders.add(exchange.getRequestHeaders().getFirst("X-API-Key"));
			authorizationHeaders.add(exchange.getRequestHeaders().getFirst("Authorization"));
			if ("GET".equals(method) && "/memories".equals(path)) {
				handleGet(exchange);
			} else if ("POST".equals(method) && "/memories".equals(path)) {
				handlePost(exchange);
			} else if ("PUT".equals(method) && path.matches("/memories/[^/]+")) {
				handlePut(exchange, path);
			} else if ("DELETE".equals(method) && path.matches("/memories/[^/]+")) {
				handleDelete(exchange, path);
			} else {
				respond(exchange, 404, "{}");
			}
		} catch (Exception e) {
			respond(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
		}
	}

	private void handleGet(HttpExchange exchange) throws IOException {
		String query = exchange.getRequestURI().getRawQuery();
		getQueries.add(query == null ? "" : query);
		Map<String, String> params = parseQuery(query);
		JsonArray array = new JsonArray();
		for (JsonObject item : items.values()) {
			if (scopeMatches(item, params)) {
				array.add(item);
			}
		}
		respond(exchange, 200, GSON.toJson(array));
	}

	private void handlePost(HttpExchange exchange) throws IOException {
		String body = readBody(exchange);
		addBodies.add(body);
		JsonObject root = JsonParser.parseString(body).getAsJsonObject();
		String userId = root.get("user_id").getAsString();
		String agentId = root.get("agent_id").getAsString();
		String runId = root.get("run_id").getAsString();
		String text = root.getAsJsonArray("messages").get(0).getAsJsonObject().get("content").getAsString();
		// 模拟 mem0 精确哈希去重：同范围同文本不新建
		for (JsonObject existing : items.values()) {
			if (existing.get("user_id").getAsString().equals(userId)
					&& existing.get("agent_id").getAsString().equals(agentId)
					&& existing.get("run_id").getAsString().equals(runId)
					&& existing.get("memory").getAsString().equals(text)) {
				respond(exchange, 200, "{\"results\":[{\"id\":\"" + existing.get("id").getAsString()
						+ "\",\"event\":\"NOOP\"}]}");
				return;
			}
		}
		String id = "m-" + idSequence.incrementAndGet();
		JsonObject item = new JsonObject();
		item.addProperty("id", id);
		item.addProperty("memory", text);
		item.addProperty("user_id", userId);
		item.addProperty("agent_id", agentId);
		item.addProperty("run_id", runId);
		item.add("metadata", root.get("metadata"));
		items.put(id, item);
		respond(exchange, 200, "{\"results\":[{\"id\":\"" + id + "\",\"event\":\"ADD\"}]}");
	}

	private void handlePut(HttpExchange exchange, String path) throws IOException {
		String id = memoryIdOf(path);
		JsonObject item = items.get(id);
		if (item == null) {
			respond(exchange, 404, "{}");
			return;
		}
		JsonObject body = JsonParser.parseString(readBody(exchange)).getAsJsonObject();
		item.addProperty("memory", body.get("text").getAsString());
		putCount.incrementAndGet();
		respond(exchange, 200, "{\"event\":\"UPDATE\"}");
	}

	private void handleDelete(HttpExchange exchange, String path) throws IOException {
		items.remove(memoryIdOf(path));
		deleteCount.incrementAndGet();
		respond(exchange, 200, "{\"event\":\"DELETE\"}");
	}

	private static String memoryIdOf(String path) {
		return path.substring("/memories/".length());
	}

	private static boolean scopeMatches(JsonObject item, Map<String, String> params) {
		return matches(item, "user_id", params.get("user_id"))
				&& matches(item, "agent_id", params.get("agent_id"))
				&& matches(item, "run_id", params.get("run_id"));
	}

	private static boolean matches(JsonObject item, String field, String expected) {
		if (expected == null) {
			return true;
		}
		return item.has(field) && item.get(field).getAsString().equals(expected);
	}

	private static Map<String, String> parseQuery(String query) {
		Map<String, String> params = new LinkedHashMap<>();
		if (query == null || query.isEmpty()) {
			return params;
		}
		for (String pair : query.split("&")) {
			int index = pair.indexOf('=');
			if (index > 0) {
				params.put(urlDecode(pair.substring(0, index)), urlDecode(pair.substring(index + 1)));
			}
		}
		return params;
	}

	private static String urlDecode(String value) {
		try {
			return java.net.URLDecoder.decode(value, "UTF-8");
		} catch (java.io.UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	private static String readBody(HttpExchange exchange) throws IOException {
		InputStream in = exchange.getRequestBody();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[2048];
		int read;
		while ((read = in.read(buffer)) >= 0) {
			out.write(buffer, 0, read);
		}
		return new String(out.toByteArray(), StandardCharsets.UTF_8);
	}

	private static void respond(HttpExchange exchange, int status, String body) throws IOException {
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");
		exchange.sendResponseHeaders(status, bytes.length);
		OutputStream out = exchange.getResponseBody();
		out.write(bytes);
		out.close();
	}
}
