package com.xiongx9527.fw.plus.ai.nativeimpl.memory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.base.memory.AbstractConversationMemoryPort;
import com.xiongx9527.fw.plus.ai.base.memory.ConversationMemoryAddRequest;
import com.xiongx9527.fw.plus.ai.base.memory.ConversationMemoryException;
import com.xiongx9527.fw.plus.ai.base.memory.ConversationMemoryListener;
import com.xiongx9527.fw.plus.ai.base.memory.ConversationMemoryScope;
import com.xiongx9527.fw.plus.ai.base.memory.ConversationMemorySearchRequest;
import com.xiongx9527.fw.plus.ai.base.memory.MemoryRecord;
import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.nativeimpl.http.OkHttp;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 真实原子能力：mem0 自托管服务的 REST 客户端（语义记忆后端）。
 *
 * <p>对齐 mem0 OSS 服务端 API（默认 {@code http://localhost:8888}，<b>无</b> {@code /v1} 前缀）：
 * {@code POST /memories} 写入、{@code POST /search} 检索、{@code GET /memories} 列出、
 * {@code DELETE /memories/{id}} 与 {@code DELETE /memories} 删除。服务端可选鉴权
 * （{@code X-API-Key} 或 {@code Authorization: Bearer}），关闭鉴权时不传 apiKey 即可。</p>
 *
 * <p>落点是 base 的 {@code ConversationMemoryPort}；与 LLM 端口一致，监听器由
 * {@link AbstractConversationMemoryPort} 统一派发。</p>
 */
public class Mem0ServerConversationMemoryPort extends AbstractConversationMemoryPort {

    private static final Logger log = LoggerFactory.getLogger(Mem0ServerConversationMemoryPort.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final OkHttpClient client;
    private final String baseUrl;
    private final String apiKey;
    private final MemoryAuthScheme authScheme;

    public Mem0ServerConversationMemoryPort(String baseUrl, String apiKey) {
        this(baseUrl, apiKey, MemoryAuthScheme.API_KEY, null, null, null);
    }

    public Mem0ServerConversationMemoryPort(String baseUrl, String apiKey, MemoryAuthScheme authScheme,
            Integer connectTimeoutMillis, Integer readTimeoutMillis,
            List<ConversationMemoryListener> listeners) {
        super(listeners);
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("mem0 baseUrl is required");
        }
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.apiKey = apiKey;
        this.authScheme = authScheme == null ? MemoryAuthScheme.API_KEY : authScheme;
        long connect = connectTimeoutMillis != null ? connectTimeoutMillis : 8_000L;
        long read = readTimeoutMillis != null ? readTimeoutMillis : 30_000L;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(connect, TimeUnit.MILLISECONDS)
                .readTimeout(read, TimeUnit.MILLISECONDS)
                .build();
    }

    @Override
    public List<MemoryRecord> add(ConversationMemoryAddRequest request) {
        if (request == null || request.getMessages().isEmpty()) {
            throw new ConversationMemoryException("mem0 add requires at least one message");
        }
        List<Map<String, Object>> messages = new ArrayList<Map<String, Object>>();
        for (ChatMessage m : request.getMessages()) {
            Map<String, Object> entry = new LinkedHashMap<String, Object>();
            entry.put("role", m.getRole().name().toLowerCase());
            entry.put("content", m.getContent() == null ? "" : m.getContent());
            messages.add(entry);
        }
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("messages", messages);
        putScope(body, request.getUserId(), request.getAgentId(), request.getRunId());
        if (!request.getMetadata().isEmpty()) {
            body.put("metadata", request.getMetadata());
        }
        JsonNode root = post(baseUrl + "/memories", body);
        List<MemoryRecord> records = parseResults(root, request.getUserId(), request.getAgentId(), request.getRunId());
        notifyAdd(scopeOf(request.getUserId(), request.getAgentId(), request.getRunId()), records);
        return records;
    }

    @Override
    public List<MemoryRecord> search(ConversationMemorySearchRequest request) {
        if (request == null || request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            throw new ConversationMemoryException("mem0 search requires a query");
        }
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("query", request.getQuery());
        putScope(body, request.getUserId(), request.getAgentId(), request.getRunId());
        if (!request.getFilters().isEmpty()) {
            body.put("filters", request.getFilters());
        }
        if (request.getTopK() != null) {
            body.put("top_k", request.getTopK());
        }
        JsonNode root = post(baseUrl + "/search", body);
        List<MemoryRecord> records = parseResults(root, request.getUserId(), request.getAgentId(), request.getRunId());
        notifySearch(scopeOf(request.getUserId(), request.getAgentId(), request.getRunId()), records);
        return records;
    }

    @Override
    public List<MemoryRecord> list(ConversationMemoryScope scope) {
        String url = baseUrl + "/memories" + toQuery(scope);
        JsonNode root = get(url);
        List<MemoryRecord> records = parseResults(root, scope.getUserId(), scope.getAgentId(), scope.getRunId());
        return records;
    }

    @Override
    public void delete(String memoryId) {
        if (memoryId == null || memoryId.trim().isEmpty()) {
            throw new ConversationMemoryException("mem0 delete requires a memory id");
        }
        delete(baseUrl + "/memories/" + memoryId, memoryId);
    }

    @Override
    public void deleteAll(ConversationMemoryScope scope) {
        if (scope.isEmpty()) {
            throw new ConversationMemoryException("mem0 deleteAll requires at least one scope identifier");
        }
        delete(baseUrl + "/memories" + toQuery(scope), null);
    }

    private void delete(String url, String memoryId) {
        Request.Builder rb = new Request.Builder().url(url).delete();
        addAuth(rb);
        try (Response resp = client.newCall(rb.build()).execute()) {
            if (!resp.isSuccessful()) {
                String msg = bodyOf(resp);
                notifyError("mem0 delete failed: http " + resp.code(), scopeOf(null, null, null));
                throw new ConversationMemoryException("mem0 delete http " + resp.code() + ": " + msg);
            }
            notifyDelete(scopeOf(null, null, null), memoryId);
        } catch (IOException e) {
            notifyError("mem0 delete io: " + e.getMessage(), scopeOf(null, null, null));
            throw new ConversationMemoryException("mem0 delete failed: " + e.getMessage(), e);
        }
    }

    private JsonNode post(String url, Map<String, Object> body) {
        try {
            byte[] bytes = MAPPER.writeValueAsBytes(body);
            Request.Builder rb = new Request.Builder().url(url)
                    .post(RequestBody.create(JSON, bytes));
            addAuth(rb);
            try (Response resp = client.newCall(rb.build()).execute()) {
                return readJson(resp);
            }
        } catch (IOException e) {
            notifyError("mem0 post io: " + e.getMessage(), scopeOf(null, null, null));
            throw new ConversationMemoryException("mem0 request failed: " + e.getMessage(), e);
        }
    }

    private JsonNode get(String url) {
        try {
            Request.Builder rb = new Request.Builder().url(url).get();
            addAuth(rb);
            try (Response resp = client.newCall(rb.build()).execute()) {
                return readJson(resp);
            }
        } catch (IOException e) {
            notifyError("mem0 get io: " + e.getMessage(), scopeOf(null, null, null));
            throw new ConversationMemoryException("mem0 request failed: " + e.getMessage(), e);
        }
    }

    private JsonNode readJson(Response resp) throws IOException {
        String text = bodyOf(resp);
        if (!resp.isSuccessful()) {
            notifyError("mem0 http " + resp.code(), scopeOf(null, null, null));
            throw new ConversationMemoryException("mem0 http " + resp.code() + ": " + text);
        }
        try {
            return MAPPER.readTree(text);
        } catch (IOException e) {
            throw new ConversationMemoryException("mem0 response is not json: " + text, e);
        }
    }

    private static String bodyOf(Response resp) throws IOException {
        okhttp3.ResponseBody b = resp.body();
        return b == null ? "" : b.string();
    }

    private void addAuth(Request.Builder rb) {
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            if (authScheme == MemoryAuthScheme.BEARER) {
                rb.header("Authorization", "Bearer " + apiKey);
            } else {
                rb.header("X-API-Key", apiKey);
            }
        }
    }

    private static void putScope(Map<String, Object> body, String userId, String agentId, String runId) {
        if (userId != null && !userId.trim().isEmpty()) {
            body.put("user_id", userId);
        }
        if (agentId != null && !agentId.trim().isEmpty()) {
            body.put("agent_id", agentId);
        }
        if (runId != null && !runId.trim().isEmpty()) {
            body.put("run_id", runId);
        }
    }

    private static String toQuery(ConversationMemoryScope scope) {
        StringBuilder sb = new StringBuilder();
        append(sb, "user_id", scope.getUserId());
        append(sb, "agent_id", scope.getAgentId());
        append(sb, "run_id", scope.getRunId());
        return sb.length() == 0 ? "" : sb.toString();
    }

    private static void append(StringBuilder sb, String name, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        sb.append(sb.length() == 0 ? "?" : "&");
        sb.append(name).append("=").append(encode(value));
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }

    private static ConversationMemoryScope scopeOf(String userId, String agentId, String runId) {
        return ConversationMemoryScope.builder()
                .userId(userId).agentId(agentId).runId(runId).build();
    }

    private static List<MemoryRecord> parseResults(JsonNode root, String userId, String agentId, String runId) {
        List<MemoryRecord> out = new ArrayList<MemoryRecord>();
        JsonNode array = root;
        if (root != null && root.has("results")) {
            array = root.get("results");
        }
        if (array == null || !array.isArray()) {
            return out;
        }
        for (JsonNode node : array) {
            String id = textOrNull(node, "id");
            String memory = textOrNull(node, "memory");
            Double score = node.has("score") && !node.get("score").isNull()
                    ? node.get("score").asDouble() : null;
            String event = textOrNull(node, "event");
            String createdAt = textOrNull(node, "created_at");
            String u = node.has("user_id") ? textOrNull(node, "user_id") : userId;
            String a = node.has("agent_id") ? textOrNull(node, "agent_id") : agentId;
            String r = node.has("run_id") ? textOrNull(node, "run_id") : runId;
            Map<String, Object> metadata = new LinkedHashMap<String, Object>();
            JsonNode meta = node.get("metadata");
            if (meta != null && meta.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> it = meta.fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> e = it.next();
                    metadata.put(e.getKey(), e.getValue().asText());
                }
            }
            out.add(MemoryRecord.builder()
                    .id(id).memory(memory).score(score).event(event).createdAt(createdAt)
                    .userId(u).agentId(a).runId(r).metadata(metadata).build());
        }
        return out;
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }
}
