package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.facts;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.Mem0RestClient;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 mem0 兼容 REST 服务的 {@link FactStore}（跨会话事实通道，独立 mem0 实例/集合）。
 *
 * <p>与消息通道互不干扰：不与消息通道共享 {@code run_id} 语义，写入请求不带会话键，
 * 也不携带消息通道的 {@code metadata}（{@code seq} / {@code messageType}）。</p>
 *
 * <p>端点契约（相对 {@code base-url}，自托管 OSS 无 {@code /v1} 前缀）：</p>
 * <ul>
 *   <li>{@code POST /memories}（{@code infer=true}）— 抽取式写入：真实对话消息交由服务端抽取；
 *       <b>不带</b> {@code run_id}（不锁定会话，跨会话可见）；</li>
 *   <li>{@code POST /memories}（{@code infer=false}）— 直存写入：本地已抽取事实逐条落库；</li>
 *   <li>{@code POST /search} — 语义检索（{@code query} + {@code user_id} + {@code top_k}，
 *       响应取 {@code results[].memory}）；</li>
 *   <li>{@code GET /memories} — 列表（{@code user_id} + {@code top_k} 截断）。</li>
 * </ul>
 *
 * <p>失败语义：不吞异常（非 2xx 由 {@link Mem0RestClient} 抛 {@link IllegalStateException}），
 * 由上层隔离。</p>
 */
public class Mem0FactStore implements FactStore {

	private static final Gson GSON = new Gson();

	private static final int MAX_ERROR_SNIPPET = 256;

	private final Mem0RestClient client;

	public Mem0FactStore(String baseUrl, String apiKey, Duration connectTimeout, Duration readTimeout) {
		this(new Mem0RestClient(baseUrl, apiKey, connectTimeout, readTimeout));
	}

	Mem0FactStore(Mem0RestClient client) {
		if (client == null) {
			throw new IllegalArgumentException("Mem0FactStore: client 不能为空");
		}
		this.client = client;
	}

	@Override
	public void addDialogue(List<ChatMessage> turnMessages, String userId) {
		requireUserId(userId);
		JsonArray messages = toMessages(turnMessages);
		if (messages.size() == 0) {
			return;
		}
		JsonObject root = new JsonObject();
		root.add("messages", messages);
		root.addProperty("user_id", userId.trim());
		root.addProperty("infer", true);
		client.post("/memories", GSON.toJson(root));
	}

	@Override
	public void addFacts(List<String> facts, String userId) {
		requireUserId(userId);
		if (facts == null) {
			return;
		}
		for (String fact : facts) {
			if (fact == null || fact.trim().isEmpty()) {
				continue;
			}
			JsonObject message = new JsonObject();
			message.addProperty("role", "user");
			message.addProperty("content", fact.trim());
			JsonArray messages = new JsonArray();
			messages.add(message);
			JsonObject root = new JsonObject();
			root.add("messages", messages);
			root.addProperty("user_id", userId.trim());
			root.addProperty("infer", false);
			client.post("/memories", GSON.toJson(root));
		}
	}

	@Override
	public List<String> search(String query, String userId, int topK) {
		requireUserId(userId);
		JsonObject root = new JsonObject();
		root.addProperty("query", query == null ? "" : query);
		root.addProperty("user_id", userId.trim());
		root.addProperty("top_k", Math.max(topK, 1));
		return extractMemories(client.post("/search", GSON.toJson(root)));
	}

	@Override
	public List<String> listAll(String userId, int limit) {
		requireUserId(userId);
		Map<String, String> params = new LinkedHashMap<String, String>();
		params.put("user_id", userId.trim());
		params.put("top_k", String.valueOf(Math.max(limit, 1)));
		return extractMemories(client.get("/memories", Mem0RestClient.query(params)));
	}

	// -------------------------------------------------- 内部实现

	private static void requireUserId(String userId) {
		if (userId == null || userId.trim().isEmpty()) {
			throw new IllegalArgumentException("Mem0FactStore: userId 不能为空");
		}
	}

	private static JsonArray toMessages(List<ChatMessage> turnMessages) {
		JsonArray messages = new JsonArray();
		if (turnMessages == null) {
			return messages;
		}
		for (ChatMessage message : turnMessages) {
			String text = textOf(message);
			if (text == null || text.trim().isEmpty()) {
				continue;
			}
			JsonObject item = new JsonObject();
			item.addProperty("role", roleOf(message));
			item.addProperty("content", text);
			messages.add(item);
		}
		return messages;
	}

	private static String roleOf(ChatMessage message) {
		if (message instanceof AiMessage) {
			return "assistant";
		}
		if (message instanceof SystemMessage) {
			return "system";
		}
		return "user";
	}

	private static String textOf(ChatMessage message) {
		if (message instanceof UserMessage) {
			UserMessage userMessage = (UserMessage) message;
			return userMessage.hasSingleText() ? userMessage.singleText() : null;
		}
		if (message instanceof AiMessage) {
			return ((AiMessage) message).text();
		}
		if (message instanceof SystemMessage) {
			return ((SystemMessage) message).text();
		}
		if (message instanceof ToolExecutionResultMessage) {
			return ((ToolExecutionResultMessage) message).text();
		}
		return null;
	}

	/** 兼容 {@code results:[{memory:...}]} 与顶层数组两种响应形态，抽取事实文本。 */
	private static List<String> extractMemories(String body) {
		JsonElement root;
		try {
			root = JsonParser.parseString(body == null ? "" : body);
		} catch (JsonSyntaxException e) {
			throw new IllegalStateException("mem0 响应不是合法 JSON: " + snippet(body), e);
		}
		JsonArray array;
		if (root.isJsonArray()) {
			array = root.getAsJsonArray();
		} else if (root.isJsonObject() && root.getAsJsonObject().has("results")
				&& root.getAsJsonObject().get("results").isJsonArray()) {
			array = root.getAsJsonObject().getAsJsonArray("results");
		} else {
			throw new IllegalStateException("mem0 响应缺少结果数组: " + snippet(body));
		}
		List<String> memories = new ArrayList<String>(array.size());
		for (JsonElement element : array) {
			if (!element.isJsonObject()) {
				continue;
			}
			JsonElement memory = element.getAsJsonObject().get("memory");
			if (memory != null && memory.isJsonPrimitive()) {
				String text = memory.getAsString();
				if (text != null && !text.trim().isEmpty()) {
					memories.add(text);
				}
			}
		}
		return memories;
	}

	private static String snippet(String body) {
		if (body == null) {
			return "";
		}
		String trimmed = body.trim();
		return trimmed.length() <= MAX_ERROR_SNIPPET ? trimmed : trimmed.substring(0, MAX_ERROR_SNIPPET) + "...";
	}
}
