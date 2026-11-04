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
import com.xiongx9527.fw.plus.ai.base.model.content.ContentPart;
import com.xiongx9527.fw.plus.ai.base.model.content.ImageContent;
import com.xiongx9527.fw.plus.ai.base.model.content.TextContent;
import com.xiongx9527.fw.plus.ai.base.tool.ToolChoice;
import com.xiongx9527.fw.plus.ai.base.tool.ToolExecutionRequest;
import com.xiongx9527.fw.plus.ai.base.tool.ToolSpecification;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Base64;
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

/** Google Gemini 原生 generateContent / streamGenerateContent 协议端口。 */
public class GeminiLlmPort extends AbstractLlmPort {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final ObjectMapper mapper = new ObjectMapper();
    private final OkHttpClient client = new OkHttpClient.Builder().readTimeout(600, TimeUnit.SECONDS).build();
    private final String baseUrl;
    private final String apiKey;
    private final String defaultModel;
    private final String apiVersion;

    public GeminiLlmPort(String baseUrl, String apiKey, String defaultModel) { this(baseUrl, apiKey, defaultModel, null); }

    public GeminiLlmPort(String baseUrl, String apiKey, String defaultModel, String apiVersion) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) throw new IllegalArgumentException("llm baseUrl is required");
        this.baseUrl = baseUrl.replaceAll("/+$", ""); this.apiKey = apiKey; this.defaultModel = defaultModel;
        this.apiVersion = apiVersion == null || apiVersion.trim().isEmpty() ? "v1beta" : apiVersion.trim().replaceAll("^/+|/+$", "");
    }

    @Override public LlmCapabilities capabilities() {
        return LlmCapabilities.builder().streaming(true).toolCalling(true).structuredOutput(true)
                .imageInput(true).audioInput(true).fileInput(true).usage(true).build();
    }

    @Override protected void doStream(final LlmRequest request, final LlmStreamHandler handler) {
        String model = request.getModel() == null ? defaultModel : request.getModel();
        if (model == null || model.trim().isEmpty()) { handler.onError("Gemini model is required"); return; }
        try {
            String url = endpoint(model, true);
            Request.Builder rb = new Request.Builder().url(url).post(RequestBody.create(JSON, mapper.writeValueAsString(buildBody(request))));
            if (apiKey != null && !apiKey.isEmpty()) rb.header("x-goog-api-key", apiKey);
            final CountDownLatch done = new CountDownLatch(1);
            EventSourceListener listener = new EventSourceListener() {
                @Override public void onEvent(EventSource source, String id, String type, String data) {
                    if (data == null || data.trim().isEmpty() || "[DONE]".equals(data.trim())) return;
                    try { dispatch(data, handler); } catch (IOException e) { handler.onError(e.getMessage()); }
                }
                @Override public void onFailure(EventSource source, Throwable t, Response response) {
                    handler.onError(t == null ? "Gemini stream failed" : t.getMessage()); done.countDown();
                }
                @Override public void onClosed(EventSource source) { done.countDown(); }
            };
            EventSources.createFactory(client).newEventSource(rb.build(), listener);
            done.await(600, TimeUnit.SECONDS);
            handler.onComplete();
        } catch (Exception e) { handler.onError(e.getMessage() == null ? "Gemini request failed" : e.getMessage()); }
    }

    private Map<String, Object> buildBody(LlmRequest request) {
        List<Object> contents = new ArrayList<Object>();
        List<Object> systemParts = new ArrayList<Object>();
        for (ChatMessage message : request.getMessages()) {
            if (message.getRole() == ChatRole.SYSTEM) { addParts(systemParts, message.getParts()); continue; }
            Map<String, Object> content = new LinkedHashMap<String, Object>();
            content.put("role", message.getRole() == ChatRole.ASSISTANT ? "model" : "user");
            List<Object> parts = new ArrayList<Object>();
            addParts(parts, message.getParts());
            if (message.hasToolCalls()) for (ToolExecutionRequest call : message.getToolCalls()) {
                Map<String, Object> fc = new LinkedHashMap<String, Object>(); fc.put("name", call.getName());
                try { fc.put("args", mapper.readValue(call.getArguments(), Map.class)); }
                catch (Exception e) { fc.put("args", Collections.emptyMap()); }
                Map<String, Object> part = new LinkedHashMap<String, Object>(); part.put("functionCall", fc); parts.add(part);
            }
            if (message.getRole() == ChatRole.TOOL) {
                Map<String, Object> fr = new LinkedHashMap<String, Object>(); fr.put("name", message.getName());
                fr.put("response", Collections.<String, Object>singletonMap("content", message.getContent()));
                Map<String, Object> part = new LinkedHashMap<String, Object>(); part.put("functionResponse", fr); parts.add(part);
            }
            content.put("parts", parts); contents.add(content);
        }
        Map<String, Object> body = new LinkedHashMap<String, Object>(request.getExtraParams());
        body.put("contents", contents);
        if (!systemParts.isEmpty()) body.put("systemInstruction", Collections.<String, Object>singletonMap("parts", systemParts));
        Map<String, Object> generation = new LinkedHashMap<String, Object>();
        if (request.getTemperature() != null) generation.put("temperature", request.getTemperature());
        if (request.getTopP() != null) generation.put("topP", request.getTopP());
        if (request.getMaxTokens() != null) generation.put("maxOutputTokens", request.getMaxTokens());
        if (request.getSeed() != null) generation.put("seed", request.getSeed());
        if (!generation.isEmpty()) body.put("generationConfig", generation);
        if (!request.getTools().isEmpty()) {
            List<Object> declarations = new ArrayList<Object>();
            for (ToolSpecification spec : request.getTools()) {
                Map<String, Object> declaration = new LinkedHashMap<String, Object>(); declaration.put("name", spec.getName());
                declaration.put("description", spec.getDescription()); declaration.put("parameters", spec.getParameters()); declarations.add(declaration);
            }
            body.put("tools", Collections.<Object>singletonList(Collections.<String, Object>singletonMap("functionDeclarations", declarations)));
            ToolChoice choice = request.getToolChoice();
            if (choice != null) {
                String mode = choice.getMode() == ToolChoice.Mode.NONE ? "NONE" :
                        choice.getMode() == ToolChoice.Mode.REQUIRED ? "ANY" : "AUTO";
                Map<String, Object> config = new LinkedHashMap<String, Object>(); config.put("mode", mode);
                if (choice.getMode() == ToolChoice.Mode.NAMED) config.put("allowedFunctionNames", Collections.singletonList(choice.getName()));
                body.put("toolConfig", Collections.<String, Object>singletonMap("functionCallingConfig", config));
            }
        }
        return body;
    }

    private void addParts(List<Object> target, List<ContentPart> source) {
        for (ContentPart part : source) {
            if (part instanceof TextContent) target.add(Collections.<String, Object>singletonMap("text", part.text()));
            else if (part instanceof ImageContent) {
                ImageContent image = (ImageContent) part;
                Map<String, Object> data = new LinkedHashMap<String, Object>();
                if (image.isUrl()) { data.put("fileUri", image.getUrl()); data.put("mimeType", image.getMimeType() == null ? "image/jpeg" : image.getMimeType()); }
                else if (image.getData() != null) { data.put("mimeType", image.getMimeType() == null ? "image/jpeg" : image.getMimeType()); data.put("data", Base64.getEncoder().encodeToString(image.getData())); }
                target.add(Collections.<String, Object>singletonMap("inlineData", data));
            }
        }
    }

    private void dispatch(String json, LlmStreamHandler handler) throws IOException {
        JsonNode root = mapper.readTree(json);
        JsonNode candidates = root.path("candidates");
        if (candidates.isArray() && candidates.size() > 0) {
            for (JsonNode part : candidates.get(0).path("content").path("parts")) {
                if (part.has("text")) handler.onToken(part.path("text").asText(), null);
                if (part.has("functionCall")) {
                    JsonNode call = part.path("functionCall");
                    handler.onToolCall(ToolExecutionRequest.of(call.path("name").asText(), mapper.writeValueAsString(call.path("args"))));
                }
            }
        }
        JsonNode usage = root.path("usageMetadata");
        if (!usage.isMissingNode()) handler.onUsageDetail(usage.path("promptTokenCount").asLong(),
                usage.path("candidatesTokenCount").asLong(), usage.path("totalTokenCount").asLong());
        if (root.has("error")) handler.onError(root.path("error").path("message").asText("Gemini API error"));
    }

    private String endpoint(String model, boolean stream) throws IOException {
        String root = apiRoot();
        String encoded = URLEncoder.encode(model.replaceFirst("^models/", ""), "UTF-8");
        return root + "/models/" + encoded + (stream ? ":streamGenerateContent?alt=sse" : ":generateContent");
    }

    private String apiRoot() {
        if (baseUrl.endsWith("/" + apiVersion)) return baseUrl;
        String root = baseUrl.endsWith("/v1") ? baseUrl.substring(0, baseUrl.length() - 3) : baseUrl;
        return root + "/" + apiVersion;
    }

    @Override public List<ModelInfo> listModels() {
        Request.Builder rb = new Request.Builder().url(apiRoot() + "/models").get();
        if (apiKey != null && !apiKey.isEmpty()) rb.header("x-goog-api-key", apiKey);
        List<ModelInfo> result = new ArrayList<ModelInfo>();
        try (Response response = client.newCall(rb.build()).execute()) {
            if (!response.isSuccessful() || response.body() == null) return result;
            for (JsonNode node : mapper.readTree(response.body().string()).path("models")) {
                boolean supports = false;
                for (JsonNode method : node.path("supportedGenerationMethods")) if ("generateContent".equals(method.asText())) supports = true;
                if (!supports) continue;
                String name = node.path("name").asText(); String id = name.replaceFirst("^models/", "");
                result.add(ModelInfo.builder().id(id).name(node.path("displayName").asText(id)).ownedBy("google").type(ModelType.LLM).build());
            }
        } catch (IOException ignored) { }
        return result;
    }

    @Override public ModelValidation validateModel(String model) { return validateModelWithCompletion(model); }
}
