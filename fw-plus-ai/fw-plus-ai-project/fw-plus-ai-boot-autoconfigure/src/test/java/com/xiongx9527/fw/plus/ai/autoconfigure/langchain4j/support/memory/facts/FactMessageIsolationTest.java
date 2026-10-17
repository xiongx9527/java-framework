package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.facts;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.Mem0ChatMemoryStore;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageJsonCodec;
import dev.langchain4j.data.message.JacksonChatMessageJsonCodec;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 任务 1.2：事实通道与消息通道的物理隔离。
 *
 * <p>mock 服务以 {@code run_id} 区分两套逻辑集合（对应真实部署中的独立 mem0 实例/集合）：
 * 携带 {@code run_id} 的写入归属消息通道，不携带的归属事实通道；读取按过滤参数路由。
 * 本测试同时校验两通道的请求形状（会话键的有无）与读取结果互不出现对方内容。</p>
 */
class FactMessageIsolationTest {

	private static final String MEM_ID = "t1:crm:bot:u1";

	private static final ChatMessageJsonCodec CODEC = new JacksonChatMessageJsonCodec();

	private static final Gson GSON = new Gson();

	private HttpServer server;

	private String baseUrl;

	/** 消息通道条目（POST 携带 run_id） */
	private final List<JsonObject> messageItems = new ArrayList<>();

	/** 事实通道条目（POST 不携带 run_id / metadata） */
	private final List<JsonObject> factItems = new ArrayList<>();

	private final AtomicInteger idSeq = new AtomicInteger();

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

	// -------------------------------------------------- 隔离断言

	@Test
	void factWritesDoNotPolluteMessageMemory() {
		Mem0ChatMemoryStore messageStore = newMessageStore();
		Mem0FactStore factStore = newFactStore();
		List<ChatMessage> messages = Arrays.asList(UserMessage.from("我叫张三"), AiMessage.from("你好张三"));

		messageStore.updateMessages(MEM_ID, messages);

		// 事实通道：抽取写入（真实对话文本）与直存写入，同一用户段 u1
		factStore.addDialogue(Arrays.asList(UserMessage.from("我叫张三"), AiMessage.from("你好张三")), "u1");
		factStore.addFacts(Arrays.asList("用户叫张三"), "u1");

		// 按会话读取消息记忆：逐条还原且不出现事实内容，消息体解析不受影响
		String readJson = jsonOf(messageStore.getMessages(MEM_ID));
		assertThat(readJson).isEqualTo(jsonOf(messages));
		assertThat(readJson).doesNotContain("用户叫张三");
	}

	@Test
	void messageItemsInvisibleToFactReads() {
		Mem0ChatMemoryStore messageStore = newMessageStore();
		Mem0FactStore factStore = newFactStore();

		messageStore.updateMessages(MEM_ID, Arrays.asList(UserMessage.from("我叫张三"), AiMessage.from("你好张三")));
		factStore.addFacts(Arrays.asList("用户叫张三"), "u1");

		// 事实通道的检索与列表只看到事实，看不到消息通道条目
		assertThat(factStore.listAll("u1", 100)).containsExactly("用户叫张三");
		assertThat(factStore.search("张三", "u1", 5)).containsExactly("用户叫张三");
	}

	@Test
	void channelWritesCarryExpectedSessionKeyShape() {
		Mem0ChatMemoryStore messageStore = newMessageStore();
		Mem0FactStore factStore = newFactStore();

		messageStore.updateMessages(MEM_ID, Arrays.asList(UserMessage.from("我叫张三")));
		factStore.addFacts(Arrays.asList("用户叫张三"), "u1");

		// 消息通道条目：携带会话键与 metadata（读取按 run_id 精确命中）
		assertThat(messageItems).hasSize(1);
		JsonObject messageItem = messageItems.get(0);
		assertThat(messageItem.get("run_id").getAsString()).isEqualTo(MEM_ID);
		assertThat(messageItem.getAsJsonObject("metadata")).isNotNull();

		// 事实通道条目：不带会话键、不带消息通道 metadata
		assertThat(factItems).hasSize(1);
		JsonObject factItem = factItems.get(0);
		assertThat(factItem.has("run_id")).isFalse();
		assertThat(factItem.has("metadata")).isFalse();
		assertThat(factItem.get("user_id").getAsString()).isEqualTo("u1");
	}

	// -------------------------------------------------- mock 服务

