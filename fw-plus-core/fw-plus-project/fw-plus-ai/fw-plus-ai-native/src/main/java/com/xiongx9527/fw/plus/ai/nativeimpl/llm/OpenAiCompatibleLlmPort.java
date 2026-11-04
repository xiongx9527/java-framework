package com.xiongx9527.fw.plus.ai.nativeimpl.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.base.model.AbstractLlmPort;
import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmStreamHandler;
import com.xiongx9527.fw.plus.ai.base.tool.ToolChoice;
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
 * 真实原子能力：OpenAI 兼容大模型客户端（流式 / 非流式 / 工具调用）。
 * 覆盖 OpenAI、Ollama、vLLM 等同一套 /v1/chat/completions 协议的网关；
 * 通过 {@link Protocol#AZURE} 兼容 Azure OpenAI（部署写在 URL、鉴权用 api-key 头、无 Bearer）。
 * 参照 product 的 ChatModelClient 直连实现，用 JDK HttpURLConnection 自研 SSE 解析，不引入额外模型 SDK。
 * 落点是 base 的 LlmPort：业务用 TracedLlmPort 装饰即可获得日志 / 计费 / 追踪。
 * 差异较大的协议（如 Anthropic）由 {@code AnthropicLlmPort} 单独实现。
 */
public class OpenAiCompatibleLlmPort extends AbstractLlmPort {
    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleLlmPort.class);
    public enum Protocol { OPENAI, VLLM, AZURE }
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl;
    private final String apiKey;
    private final String defaultModel;
    private final Protocol protocol;
    private final String apiVersion;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    public OpenAiCompatibleLlmPort(String baseUrl, String apiKey, String defaultModel) {
        this(baseUrl, apiKey, defaultModel, Protocol.OPENAI, null, 8000, 600000);
    }
    public OpenAiCompatibleLlmPort(String baseUrl, String apiKey, String defaultModel, Protocol protocol) {
        this(baseUrl, apiKey, defaultModel, protocol, null, 8000, 600000);
    }
    public OpenAiCompatibleLlmPort(String baseUrl, String apiKey, String defaultModel, Protocol protocol, String apiVersion) {
        this(baseUrl, apiKey, defaultModel, protocol, apiVersion, 8000, 600000);
    }
    private OpenAiCompatibleLlmPort(String baseUrl, String apiKey, String defaultModel, Protocol protocol, String apiVersion, int connectTimeoutMillis, int readTimeoutMillis) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) throw new IllegalArgumentException("llm baseUrl is required");
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.apiKey = apiKey;
        this.defaultModel = defaultModel;
        this.protocol = protocol == null ? Protocol.OPENAI : protocol;
        this.apiVersion = apiVersion;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
    }
    @Override
    protected void doStream(LlmRequest request, LlmStreamHandler handler) {
        if (baseUrl == null) { handler.onError("llm not configured"); return; }
        try {
            String body = buildBody(request);
            HttpURLConnection conn = openConnection(resolveUrl(request));
            try (OutputStream os = conn.getOutputStream()) { os.write(body.getBytes(StandardCharsets.UTF_8)); }
            int code = conn.getResponseCode();
            if (code != 200) {
                String err = readError(conn);
                handler.onError("llm http " + code + (err.isEmpty() ? "" : ": " + err));
                return;
            }
            parseSse(conn.getInputStream(), handler);
        } catch (Exception ex) {
            log.warn("llm stream failed: {}", ex.getMessage());
            handler.onError(ex.getMessage());
        }
    }
    private String resolveUrl(LlmRequest request) {
        if (protocol == Protocol.AZURE) {
            String deployment = request.getModel() != null ? request.getModel() : defaultModel;
            String v = apiVersion != null ? apiVersion : "2024-02-15-preview";
            return baseUrl + "/openai/deployments/" + deployment + "/chat/completions?api-version=" + v;
        }
        return baseUrl + "/chat/completions";
    }
    private HttpURLConnection openConnection(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(connectTimeoutMillis);
        conn.setReadTimeout(readTimeoutMillis);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        if (protocol == Protocol.AZURE) {
            if (apiKey != null && !apiKey.isEmpty()) conn.setRequestProperty("api-key", apiKey);
        } else {
            if (apiKey != null && !apiKey.isEmpty()) conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        }
        return conn;
    }
    private String buildBody(LlmRequest request) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("model", request.getModel() != null ? request.getModel() : defaultModel);
        List<Object> msgs = new ArrayList<Object>();
        for (ChatMessage m : request.getMessages()) msgs.add(toWire(m));
        payload.put("messages", msgs);
        if (request.getTemperature() != null) payload.put("temperature", request.getTemperature());
        if (request.getTopP() != null) payload.put("top_p", request.getTopP());
        if (request.getMaxTokens() != null) payload.put("max_tokens", request.getMaxTokens());
        if (!request.getTools().isEmpty()) {
            List<Object> tools = new ArrayList<Object>();
            for (ToolSpecification t : request.getTools()) {
                Map<String, Object> fn = new LinkedHashMap<String, Object>();
                fn.put("name", t.getName());
                fn.put("description", t.getDescription());
                fn.put("parameters", t.getParameters());
                Map<String, Object> tool = new LinkedHashMap<String, Object>();
                tool.put("type", "function");
                tool.put("function", fn);
                tools.add(tool);
            }
            payload.put("tools", tools);
            payload.put("tool_choice", toWire(request.getToolChoice()));
        }
        payload.put("stream", true);
        return mapper.writeValueAsString(payload);
    }
    private Map<String, Object> toWire(ChatMessage m) {
        Map<String, Object> o = new LinkedHashMap<String, Object>();
        o.put("role", m.getRole().wireName());
        if (m.getContent() != null) o.put("content", m.getContent());
        if (m.getToolCallId() != null) o.put("tool_call_id", m.getToolCallId());
        if (m.hasToolCalls()) {
            List<Object> calls = new ArrayList<Object>();
            for (ToolExecutionRequest c : m.getToolCalls()) {
                Map<String, Object> fn = new LinkedHashMap<String, Object>();
                fn.put("name", c.getName());
                fn.put("arguments", c.getArguments() == null ? "{}" : c.getArguments());
                Map<String, Object> call = new LinkedHashMap<String, Object>();
                call.put("id", c.getId());
                call.put("type", "function");
                call.put("function", fn);
                calls.add(call);
            }
            o.put("tool_calls", calls);
        }
        return o;
    }
    private Object toWire(ToolChoice choice) {
        if (choice == null) return "auto";
        switch (choice.getMode()) {
            case NONE: return "none";
            case REQUIRED: return "required";
            case NAMED:
                Map<String, Object> f = new LinkedHashMap<String, Object>();
                f.put("name", choice.getName());
                Map<String, Object> t = new LinkedHashMap<String, Object>();
                t.put("type", "function");
                t.put("function", f);
                return t;
            case AUTO:
            default: return "auto";
        }
    }
    private void parseSse(InputStream in, LlmStreamHandler handler) throws IOException {
        Map<Integer, ToolAcc> acc = new LinkedHashMap<Integer, ToolAcc>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        StringBuilder data = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("data:")) {
                String chunk = line.substring(5).trim();
                if ("[DONE]".equals(chunk)) continue;
                data.append(chunk);
            } else {
                if (data.length() > 0) { dispatch(data.toString(), handler, acc); data.setLength(0); }
            }
        }
        if (data.length() > 0) dispatch(data.toString(), handler, acc);
        for (ToolAcc a : acc.values()) handler.onToolCall(new ToolExecutionRequest(a.id, a.name, a.args.toString()));
        handler.onComplete();
    }
    private void dispatch(String json, LlmStreamHandler handler, Map<Integer, ToolAcc> acc) {
        try {
            JsonNode root = mapper.readTree(json);
            JsonNode usage = root.get("usage");
            if (usage != null) {
                long input = longOr(usage, "prompt_tokens", 0);
                long output = longOr(usage, "completion_tokens", 0);
                long total = longOr(usage, "total_tokens", input + output);
                handler.onUsageDetail(input, output, total);
            }
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode delta = choices.get(0).get("delta");
                if (delta != null) {
                    if (delta.has("content")) { JsonNode c = delta.get("content"); if (!c.isNull()) handler.onToken(c.asText(), null); }
                    if (delta.has("reasoning_content")) { JsonNode r = delta.get("reasoning_content"); if (!r.isNull()) handler.onToken(null, r.asText()); }
                    else if (delta.has("reasoning")) { JsonNode r = delta.get("reasoning"); if (!r.isNull()) handler.onToken(null, r.asText()); }
                    JsonNode calls = delta.get("tool_calls");
                    if (calls != null && calls.isArray()) {
                        for (JsonNode call : calls) {
                            int idx = call.has("index") ? call.get("index").asInt() : 0;
                            ToolAcc a = acc.get(idx); if (a == null) { a = new ToolAcc(); acc.put(idx, a); }
                            if (call.has("id") && !call.get("id").isNull()) a.id = call.get("id").asText();
                            JsonNode fn = call.get("function");
                            if (fn != null) {
                                if (fn.has("name") && !fn.get("name").isNull()) a.name = fn.get("name").asText();
                                if (fn.has("arguments") && !fn.get("arguments").isNull()) a.args.append(fn.get("arguments").asText());
                            }
                        }
                    }
                }
            }
        } catch (IOException ex) {
            log.warn("llm sse parse skipped: {}", ex.getMessage());
        }
    }
    private static long longOr(JsonNode node, String field, long dft) {
        JsonNode v = node.get(field);
        return v != null && v.isNumber() ? v.asLong() : dft;
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
