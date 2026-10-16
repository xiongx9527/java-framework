package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.facts;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.langchain4j.data.message.AiMessage;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link Mem0FactStore} 对 mem0 兼容 REST 数据面的事实读写行为（任务 1.1）：
 * 以 JDK {@code com.sun.net.httpserver} 起本地 mock，模拟
 * {@code POST /memories}（抽取写入与直存）、{@code POST /search}（语义检索）、
 * {@code GET /memories}（列表）——均无尾斜杠（自托管实测契约）。
 */
class Mem0FactStoreTest {

	private static final Gson GSON = new Gson();

	private HttpServer server;

	private String baseUrl;

	/** id → 记忆条目（预置数据，用于检索/列表断言） */
	private final Map<String, JsonObject> items = new LinkedHashMap<>();

	private final List<String> addBodies = new ArrayList<>();

	private final List<String> searchBodies = new ArrayList<>();

	private final List<String> getQueries = new ArrayList<>();

	private final List<String> apiKeyHeaders = new ArrayList<>();

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

	// -------------------------------------------------- 写入契约

	@Test
	void addDialogueSendsRealDialogueWithInferTrueAndNoSessionKey() {
		Mem0FactStore store = newStore();

		store.addDialogue(Arrays.asList(UserMessage.from("我叫张三"), AiMessage.from("你好张三")), "u-100");

		assertThat(addBodies).hasSize(1);
		String raw = addBodies.get(0);
		JsonObject body = JsonParser.parseString(raw).getAsJsonObject();
		assertThat(body.get("user_id").getAsString()).isEqualTo("u-100");
		assertThat(body.get("infer").getAsBoolean()).isTrue();
		// 事实通道不带会话键、不携带消息通道的 metadata
		assertThat(body.has("run_id")).isFalse();
		assertThat(body.has("agent_id")).isFalse();
		assertThat(body.has("metadata")).isFalse();
		JsonArray messages = body.getAsJsonArray("messages");
		assertThat(messages).hasSize(2);
		assertThat(messages.get(0).getAsJsonObject().get("role").getAsString()).isEqualTo("user");
		assertThat(messages.get(0).getAsJsonObject().get("content").getAsString()).isEqualTo("我叫张三");
		assertThat(messages.get(1).getAsJsonObject().get("role").getAsString()).isEqualTo("assistant");
		assertThat(messages.get(1).getAsJsonObject().get("content").getAsString()).isEqualTo("你好张三");
		// 抽取输入为真实对话文本，禁止复用消息通道的 {"seq","msg"} 包装串
		assertThat(raw).doesNotContain("\"seq\"").doesNotContain("\"msg\"");
	}

	@Test
	void addFactsStoresEachFactWithInferFalse() {
		Mem0FactStore store = newStore();

		store.addFacts(Arrays.asList("用户偏好深色主题", "用户叫张三", " "), "u-100");

		assertThat(addBodies).hasSize(2);
		assertThat(JsonParser.parseString(addBodies.get(0)).getAsJsonObject().get("user_id").getAsString())
				.isEqualTo("u-100");
		for (int i = 0; i < addBodies.size(); i++) {
			JsonObject body = JsonParser.parseString(addBodies.get(i)).getAsJsonObject();
			assertThat(body.get("infer").getAsBoolean()).isFalse();
			assertThat(body.has("run_id")).isFalse();
			JsonArray messages = body.getAsJsonArray("messages");
			assertThat(messages).hasSize(1);
			assertThat(messages.get(0).getAsJsonObject().get("role").getAsString()).isEqualTo("user");
			assertThat(messages.get(0).getAsJsonObject().get("content").getAsString())
					.isEqualTo(i == 0 ? "用户偏好深色主题" : "用户叫张三");
		}
	}

	@Test
	void blankUserIdRejected() {
		Mem0FactStore store = newStore();

		assertThatThrownBy(() -> store.addDialogue(Arrays.asList(UserMessage.from("你好")), " "))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> store.addFacts(Arrays.asList("事实"), null))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> store.search("查询", " ", 5)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> store.listAll("", 10)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void apiKeySentAsXApiKeyHeader() {
		Mem0FactStore store = new Mem0FactStore(baseUrl, "secret-1", Duration.ofSeconds(2), Duration.ofSeconds(5));

		store.addFacts(Arrays.asList("事实一条"), "u-100");

		assertThat(apiKeyHeaders).containsOnly("secret-1");
	}

	// -------------------------------------------------- 读取契约

