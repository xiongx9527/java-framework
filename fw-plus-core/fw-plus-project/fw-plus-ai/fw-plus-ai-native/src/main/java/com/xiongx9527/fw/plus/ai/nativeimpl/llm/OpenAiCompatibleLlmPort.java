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
import java.io.IOException;
import java.util.ArrayList;
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
 * 真实原子能力：OpenAI 兼容大模型客户端（流式 / 非流式 / 工具调用）。
 * 覆盖 OpenAI、Ollama、vLLM、Qwen(DashScope 兼容端点)、Azure OpenAI 等同族协议；
 * Azure 走部署写在 URL + api-key 头（无 Bearer）。用 okhttp3 + okhttp-sse 自研 SSE 解析。
 * 落点是 base 的 LlmPort：业务用 TracedLlmPort 装饰即可获得日志 / 计费 / 追踪。
 * 差异较大的协议（Anthropic）由 AnthropicLlmPort 单独实现。
 */
public class OpenAiCompatibleLlmPort extends AbstractLlmPort {
    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleLlmPort.class);
    public enum Protocol { OPENAI, VLLM, AZURE }
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final ObjectMapper mapper = new ObjectMapper();
    private final OkHttpClient client;
    private final String baseUrl;
    private final String apiKey;
    private final String defaultModel;
    private final Protocol protocol;
    private final String apiVersion;
    public OpenAiCompatibleLlmPort(String baseUrl, String apiKey, String defaultModel) {
        this(baseUrl, apiKey, defaultModel, Protocol.OPENAI, null);
    }
    public OpenAiCompatibleLlmPort(String baseUrl, String apiKey, String defaultModel, Protocol protocol) {
        this(baseUrl, apiKey, defaultModel, protocol, null);
    }
    public OpenAiCompatibleLlmPort(String baseUrl, String apiKey, String defaultModel, Protocol protocol, String apiVersion) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) throw new IllegalArgumentException("llm baseUrl is required");
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.apiKey = apiKey;
        this.defaultModel = defaultModel;
        this.protocol = protocol == null ? Protocol.OPENAI : protocol;
        this.apiVersion = apiVersion;
        this.client = new OkHttpClient.Builder().connectTimeout(8_000, java.util.concurrent.TimeUnit.MILLISECONDS).readTimeout(600_000, java.util.concurrent.TimeUnit.MILLISECONDS).build();
    }
    @Override
    protected void doStream(LlmRequest request, LlmStreamHandler handler) {
        if (baseUrl == null) { handler.onError("llm not configured"); return; }
        try {
            String body = mapper.writeValueAsString(buildBody(request));
            Request.Builder rb = new Request.Builder().url(resolveUrl(request)).post(RequestBody.create(JSON, body));
            if (protocol == Protocol.AZURE) { if (apiKey != null && !apiKey.isEmpty()) rb.addHeader("api-key", apiKey); }
            else { if (apiKey != null && !apiKey.isEmpty()) rb.addHeader("Authorization", "Bearer " + apiKey); }
            final CountDownLatch done = new CountDownLatch(1);
            final String[] errorBox = new String[1];
            Map<Integer, ToolAcc> acc = new LinkedHashMap<Integer, ToolAcc>();
            EventSourceListener listener = new EventSourceListener() {
                @Override public void onEvent(EventSource es, String id, String type, String data) {
                    if (data == null || data.isEmpty()) return;
                    if ("[DONE]".equals(data.trim())) return;
                    try { dispatch(data, handler, acc); } catch (IOException ex) { log.warn("llm sse parse skipped: {}", ex.getMessage()); }
                }
                @Override public void onFailure(EventSource es, Throwable t, Response r) {
                    errorBox[0] = t != null ? t.getMessage() : "llm stream failed";
                    log.warn("llm stream failed: {}", errorBox[0]);
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
    private Map<String, Object> buildBody(LlmRequest request) throws IOException {
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
        return payload;
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
    private void dispatch(String json, LlmStreamHandler handler, Map<Integer, ToolAcc> acc) throws IOException {
        JsonNode root = mapper.readTree(json);
        if (root.has("choices")) {
            JsonNode usage = root.get("usage");
            if (usage != null) {
                long input = longOr(usage, "prompt_tokens", 0);
                long output = longOr(usage, "completion_tokens", 0);
                long total = longOr(usage, "total_tokens", input + output);
                handler.onUsageDetail(input, output, total);
            }
            JsonNode choices = root.get("choices");
            if (choices.isArray() && choices.size() > 0) {
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
        }
    }
    private static long longOr(JsonNode node, String field, long dft) {
        JsonNode v = node.get(field);
        return v != null && v.isNumber() ? v.asLong() : dft;
    }
    private static final class ToolAcc {
        String id;
        String name = "";
        StringBuilder args = new StringBuilder();
    }
}
