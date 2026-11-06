package com.xiongx9527.fw.plus.ai.nativeimpl.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.base.model.AbstractLlmPort;
import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.ChatRole;
import com.xiongx9527.fw.plus.ai.base.model.LlmCapabilities;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

/** OpenAI Responses API 专用端口；不把其事件模型伪装成 Chat Completions。 */
public class OpenAiResponsesLlmPort extends AbstractLlmPort {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final ObjectMapper mapper = new ObjectMapper();
    private final OkHttpClient client;
    private final String baseUrl;
    private final String apiKey;
    private final String defaultModel;

    public OpenAiResponsesLlmPort(String baseUrl, String apiKey, String defaultModel) {
        this(baseUrl, apiKey, defaultModel, null, null);
    }

    public OpenAiResponsesLlmPort(String baseUrl, String apiKey, String defaultModel,
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

    @Override public LlmCapabilities capabilities() {
        return LlmCapabilities.builder().streaming(true).toolCalling(true).structuredOutput(true)
                .imageInput(true).reasoning(true).usage(true).build();
    }

    @Override protected void doStream(final LlmRequest request, final LlmStreamHandler handler) {
        try {
            String json = mapper.writeValueAsString(buildBody(request));
            Request.Builder rb = new Request.Builder().url(LlmUrlUtil.normalizeBaseUrl(baseUrl) + "/v1/responses")
                    .post(RequestBody.create(JSON, json));
            if (apiKey != null && !apiKey.isEmpty()) rb.header("Authorization", "Bearer " + apiKey);
            final CountDownLatch done = new CountDownLatch(1);
            final Map<String, ToolAcc> tools = new LinkedHashMap<String, ToolAcc>();
            EventSourceListener listener = new EventSourceListener() {
                @Override public void onEvent(EventSource source, String id, String type, String data) {
                    if (data == null || data.trim().isEmpty() || "[DONE]".equals(data.trim())) return;
                    handler.onUnmappedRawEvent(data);
                    try { dispatch(type, data, handler, tools); } catch (IOException e) { handler.onError(e.getMessage()); }
                }
                @Override public void onFailure(EventSource source, Throwable t, Response response) {
                    handler.onError(t == null ? "OpenAI Responses stream failed" : t.getMessage()); done.countDown();
                }
                @Override public void onClosed(EventSource source) { done.countDown(); }
            };
            EventSources.createFactory(client).newEventSource(rb.build(), listener);
            done.await(600, TimeUnit.SECONDS);
            for (ToolAcc acc : tools.values()) {
                ToolExecutionRequest call = new ToolExecutionRequest(acc.callId, acc.name, acc.args.toString());
                handler.onCompleteToolCall(call);
                handler.onToolCall(call);
            }
            handler.onFinish("STOP");
            handler.onComplete();
        } catch (Exception e) {
            handler.onError(e.getMessage() == null ? "OpenAI Responses request failed" : e.getMessage());
        }
    }

    private Map<String, Object> buildBody(LlmRequest request) {
        Map<String, Object> body = new LinkedHashMap<String, Object>(request.getExtraParams());
        body.put("model", request.getModel() == null ? defaultModel : request.getModel());
        List<Object> input = new ArrayList<Object>();
        StringBuilder instructions = new StringBuilder();
        for (ChatMessage message : request.getMessages()) {
            if (message.getRole() == ChatRole.SYSTEM) {
                if (instructions.length() > 0) instructions.append('\n');
                if (message.getContent() != null) instructions.append(message.getContent());
                continue;
            }
            if (message.getRole() == ChatRole.TOOL) {
                Map<String, Object> output = new LinkedHashMap<String, Object>();
                output.put("type", "function_call_output");
                output.put("call_id", message.getToolCallId());
                output.put("output", message.getContent() == null ? "" : message.getContent());
                input.add(output);
                continue;
            }
            if (message.hasToolCalls()) {
                for (ToolExecutionRequest call : message.getToolCalls()) {
                    Map<String, Object> item = new LinkedHashMap<String, Object>();
                    item.put("type", "function_call"); item.put("call_id", call.getId());
                    item.put("name", call.getName()); item.put("arguments", call.getArguments()); input.add(item);
                }
            }
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("role", message.getRole() == ChatRole.ASSISTANT ? "assistant" : "user");
            item.put("content", Collections.singletonList(textPart(message.getContent())));
            input.add(item);
        }
        body.put("input", input);
        if (instructions.length() > 0) body.put("instructions", instructions.toString());
        if (request.getTemperature() != null) body.put("temperature", request.getTemperature());
        if (request.getTopP() != null) body.put("top_p", request.getTopP());
        if (request.getMaxTokens() != null) body.put("max_output_tokens", request.getMaxTokens());
        if (!request.getStopSequences().isEmpty()) body.put("stop", request.getStopSequences());
        if (request.getResponseFormat() != null) {
            Map<String, Object> text = new LinkedHashMap<String, Object>();
            text.put("format", request.getResponseFormat());
            body.put("text", text);
        }
        if (!request.getTools().isEmpty()) {
            List<Object> tools = new ArrayList<Object>();
            for (ToolSpecification spec : request.getTools()) {
                Map<String, Object> tool = new LinkedHashMap<String, Object>(); tool.put("type", "function");
                tool.put("name", spec.getName()); tool.put("description", spec.getDescription());
                tool.put("parameters", spec.getParameters().isEmpty() ? defaultSchema() : spec.getParameters()); tools.add(tool);
            }
            body.put("tools", tools);
            ToolChoice choice = request.getToolChoice();
            if (choice != null) body.put("tool_choice", choice.getMode() == ToolChoice.Mode.NAMED
                    ? namedToolChoice(choice.getName()) : choice.getMode().name().toLowerCase());
        }
        body.put("stream", true);
        return body;
    }

    private Map<String, Object> textPart(String text) {
        Map<String, Object> part = new LinkedHashMap<String, Object>();
        part.put("type", "input_text"); part.put("text", text == null ? "" : text); return part;
    }

    private Map<String, Object> namedToolChoice(String name) {
        Map<String, Object> choice = new LinkedHashMap<String, Object>();
        choice.put("type", "function"); choice.put("name", name); return choice;
    }

    private Map<String, Object> defaultSchema() {
        Map<String, Object> schema = new LinkedHashMap<String, Object>(); schema.put("type", "object");
        schema.put("properties", Collections.emptyMap()); return schema;
    }

    private void dispatch(String event, String json, LlmStreamHandler handler, Map<String, ToolAcc> tools) throws IOException {
        JsonNode root = mapper.readTree(json);
        String type = root.path("type").asText(event == null ? "" : event);
        if ("response.output_text.delta".equals(type)) {
            handler.onToken(root.path("delta").asText(), null);
        } else if ("response.reasoning_summary_text.delta".equals(type) || "response.reasoning_text.delta".equals(type)) {
            handler.onToken(null, root.path("delta").asText());
        } else         if ("response.output_item.added".equals(type)) {
            JsonNode item = root.path("item");
            if ("function_call".equals(item.path("type").asText())) {
                ToolAcc acc = new ToolAcc(); acc.callId = item.path("call_id").asText(item.path("id").asText());
                acc.name = item.path("name").asText(); tools.put(item.path("id").asText(acc.callId), acc);
                handler.onPartialToolCall(acc.callId, acc.name, "");
            }
        } else if ("response.function_call_arguments.delta".equals(type)) {
            String id = root.path("item_id").asText(); ToolAcc acc = tools.get(id);
            if (acc == null) { acc = new ToolAcc(); tools.put(id, acc); }
            String delta = root.path("delta").asText();
            acc.args.append(delta);
            handler.onPartialToolCall(acc.callId, acc.name, delta);
        } else if ("response.completed".equals(type)) {
            JsonNode usage = root.path("response").path("usage");
            if (!usage.isMissingNode()) handler.onUsageDetail(usage.path("input_tokens").asLong(),
                    usage.path("output_tokens").asLong(), usage.path("total_tokens").asLong());
        } else if ("error".equals(type) || "response.failed".equals(type)) {
            handler.onError(root.path("error").path("message").asText("OpenAI Responses error"));
        }
    }

    @Override public List<ModelInfo> listModels() {
        Request.Builder rb = new Request.Builder().url(LlmUrlUtil.normalizeBaseUrl(baseUrl) + "/v1/models").get();
        if (apiKey != null && !apiKey.isEmpty()) rb.header("Authorization", "Bearer " + apiKey);
        List<ModelInfo> models = new ArrayList<ModelInfo>();
        try (Response response = client.newCall(rb.build()).execute()) {
            if (!response.isSuccessful() || response.body() == null) return models;
            JsonNode data = mapper.readTree(response.body().string()).path("data");
            for (JsonNode item : data) models.add(ModelInfo.builder().id(item.path("id").asText())
                    .name(item.path("id").asText()).ownedBy(item.path("owned_by").asText(null)).type(ModelType.LLM).build());
        } catch (IOException ignored) { }
        return models;
    }

    @Override public ModelValidation validateModel(String model) { return validateModelWithCompletion(model); }
    private static final class ToolAcc { String callId; String name = ""; StringBuilder args = new StringBuilder(); }
}
