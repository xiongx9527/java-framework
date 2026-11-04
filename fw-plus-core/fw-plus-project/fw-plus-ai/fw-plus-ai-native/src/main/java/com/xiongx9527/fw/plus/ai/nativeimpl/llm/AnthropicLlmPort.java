package com.xiongx9527.fw.plus.ai.nativeimpl.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.base.model.AbstractLlmPort;
import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmStreamHandler;
import com.xiongx9527.fw.plus.ai.base.tool.ToolExecutionRequest;
import com.xiongx9527.fw.plus.ai.base.tool.ToolSpecification;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 真实原子能力：Anthropic（Claude）Messages API 客户端（流式 / 非流式 / 工具调用）。
 * 与 OpenAI 协议差异较大（鉴权用 x-api-key、请求把 system 提到顶层、响应用 content_block_delta、
 * 工具以 tool_use 块返回），故单独实现，但仍落点 base 的 LlmPort，与 OpenAiCompatibleLlmPort 可互换。
 */
public class AnthropicLlmPort extends AbstractLlmPort {
    private static final Logger log = LoggerFactory.getLogger(AnthropicLlmPort.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl;
    private final String apiKey;
    private final String defaultModel;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    private final String anthropicVersion;
    public AnthropicLlmPort(String baseUrl, String apiKey, String defaultModel) {
        this(baseUrl, apiKey, defaultModel, 8000, 600000, "2023-06-01");
    }
    public AnthropicLlmPort(String baseUrl, String apiKey, String defaultModel, int connectTimeoutMillis, int readTimeoutMillis, String anthropicVersion) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) throw new IllegalArgumentException("llm baseUrl is required");
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.apiKey = apiKey;
        this.defaultModel = defaultModel;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
        this.anthropicVersion = anthropicVersion == null ? "2023-06-01" : anthropicVersion;
    }
    @Override
    protected void doStream(LlmRequest request, LlmStreamHandler handler) {
        if (baseUrl == null) { handler.onError("llm not configured"); return; }
        try {
            String body = buildBody(request);
            HttpURLConnection conn = openConnection();
            try (OutputStream os = conn.getOutputStream()) { os.write(body.getBytes(StandardCharsets.UTF_8)); }
            int code = conn.getResponseCode();
            if (code != 200) {
                String err = readError(conn);
                handler.onError("anthropic http " + code + (err.isEmpty() ? "" : ": " + err));
                return;
            }
            parseSse(conn.getInputStream(), handler);
        } catch (Exception ex) {
            log.warn("anthropic stream failed: {}", ex.getMessage());
            handler.onError(ex.getMessage());
        }
    }
    private HttpURLConnection openConnection() throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + "/v1/messages").openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(connectTimeoutMillis);
        conn.setReadTimeout(readTimeoutMillis);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Accept", "text/event-stream");
        if (apiKey != null && !apiKey.isEmpty()) conn.setRequestProperty("x-api-key", apiKey);
        conn.setRequestProperty("anthropic-version", anthropicVersion);
        return conn;
    }
    private String buildBody(LlmRequest request) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("model", request.getModel() != null ? request.getModel() : defaultModel);
        payload.put("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : 4096);
        StringBuilder sys = new StringBuilder();
        List<Object> msgs = new ArrayList<Object>();
        for (ChatMessage m : request.getMessages()) {
            if (m.getRole() == com.xiongx9527.fw.plus.ai.base.model.ChatRole.SYSTEM) {
                if (sys.length() > 0) sys.append("\n");
                if (m.getContent() != null) sys.append(m.getContent());
            } else if (m.getRole() == com.xiongx9527.fw.plus.ai.base.model.ChatRole.ASSISTANT) {
                msgs.add(assistantToWire(m));
            } else if (m.getRole() == com.xiongx9527.fw.plus.ai.base.model.ChatRole.TOOL) {
                Map<String, Object> um = new LinkedHashMap<String, Object>();
                um.put("role", "user");
                Map<String, Object> tr = new LinkedHashMap<String, Object>();
                tr.put("type", "tool_result");
                tr.put("tool_use_id", m.getToolCallId());
                tr.put("content", m.getContent() == null ? "" : m.getContent());
                um.put("content", java.util.Collections.singletonList(tr));
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
        return mapper.writeValueAsString(payload);
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
    private void parseSse(InputStream in, LlmStreamHandler handler) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        String event = "";
        long outputTokens = 0;
        Map<String, ToolAcc> acc = new LinkedHashMap<String, ToolAcc>();
        String pendingData = null;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("event:")) { event = line.substring(6).trim(); continue; }
            if (line.startsWith("data:")) { pendingData = line.substring(5).trim(); }
            else { pendingData = null; continue; }
            if (pendingData == null || pendingData.isEmpty()) continue;
            try {
                JsonNode root = mapper.readTree(pendingData);
                String type = root.has("type") ? root.get("type").asText() : event;
                if ("message_start".equals(type)) {
                    JsonNode usage = root.path("message").path("usage");
                    if (usage.has("input_tokens")) handler.onUsageDetail(usage.get("input_tokens").asLong(), 0, usage.get("input_tokens").asLong());
                } else if ("content_block_start".equals(type)) {
                    JsonNode cb = root.get("content_block");
                    if (cb != null && "tool_use".equals(cb.path("type").asText())) {
                        ToolAcc a = new ToolAcc();
                        a.id = cb.has("id") ? cb.get("id").asText() : null;
                        a.name = cb.has("name") ? cb.get("name").asText() : "";
                        acc.put(a.id != null ? a.id : "x", a);
                    }
                } else if ("content_block_delta".equals(type)) {
                    JsonNode delta = root.get("delta");
                    if (delta != null) {
                        String dt = delta.path("type").asText();
                        if ("text_delta".equals(dt) && delta.has("text")) handler.onToken(delta.get("text").asText(), null);
                        else if ("input_json_delta".equals(dt) && delta.has("partial_json")) a_append(acc, root, delta.get("partial_json").asText());
                    }
                } else if ("message_delta".equals(type)) {
                    JsonNode u = root.path("usage");
                    if (u.has("output_tokens")) outputTokens = u.get("output_tokens").asLong();
                } else if ("error".equals(type) || "ping".equals(type)) {
                    if ("error".equals(type)) handler.onError(root.path("error").path("message").asText("anthropic error"));
                }
            } catch (IOException ex) {
                log.warn("anthropic sse parse skipped: {}", ex.getMessage());
            }
        }
        handler.onUsageDetail(0, outputTokens, outputTokens);
        for (ToolAcc a : acc.values()) handler.onToolCall(new ToolExecutionRequest(a.id, a.name, a.args.toString()));
        handler.onComplete();
    }
    private void a_append(Map<String, ToolAcc> acc, JsonNode root, String partial) {
        int idx = root.has("index") ? root.get("index").asInt() : 0;
        ToolAcc a = null;
        for (ToolAcc v : acc.values()) { a = v; break; }
        if (a == null) { a = new ToolAcc(); a.id = "x" + idx; acc.put(a.id, a); }
        a.args.append(partial);
    }
    private static String readError(HttpURLConnection conn) {
        try {
            InputStream e = conn.getErrorStream();
            if (e == null) return "";
            StringBuilder sb = new StringBuilder();
            int c;
            while ((c = e.read()) != -1) sb.append((char) c);
            return sb.toString().replace("\n", " ").replace("\r", " ");
        } catch (IOException ex) { return ""; }
    }
    private static final class ToolAcc {
        String id;
        String name = "";
        StringBuilder args = new StringBuilder();
    }
}