	@Test
	void searchQueriesByUserDimensionAndReturnsFacts() {
		seed("m-1", "用户偏好深色主题", "u-100");
		seed("m-2", "用户住在杭州", "u-100");
		seed("m-3", "别的用户的事实", "u-200");
		Mem0FactStore store = newStore();

		List<String> facts = store.search("主题", "u-100", 5);

		assertThat(facts).containsExactly("用户偏好深色主题", "用户住在杭州");
		JsonObject body = JsonParser.parseString(searchBodies.get(0)).getAsJsonObject();
		assertThat(body.get("query").getAsString()).isEqualTo("主题");
		assertThat(body.get("user_id").getAsString()).isEqualTo("u-100");
		assertThat(body.get("top_k").getAsInt()).isEqualTo(5);
		assertThat(body.has("run_id")).isFalse();
	}

	@Test
	void searchHonorsTopK() {
		seed("m-1", "事实一", "u-100");
		seed("m-2", "事实二", "u-100");
		Mem0FactStore store = newStore();

		assertThat(store.search("事实", "u-100", 1)).hasSize(1);
	}

	@Test
	void listAllReadsUserMemoriesWithTopK() {
		seed("m-1", "事实一", "u-100");
		seed("m-2", "事实二", "u-100");
		seed("m-3", "别的用户的事实", "u-200");
		Mem0FactStore store = newStore();

		List<String> facts = store.listAll("u-100", 50);

		assertThat(facts).containsExactly("事实一", "事实二");
		assertThat(getQueries.get(0)).contains("user_id=u-100").contains("top_k=50");
	}

	// -------------------------------------------------- 失败语义

	@Test
	void httpFailureFailsFast() {
		Mem0FactStore store = newStore();
		failing = true;

		assertThatThrownBy(() -> store.addDialogue(Arrays.asList(UserMessage.from("你好")), "u-100"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("mem0 请求失败");
		assertThatThrownBy(() -> store.search("查询", "u-100", 5))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("mem0 请求失败");
	}

	@Test
	void invalidBaseUrlRejected() {
		assertThatThrownBy(() -> new Mem0FactStore(" ", "", Duration.ofSeconds(1), Duration.ofSeconds(1)))
				.isInstanceOf(IllegalArgumentException.class);
	}

	// -------------------------------------------------- 基础设施

	private Mem0FactStore newStore() {
		return new Mem0FactStore(baseUrl, "", Duration.ofSeconds(2), Duration.ofSeconds(5));
	}

	private void seed(String id, String memory, String userId) {
		JsonObject item = new JsonObject();
		item.addProperty("id", id);
		item.addProperty("memory", memory);
		item.addProperty("user_id", userId);
		items.put(id, item);
	}

	private void handle(HttpExchange exchange) throws IOException {
		try {
			apiKeyHeaders.add(exchange.getRequestHeaders().getFirst("X-API-Key"));
			if (failing) {
				respond(exchange, 500, "{\"error\":\"boom\"}");
				return;
			}
			String path = exchange.getRequestURI().getPath();
			String method = exchange.getRequestMethod();
			if ("POST".equals(method) && "/memories".equals(path)) {
				addBodies.add(readBody(exchange));
				respond(exchange, 200, "{\"results\":[{\"id\":\"m-new\",\"event\":\"ADD\"}]}");
			} else if ("POST".equals(method) && "/search".equals(path)) {
				handleSearch(exchange);
			} else if ("GET".equals(method) && "/memories".equals(path)) {
				handleGet(exchange);
			} else {
				respond(exchange, 404, "{}");
			}
		} catch (Exception e) {
			respond(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
		}
	}

	private void handleSearch(HttpExchange exchange) throws IOException {
		String raw = readBody(exchange);
		searchBodies.add(raw);
		JsonObject body = JsonParser.parseString(raw).getAsJsonObject();
		String userId = body.get("user_id").getAsString();
		int topK = body.has("top_k") ? body.get("top_k").getAsInt() : Integer.MAX_VALUE;
		JsonArray array = new JsonArray();
		for (JsonObject item : items.values()) {
			if (array.size() >= topK) {
				break;
			}
			if (item.get("user_id").getAsString().equals(userId)) {
				array.add(item);
			}
		}
		JsonObject root = new JsonObject();
		root.add("results", array);
		respond(exchange, 200, GSON.toJson(root));
	}

	private void handleGet(HttpExchange exchange) throws IOException {
		String query = exchange.getRequestURI().getRawQuery();
		getQueries.add(query == null ? "" : query);
		Map<String, String> params = parseQuery(query);
		String userId = params.get("user_id");
		int topK = params.containsKey("top_k") ? Integer.parseInt(params.get("top_k")) : Integer.MAX_VALUE;
		// 顶层数组形态（与 {"results":[...]} 兼容解析对照）
		JsonArray array = new JsonArray();
		for (JsonObject item : items.values()) {
			if (array.size() >= topK) {
				break;
			}
			if (item.get("user_id").getAsString().equals(userId)) {
				array.add(item);
			}
		}
		respond(exchange, 200, GSON.toJson(array));
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