	private void handle(HttpExchange exchange) throws IOException {
		try {
			String path = exchange.getRequestURI().getPath();
			String method = exchange.getRequestMethod();
			if ("POST".equals(method) && "/memories".equals(path)) {
				handlePost(exchange);
			} else if ("GET".equals(method) && "/memories".equals(path)) {
				handleGet(exchange);
			} else if ("POST".equals(method) && "/search".equals(path)) {
				handleSearch(exchange);
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

	private void handlePost(HttpExchange exchange) throws IOException {
		JsonObject body = JsonParser.parseString(readBody(exchange)).getAsJsonObject();
		JsonElement runId = body.get("run_id");
		String userId = body.has("user_id") ? body.get("user_id").getAsString() : null;
		if (runId != null && runId.isJsonPrimitive() && !runId.getAsString().isEmpty()) {
			// 消息通道：一条消息一个条目，保留 metadata 供读取还原序号/类型
			JsonObject item = new JsonObject();
			item.addProperty("id", "m-" + idSeq.incrementAndGet());
			item.addProperty("memory", firstContent(body));
			item.addProperty("user_id", userId);
			item.addProperty("run_id", runId.getAsString());
			JsonElement metadata = body.get("metadata");
			if (metadata != null) {
				item.add("metadata", metadata);
			}
			messageItems.add(item);
		} else {
			// 事实通道：正文为用户/AI 文本，无 run_id、无通道 metadata
			for (JsonElement message : body.getAsJsonArray("messages")) {
				JsonObject item = new JsonObject();
				item.addProperty("id", "f-" + idSeq.incrementAndGet());
				item.addProperty("memory", message.getAsJsonObject().get("content").getAsString());
				item.addProperty("user_id", userId);
				factItems.add(item);
			}
		}
		respond(exchange, 200, "{\"results\":[{\"event\":\"ADD\"}]}");
	}

	private void handleGet(HttpExchange exchange) throws IOException {
		Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
		String runId = params.get("run_id");
		String userId = params.get("user_id");
		int topK = params.containsKey("top_k") ? Integer.parseInt(params.get("top_k")) : Integer.MAX_VALUE;
		// 带 run_id 的读取只路由消息集合；否则路由事实集合（独立实例语义）
		JsonArray array = new JsonArray();
		List<JsonObject> source = runId != null ? messageItems : factItems;
		for (JsonObject item : source) {
			if (array.size() >= topK) {
				break;
			}
			if (matches(item, "user_id", userId) && (runId == null || matches(item, "run_id", runId))) {
				array.add(item);
			}
		}
		respond(exchange, 200, GSON.toJson(array));
	}

	private void handleSearch(HttpExchange exchange) throws IOException {
		JsonObject body = JsonParser.parseString(readBody(exchange)).getAsJsonObject();
		String userId = body.has("user_id") ? body.get("user_id").getAsString() : null;
		int topK = body.has("top_k") ? body.get("top_k").getAsInt() : Integer.MAX_VALUE;
		JsonArray array = new JsonArray();
		for (JsonObject item : factItems) {
			if (array.size() >= topK) {
				break;
			}
			if (matches(item, "user_id", userId)) {
				array.add(item);
			}
		}
		JsonObject root = new JsonObject();
		root.add("results", array);
		respond(exchange, 200, GSON.toJson(root));
	}

	private void handlePut(HttpExchange exchange, String path) throws IOException {
		JsonObject body = JsonParser.parseString(readBody(exchange)).getAsJsonObject();
		String id = path.substring("/memories/".length());
		for (JsonObject item : messageItems) {
			if (id.equals(item.get("id").getAsString())) {
				item.addProperty("memory", body.get("text").getAsString());
			}
		}
		respond(exchange, 200, "{}");
	}

	private void handleDelete(HttpExchange exchange, String path) throws IOException {
		String id = path.substring("/memories/".length());
		remove(messageItems, id);
		remove(factItems, id);
		respond(exchange, 200, "{}");
	}

	private static void remove(List<JsonObject> items, String id) {
		items.removeIf(item -> id.equals(item.get("id").getAsString()));
	}

	private static boolean matches(JsonObject item, String field, String value) {
		if (value == null) {
			return true;
		}
		JsonElement element = item.get(field);
		return element != null && element.isJsonPrimitive() && value.equals(element.getAsString());
	}

	private static String firstContent(JsonObject body) {
		JsonArray messages = body.getAsJsonArray("messages");
		return messages.size() == 0 ? "" : messages.get(0).getAsJsonObject().get("content").getAsString();
	}

	// -------------------------------------------------- 基础设施

	private Mem0ChatMemoryStore newMessageStore() {
		return new Mem0ChatMemoryStore(() -> "resolver-t", "app-cfg", baseUrl, "",
				Duration.ofSeconds(2), Duration.ofSeconds(5));
	}

	private Mem0FactStore newFactStore() {
		return new Mem0FactStore(baseUrl, "", Duration.ofSeconds(2), Duration.ofSeconds(5));
	}

	private static String jsonOf(List<ChatMessage> messages) {
		return CODEC.messagesToJson(messages);
	}

	private static Map<String, String> parseQuery(String query) {
		Map<String, String> params = new LinkedHashMap<>();
		if (query == null || query.isEmpty()) {
			return Collections.emptyMap();
		}
		for (String pair : query.split("&")) {
			int index = pair.indexOf('=');
			if (index > 0) {
				try {
					params.put(URLDecoder.decode(pair.substring(0, index), "UTF-8"),
							URLDecoder.decode(pair.substring(index + 1), "UTF-8"));
				} catch (java.io.UnsupportedEncodingException e) {
					throw new IllegalStateException(e);
				}
			}
		}
		return params;
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
