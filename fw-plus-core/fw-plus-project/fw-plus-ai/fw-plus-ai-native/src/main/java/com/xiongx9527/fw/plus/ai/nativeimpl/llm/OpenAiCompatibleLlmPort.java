package com.xiongx9527.fw.plus.ai.nativeimpl.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.base.model.AbstractLlmPort;
import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmStreamHandler;
import com.xiongx9527.fw.plus.ai.base.model.ModelInfo;
import com.xiongx9527.fw.plus.ai.base.model.ModelType;
import com.xiongx9527.fw.plus.ai.base.model.ModelValidation;
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
 * 覆盖使用 OpenAI Chat Completions 协议的服务。用 okhttp3 + okhttp-sse 自研 SSE 解析。
 * 落点是 base 的 LlmPort：业务用 TracedLlmPort 装饰即可获得日志 / 计费 / 追踪。
 * 协议差异由独立端口实现；Azure OpenAI 使用 {@link AzureOpenAiLlmPort}。
 */
public class OpenAiCompatibleLlmPort extends AbstractLlmPort {
    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleLlmPort.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final ObjectMapper mapper = new ObjectMapper();
    protected final OkHttpClient client;
    protected final String baseUrl;
    protected final String apiKey;
    protected final String defaultModel;
    public OpenAiCompatibleLlmPort(String baseUrl, String apiKey, String defaultModel) {
        this(baseUrl, apiKey, defaultModel, null, null);
    }

    public OpenAiCompatibleLlmPort(String baseUrl, String apiKey, String defaultModel,
            Integer connectTimeoutMillis, Integer readTimeoutMillis) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) throw new IllegalArgumentException("llm baseUrl is required");
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.apiKey = apiKey;
        this.defaultModel = defaultModel;
        long connect = connectTimeoutMillis != null ? connectTimeoutMillis : 8_000L;
        long read = readTimeoutMillis != null ? readTimeoutMillis : 600_000L;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(connect, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(read, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build();
    }
    @Override
    public com.xiongx9527.fw.plus.ai.base.model.LlmCapabilities capabilities() {
        return com.xiongx9527.fw.plus.ai.base.model.LlmCapabilities.builder()
                .streaming(true).toolCalling(true).structuredOutput(true)
                .imageInput(true).reasoning(true).usage(true).build();
    }

    @Override
    protected void doStream(LlmRequest request, LlmStreamHandler handler) {
        if (baseUrl == null) { handler.onError("llm not configured"); return; }
        try {
            String body = mapper.writeValueAsString(buildBody(request));
            Request.Builder rb = new Request.Builder().url(resolveUrl(request)).post(RequestBody.create(JSON, body));
            addAuthentication(rb);
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
    /** 枚举模型；失败只记日志并返回空列表，不影响推理主链路。 */
    @Override
    public List<ModelInfo> listModels() {
        Request.Builder rb = new Request.Builder().url(modelsUrl()).get();
        addAuthentication(rb);
        List<ModelInfo> out = new ArrayList<ModelInfo>();
        try (Response r = client.newCall(rb.build()).execute()) {
            if (!r.isSuccessful()) {
                log.warn("llm list models http {}", r.code());
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
                out.add(ModelInfo.builder()
                        .id(id)
                        .name(id)
                        .ownedBy(item.hasNonNull("owned_by") ? item.get("owned_by").asText() : null)
                        .type(ModelType.LLM)
                        .build());
            }
        } catch (IOException ex) {
            log.warn("llm list models failed: {}", ex.getMessage());
        }
        return out;
    }

    @Override
    public ModelValidation validateModel(String model) {
        return validateModelWithCompletion(model);
    }

    protected String resolveUrl(LlmRequest request) {
        return baseUrl + "/chat/completions";
    }

    protected String modelsUrl() {
        return baseUrl + "/models";
    }

    protected void addAuthentication(Request.Builder request) {
        if (apiKey != null && !apiKey.isEmpty()) {
            request.addHeader("Authorization", "Bearer " + apiKey);
        }
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
        if (request.getSeed() != null) payload.put("seed", request.getSeed());
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
    private static String bodyOf(Response response) throws IOException {
        try (okhttp3.ResponseBody body = response.body()) {
            return body == null ? "" : body.string();
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
