package com.xiongx9527.fw.plus.ai.nativeimpl.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.base.model.AbstractLlmPort;
import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.ChatRole;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmStreamHandler;
import com.xiongx9527.fw.plus.ai.base.model.ModelInfo;
import com.xiongx9527.fw.plus.ai.base.model.ModelType;
import com.xiongx9527.fw.plus.ai.base.tool.ToolExecutionRequest;
import com.xiongx9527.fw.plus.ai.base.tool.ToolSpecification;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 真实原子能力：Anthropic（Claude）Messages API 客户端（流式 / 非流式 / 工具调用）。
 * 与 OpenAI 协议差异较大（鉴权用 x-api-key、system 提到顶层、响应用 content_block_delta、
 * 工具以 tool_use 块返回），故单独实现，但仍落点 base 的 LlmPort，与 OpenAiCompatibleLlmPort 可互换。
 * 用 okhttp3 + okhttp-sse 做 SSE 解析。
 */
public class AnthropicLlmPort extends AbstractLlmPort {
    private static final Logger log = LoggerFactory.getLogger(AnthropicLlmPort.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final ObjectMapper mapper = new ObjectMapper();
    private final OkHttpClient client;
    private final String baseUrl;
    private final String apiKey;
    private final String defaultModel;
    private final String anthropicVersion;
    public AnthropicLlmPort(String baseUrl, String apiKey, String defaultModel) {
        this(baseUrl, apiKey, defaultModel, "2023-06-01");
    }
    public AnthropicLlmPort(String baseUrl, String apiKey, String defaultModel, String anthropicVersion) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) throw new IllegalArgumentException("llm baseUrl is required");
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.apiKey = apiKey;
        this.defaultModel = defaultModel;
        this.anthropicVersion = anthropicVersion == null ? "2023-06-01" : anthropicVersion;
        this.client = new OkHttpClient.Builder().connectTimeout(8_000, java.util.concurrent.TimeUnit.MILLISECONDS).readTimeout(600_000, java.util.concurrent.TimeUnit.MILLISECONDS).build();
    }
    @Override
    protected void doStream(LlmRequest request, LlmStreamHandler handler) {
        if (baseUrl == null) { handler.onError("llm not configured"); return; }
        try {
            String body = mapper.writeValueAsString(buildBody(request));
            Request.Builder rb = new Request.Builder().url(baseUrl + "/v1/messages").post(RequestBody.create(JSON, body));
            rb.addHeader("Accept", "text/event-stream");
            if (apiKey != null && !apiKey.isEmpty()) rb.addHeader("x-api-key", apiKey);
            rb.addHeader("anthropic-version", anthropicVersion);
            final CountDownLatch done = new CountDownLatch(1);
            final String[] errorBox = new String[1];
            Map<Integer, ToolAcc> acc = new LinkedHashMap<Integer, ToolAcc>();
            EventSourceListener listener = new EventSourceListener() {
                @Override public void onEvent(EventSource es, String id, String type, String data) {
                    if (data == null || data.isEmpty()) return;
                    try { dispatch(data, handler, acc); } catch (IOException ex) { log.warn("anthropic sse parse skipped: {}", ex.getMessage()); }
                }
                @Override public void onFailure(EventSource es, Throwable t, Response r) {
                    errorBox[0] = t != null ? t.getMessage() : "anthropic stream failed";
                    log.warn("anthropic stream failed: {}", errorBox[0]);
                    if (errorBox[0] != null) handler.onError(errorBox[0]);
                    done.countDown();
                }
                @Override public void onClosed(EventSource es) { done.countDown(); }
            };
            EventSource es = EventSources.createFactory(client).newEventSource(rb.build(), listener);
            try { done.await(); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); handler.onError("llm interrupted"); }
            es.cancel();
            if (errorBox[0] == null) {
                for (ToolAcc a : acc.values()) handler.onToolCall(new ToolExecutionRequest(a.id, a.name, a.args.toString()));
                handler.onComplete();
            }
        } catch (IOException ex) {
            log.warn("anthropic stream failed: {}", ex.getMessage());
            handler.onError(ex.getMessage());
        }
    }
    /**
     * 枚举模型：GET {@code /v1/models}，鉴权与 Messages API 一致（x-api-key + anthropic-version）。
     * 失败只记日志并返回空列表。
     */
    @Override
    public List<ModelInfo> listModels() {
        Request.Builder rb = new Request.Builder().url(baseUrl + "/v1/models").get();
        if (apiKey != null && !apiKey.isEmpty()) {
            rb.addHeader("x-api-key", apiKey);
        }
        rb.addHeader("anthropic-version", anthropicVersion);
        List<ModelInfo> out = new ArrayList<ModelInfo>();
        try (Response r = client.newCall(rb.build()).execute()) {
            if (!r.isSuccessful()) {
                log.warn("anthropic list models http {}", r.code());
                return out;
            }
            JsonNode data = mapper.readTree(bodyOf(r)).get("data");
            if (data == null || !data.isArray()) {
                return out;
            }
            for (JsonNode item : data) {
                String id = item.path("id").asText(null);
                if (id == null || id.isEmpty()) {
                    continue;
                }
                String display = item.path("display_name").asText(null);
                out.add(ModelInfo.builder()
                        .id(id)
                        .name(display == null || display.isEmpty() ? id : display)
                        .ownedBy("anthropic")
                        .type(ModelType.LLM)
                        .build());
            }
        } catch (IOException ex) {
            log.warn("anthropic list models failed: {}", ex.getMessage());
        }
        return out;
    }

    private Map<String, Object> buildBody(LlmRequest request) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("model", request.getModel() != null ? request.getModel() : defaultModel);
        payload.put("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : 4096);
        StringBuilder sys = new StringBuilder();
        List<Object> msgs = new ArrayList<Object>();
        for (ChatMessage m : request.getMessages()) {
            if (m.getRole() == ChatRole.SYSTEM) {
                if (sys.length() > 0) sys.append("\n");
                if (m.getContent() != null) sys.append(m.getContent());
            } else if (m.getRole() == ChatRole.ASSISTANT) {
                msgs.add(assistantToWire(m));
            } else if (m.getRole() == ChatRole.TOOL) {
                Map<String, Object> um = new LinkedHashMap<String, Object>();
                um.put("role", "user");
                Map<String, Object> tr = new LinkedHashMap<String, Object>();
                tr.put("type", "tool_result");
                tr.put("tool_use_id", m.getToolCallId());
                tr.put("content", m.getContent() == null ? "" : m.getContent());
                um.put("content", Collections.singletonList(tr));
                msgs.add(um);
            } else {
                Map<String, Object> um = new LinkedHashMap<String, Object>();
                um.put("role", "user");
                um.put("content", m.getContent() == null ? "" : m.getContent());
                msgs.add(um);
            }
        }
        if (sys.length() > 0) payload.put("system", sys.toString());
        payload.put("messages", msgs);
        if (!request.getTools().isEmpty()) {
            List<Object> tools = new ArrayList<Object>();
            for (ToolSpecification t : request.getTools()) {
                Map<String, Object> tool = new LinkedHashMap<String, Object>();
                tool.put("name", t.getName());
                tool.put("description", t.getDescription());
                tool.put("input_schema", t.getParameters() == null || t.getParameters().isEmpty() ? defaultSchema() : t.getParameters());
                tools.add(tool);
            }
            payload.put("tools", tools);
        }
        payload.put("stream", true);
        return payload;
    }
    private Map<String, Object> defaultSchema() {
        Map<String, Object> s = new LinkedHashMap<String, Object>();
        s.put("type", "object");
        s.put("properties", new LinkedHashMap<String, Object>());
        return s;
    }
    private Map<String, Object> assistantToWire(ChatMessage m) throws IOException {
        Map<String, Object> am = new LinkedHashMap<String, Object>();
        am.put("role", "assistant");
        List<Object> blocks = new ArrayList<Object>();
        if (m.getContent() != null) {
            Map<String, Object> t = new LinkedHashMap<String, Object>();
            t.put("type", "text");
            t.put("text", m.getContent());
            blocks.add(t);
        }
        for (ToolExecutionRequest c : m.getToolCalls()) {
            Map<String, Object> tu = new LinkedHashMap<String, Object>();
            tu.put("type", "tool_use");
            tu.put("id", c.getId());
            tu.put("name", c.getName());
            Object input = new LinkedHashMap<String, Object>();
            if (c.getArguments() != null && !c.getArguments().trim().isEmpty() && !"{}".equals(c.getArguments())) {
                try { input = mapper.readValue(c.getArguments(), Map.class); } catch (IOException ignored) {}
            }
            tu.put("input", input);
            blocks.add(tu);
        }
        am.put("content", blocks);
        return am;
    }
    private void dispatch(String json, LlmStreamHandler handler, Map<Integer, ToolAcc> acc) throws IOException {
        JsonNode root = mapper.readTree(json);
        String type = root.has("type") ? root.get("type").asText() : "";
        if ("message_start".equals(type)) {
            JsonNode usage = root.path("message").path("usage");
            if (usage.has("input_tokens")) { long in = usage.get("input_tokens").asLong(); handler.onUsageDetail(in, 0, in); }
        } else if ("content_block_start".equals(type)) {
            JsonNode cb = root.get("content_block");
            if (cb != null && "tool_use".equals(cb.path("type").asText())) {
                ToolAcc a = new ToolAcc();
                a.id = cb.has("id") ? cb.get("id").asText() : null;
                a.name = cb.has("name") ? cb.get("name").asText() : "";
                a.index = cb.has("index") ? cb.get("index").asInt() : acc.size();
                acc.put(a.index, a);
            }
        } else if ("content_block_delta".equals(type)) {
            JsonNode delta = root.get("delta");
            if (delta != null) {
                String dt = delta.path("type").asText();
                if ("text_delta".equals(dt) && delta.has("text")) handler.onToken(delta.get("text").asText(), null);
                else if ("input_json_delta".equals(dt) && delta.has("partial_json")) {
                    int idx = root.has("index") ? root.get("index").asInt() : 0;
                    ToolAcc a = acc.get(idx); if (a == null) { a = new ToolAcc(); a.index = idx; acc.put(idx, a); }
                    a.args.append(delta.get("partial_json").asText());
                }
            }
        } else if ("message_delta".equals(type)) {
            JsonNode u = root.path("usage");
            if (u.has("output_tokens")) handler.onUsageDetail(0, u.get("output_tokens").asLong(), u.get("output_tokens").asLong());
        } else if ("error".equals(type)) {
            handler.onError(root.path("error").path("message").asText("anthropic error"));
        }
    }
    private static String bodyOf(Response response) throws IOException {
        try (okhttp3.ResponseBody body = response.body()) {
            return body == null ? "" : body.string();
        }
    }

    private static final class ToolAcc {
        String id;
        String name = "";
        StringBuilder args = new StringBuilder();
        int index;
    }
}
