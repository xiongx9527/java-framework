package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 基于 mem0 兼容 REST 服务的 {@link dev.langchain4j.store.memory.chat.ChatMemoryStore}
 * （逐条消息存储，原文直存不经过 LLM 抽取）。
 *
 * <p>存储映射（mem0 字段 ←→ memId 四段契约 {@code {tenantId}:{appId}:{agentId}:{userId}}，
 * 五段场景同样按 4 段维度解析，不改变消息通道语义）：</p>
 * <ul>
 *   <li>{@code run_id} = memId 原样（会话隔离的正确性唯一来源，写读同逻辑天然自洽）；</li>
 *   <li>{@code user_id} = 段[3]、{@code agent_id} = 段[2]、{@code app_id} = 段[1]
 *       （段数不足 4 时回退默认值：user/agent 为常量 {@code default}、app 为配置
 *       {@code default-app-id}）；</li>
 *   <li>{@code metadata} 冗余 {@code tenant_id}（段0，段数不足回退租户解析器/默认值）、
 *       {@code user_id} / {@code agent_id} / {@code app_id} 与 {@code message_seq}（排序依据）、
 *       {@code message_type}；</li>
 *   <li>读取（{@code GET /memories}）按 {@code user_id + agent_id + run_id} 服务端过滤——
 *       三者均无配置回退漂移，历史数据不因改配置而失联（run_id 支持 4/5 段）；</li>
 *   <li>记忆正文为 {@code {\"seq\":n,\"msg\":[…]}} 包装 JSON：序号编入正文保证同会话内文本唯一，
 *       规避 mem0 按精确哈希对同范围重复文本去重导致的丢消息；内层 {@code msg} 即基类约定的
 *       「单元素消息 JSON 数组」，读取时拆包还原；</li>
 *   <li>{@code messages[].role} 统一使用 {@code user}（mem0 直存会丢弃 {@code system} 角色消息，
 *       实际消息类型在 JSON 内部，不受影响），且以 {@code infer=false} 提交，跳过 LLM 抽取。</li>
 * </ul>
 *
 * <p>{@code writeMessages} 按 {@code message_seq} 原位比较——内容一致跳过、不一致
 * {@code PUT} 更新、缺失序号 {@code POST} 新增；超出目标序号的旧消息<strong>保留不删</strong>
 * （append-only：mem0 作为该维度组合的全量历史库，「加载历史记忆」按四段 {@code run_id}
 * 拉回全部；上下文注入的窗口裁剪由 {@code MessageWindowChatMemory} 负责），
 * 仅清理序号无效的脏数据与同序号重复条目，不做先删后插，避免并发读到空窗。</p>
 */
public class Mem0ChatMemoryStore extends AbstractJsonChatMemoryStore {

	/** 消息序号 metadata 键 */
	static final String META_SEQ = "message_seq";

	/** 消息类型 metadata 键 */
	static final String META_MESSAGE_TYPE = "message_type";

	/** 租户 metadata 键 */
	static final String META_TENANT_ID = "tenant_id";

	/** 用户 metadata 键 */
	static final String META_USER_ID = "user_id";

	/** AI 主体 metadata 键 */
	static final String META_AGENT_ID = "agent_id";

	/** 应用 metadata 键 */
	static final String META_APP_ID = "app_id";

	/** 单次拉取条数上限（服务端不支持 limit/offset 分页，仅 top_k 截断） */
	private static final int FETCH_LIMIT = 1000;

	private static final Gson GSON = new Gson();

	private final Mem0RestClient client;

	public Mem0ChatMemoryStore(ChatMemoryTenantResolver tenantResolver, String defaultAppId,
			String baseUrl, String apiKey, Duration connectTimeout, Duration readTimeout) {
		this(tenantResolver, defaultAppId,
				new Mem0RestClient(baseUrl, apiKey, connectTimeout, readTimeout));
	}

	Mem0ChatMemoryStore(ChatMemoryTenantResolver tenantResolver, String defaultAppId, Mem0RestClient client) {
		super(tenantResolver, defaultAppId);
		if (client == null) {
			throw new IllegalArgumentException("Mem0ChatMemoryStore: client 不能为空");
		}
		this.client = client;
	}

	@Override
	protected List<StoredMessage> readMessages(String tenantId, String memoryId, String appId) {
		List<RemoteMemory> remote = fetchAll(memoryId);
		Collections.sort(remote, (left, right) -> Integer.compare(left.seq, right.seq));
		List<StoredMessage> result = new ArrayList<>(remote.size());
		for (RemoteMemory memory : remote) {
			// 序号无效（metadata 缺失/损坏）的条目不参与读取，写入时会被当作脏数据裁剪
			if (memory.seq < 0) {
				continue;
			}
			String json = unwrapMessageJson(memory.text);
			if (json != null) {
				result.add(new StoredMessage(memory.messageType, json));
			}
		}
		return result;
	}

	@Override
	protected void writeMessages(String tenantId, String memoryId, String appId, List<StoredMessage> messages) {
		int targetSize = messages == null ? 0 : messages.size();
		List<RemoteMemory> existing = fetchAll(memoryId);
		Map<Integer, List<RemoteMemory>> bySeq = new HashMap<>();
		for (RemoteMemory memory : existing) {
			List<RemoteMemory> bucket = bySeq.get(memory.seq);
			if (bucket == null) {
				bucket = new ArrayList<>(1);
				bySeq.put(memory.seq, bucket);
			}
			bucket.add(memory);
		}
		Set<String> handled = new HashSet<>();
		for (int seq = 0; seq < targetSize; seq++) {
			StoredMessage target = messages.get(seq);
			String wrapped = wrapMessageJson(seq, target);
			List<RemoteMemory> atSeq = bySeq.get(seq);
			if (atSeq != null && !atSeq.isEmpty()) {
				// 原位覆盖：内容一致跳过，不一致 PUT 更新；同序号重复条目删除
				RemoteMemory first = atSeq.get(0);
				handled.add(first.id);
				if (!jsonEquals(first.text, wrapped)) {
					client.put(memoryPath(first.id), putBody(wrapped));
				}
				for (int i = 1; i < atSeq.size(); i++) {
					client.delete(memoryPath(atSeq.get(i).id));
					handled.add(atSeq.get(i).id);
				}
			} else {
				client.post("/memories", addBody(tenantId, memoryId, appId, seq, target, wrapped));
			}
		}
		// append-only：超出目标序号的旧消息保留（历史全量留存，上下文窗口裁剪交给 MessageWindowChatMemory）；
		// 仅清理序号无效的脏数据（读取时本就跳过，留着无用）
		for (RemoteMemory memory : existing) {
			if (memory.seq < 0 && !handled.contains(memory.id)) {
				client.delete(memoryPath(memory.id));
			}
		}
	}

	@Override
	protected void removeMessages(String tenantId, String memoryId) {
		for (RemoteMemory memory : fetchAll(memoryId)) {
			client.delete(memoryPath(memory.id));
		}
	}

	// ------------------------------------------------------------------
	// mem0 端点调用
	// ------------------------------------------------------------------

	/**
	 * 拉取当前会话的全部记忆：按 {@code user_id（段3）+ agent_id（段2）+ run_id（memId）}
	 * 服务端过滤，{@code top_k} 单次拉取（openapi 未声明 limit/offset，实测服务端忽略之，
	 * 无法翻页）；app_id 不参与过滤，避免「段数不足回退 + 改配置」导致历史数据失联。
	 */
	private List<RemoteMemory> fetchAll(String memoryId) {
		Map<String, String> params = new LinkedHashMap<>();
		params.put("user_id", ChatMemoryIds.parseUserId(memoryId, ChatMemoryIds.DEFAULT_USER_ID));
		params.put("agent_id", ChatMemoryIds.parseAgentId(memoryId, ChatMemoryIds.DEFAULT_AGENT_ID));
		params.put("run_id", memoryId);
		params.put("top_k", String.valueOf(FETCH_LIMIT));
		List<JsonObject> items = parseItems(client.get("/memories", Mem0RestClient.query(params)));
		List<RemoteMemory> all = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		for (JsonObject item : items) {
			RemoteMemory memory = toRemoteMemory(item);
			if (memory != null && seen.add(memory.id)) {
				all.add(memory);
			}
		}
		return all;
	}

	/** 解析列表响应：兼容顶层数组与 {@code {"results":[...]}} 两种形态。 */
	private static List<JsonObject> parseItems(String body) {
		JsonElement root;
		try {
			root = JsonParser.parseString(body == null ? "" : body);
		} catch (JsonSyntaxException e) {
			throw new IllegalStateException("mem0 响应不是合法 JSON: " + snippet(body), e);
		}
		JsonArray array;
		if (root.isJsonArray()) {
			array = root.getAsJsonArray();
		} else if (root.isJsonObject()) {
			JsonElement results = root.getAsJsonObject().get("results");
			if (results != null && results.isJsonArray()) {
				array = results.getAsJsonArray();
			} else {
				throw new IllegalStateException("mem0 响应缺少结果数组: " + snippet(body));
			}
		} else {
			throw new IllegalStateException("mem0 响应格式无法解析: " + snippet(body));
		}
		List<JsonObject> items = new ArrayList<>(array.size());
		for (JsonElement element : array) {
			if (element.isJsonObject()) {
				items.add(element.getAsJsonObject());
			}
		}
		return items;
	}

	private static RemoteMemory toRemoteMemory(JsonObject item) {
		JsonElement idElement = item.get("id");
		JsonElement memoryElement = item.get("memory");
		if (idElement == null || !idElement.isJsonPrimitive() || memoryElement == null
				|| !memoryElement.isJsonPrimitive()) {
			return null;
		}
		RemoteMemory memory = new RemoteMemory();
		memory.id = idElement.getAsString();
		memory.text = memoryElement.getAsString();
		memory.messageType = null;
		memory.seq = -1;
		JsonElement metadataElement = item.get("metadata");
		if (metadataElement != null && metadataElement.isJsonObject()) {
			JsonObject metadata = metadataElement.getAsJsonObject();
			memory.messageType = stringValue(metadata.get(META_MESSAGE_TYPE));
			Integer seq = intValue(metadata.get(META_SEQ));
			if (seq != null && seq >= 0) {
				memory.seq = seq;
			}
		}
		return memory;
	}

	private static String addBody(String tenantId, String memoryId, String appId, int seq,
			StoredMessage target, String wrapped) {
		String userId = ChatMemoryIds.parseUserId(memoryId, ChatMemoryIds.DEFAULT_USER_ID);
		String agentId = ChatMemoryIds.parseAgentId(memoryId, ChatMemoryIds.DEFAULT_AGENT_ID);
		JsonObject message = new JsonObject();
		message.addProperty("role", "user");
		message.addProperty("content", wrapped);
		JsonArray messages = new JsonArray();
		messages.add(message);
		JsonObject metadata = new JsonObject();
		metadata.addProperty(META_TENANT_ID, tenantId);
		metadata.addProperty(META_USER_ID, userId);
		metadata.addProperty(META_AGENT_ID, agentId);
		metadata.addProperty(META_APP_ID, appId);
		if (target.getMessageType() != null) {
			metadata.addProperty(META_MESSAGE_TYPE, target.getMessageType());
		}
		metadata.addProperty(META_SEQ, seq);
		JsonObject root = new JsonObject();
		root.add("messages", messages);
		root.addProperty("user_id", userId);
		root.addProperty("agent_id", agentId);
		root.addProperty("app_id", appId);
		root.addProperty("run_id", memoryId);
		root.add("metadata", metadata);
		root.addProperty("infer", false);
		return GSON.toJson(root);
	}

	private static String putBody(String wrapped) {
		JsonObject root = new JsonObject();
		root.addProperty("text", wrapped);
		return GSON.toJson(root);
	}

	private static String memoryPath(String id) {
		return "/memories/" + id;
	}

	// ------------------------------------------------------------------
	// 消息包装 / 拆包
	// ------------------------------------------------------------------

	/** 包装为 {@code {"seq":n,"msg":[…]}}，保证同会话内正文文本唯一。 */
	private static String wrapMessageJson(int seq, StoredMessage target) {
		JsonElement messageJson;
		try {
			messageJson = JsonParser.parseString(target.getJson());
		} catch (JsonSyntaxException e) {
			throw new IllegalStateException("Mem0ChatMemoryStore: 消息 JSON 无法解析: " + snippet(target.getJson()), e);
		}
		JsonObject wrapper = new JsonObject();
		wrapper.addProperty("seq", seq);
		wrapper.add("msg", messageJson);
		return GSON.toJson(wrapper);
	}

	/** 拆包还原「单元素消息 JSON 数组」；无法解析（脏数据）返回 {@code null} 由调用方跳过。 */
	private static String unwrapMessageJson(String text) {
		JsonElement element;
		try {
			element = JsonParser.parseString(text == null ? "" : text);
		} catch (JsonSyntaxException e) {
			return null;
		}
		if (element.isJsonObject()) {
			JsonElement message = element.getAsJsonObject().get("msg");
			return message != null && message.isJsonArray() ? message.toString() : null;
		}
		// 兼容直接存消息数组的历史数据
		return element.isJsonArray() ? element.toString() : null;
	}

	private static boolean jsonEquals(String left, String right) {
		if (left == null) {
			return right == null;
		}
		if (right == null) {
			return false;
		}
		if (left.equals(right)) {
			return true;
		}
		try {
			return JsonParser.parseString(left).equals(JsonParser.parseString(right));
		} catch (JsonSyntaxException e) {
			return false;
		}
	}

	// ------------------------------------------------------------------
	// 工具
	// ------------------------------------------------------------------

	private static String stringValue(JsonElement element) {
		return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
	}

	private static Integer intValue(JsonElement element) {
		if (element == null || !element.isJsonPrimitive()) {
			return null;
		}
		try {
			return element.getAsInt();
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String snippet(String body) {
		if (body == null) {
			return "";
		}
		String flattened = body.replaceAll("\\s+", " ").trim();
		return flattened.length() <= 256 ? flattened : flattened.substring(0, 256) + "...";
	}

	/** 远端单条记忆：ID + 序号 + 包装正文（序号无效时为 -1，写入时按脏数据裁剪）。 */
	private static final class RemoteMemory {

		private String id;

		private int seq;

		private String text;

		private String messageType;
	}
}
