package com.xiongx9527.fw.plus.ai.base.output;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.ChatRole;
import com.xiongx9527.fw.plus.ai.base.model.LlmPort;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmResponse;
import com.xiongx9527.fw.plus.ai.base.model.LlmStreamHandler;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * {@link JsonOutputPort} 的默认实现：包一个 {@link LlmPort}，在请求侧做指令 / 协议约束，
 * 在响应侧做兜底解析与纠错重试。
 *
 * <p>典型用法：</p>
 * <pre>{@code
 * JsonOutputPort json = DefaultJsonOutputPort.builder(llm)
 *     .jsonModeParam("response_format", Collections.singletonMap("type", "json_object"))
 *     .maxRetries(2)
 *     .build();
 * MyDto dto = json.completeJson(system, user, text -> parseMyDto(text));
 * }</pre>
 */
public class DefaultJsonOutputPort implements JsonOutputPort {

    /**
     * 默认 JSON 指令：要求模型只输出一个 JSON 对象或数组，不得夹带散文或 markdown 围栏。
     */
    public static final String DEFAULT_INSTRUCTION =
            "You MUST output ONLY valid JSON. Output exactly one JSON object or array.\n"
            + "Do NOT include any explanation, commentary, or markdown code fences (```).\n"
            + "Do NOT output text outside the JSON. The response must be parseable as JSON directly.";

    private static final int MAX_SNIPPET = 500;

    /** 纠错重试次数的硬上限，防止 runaway 重试。 */
    public static final int MAX_RETRIES = 5;

    private final LlmPort llmPort;

    private final String instruction;

    private final Map<String, Object> jsonModeParams;

    private final int maxRetries;

    /** 全部重试后仍无法提取时，是否再用 {@link JsonSupport#repairJson(String)} 做兜底修复（默认开启）。 */
    private final boolean repair;

    /** 兜底仍失败时的最终回退：返回其结果的文本，不抛异常（为 null 则抛 {@link JsonOutputException}）。 */
    private final Function<LlmRequest, String> fallback;

    private DefaultJsonOutputPort(Builder builder) {
        this.llmPort = builder.llmPort;
        this.instruction = builder.instruction == null ? DEFAULT_INSTRUCTION : builder.instruction;
        this.jsonModeParams = builder.jsonModeParams;
        this.maxRetries = builder.maxRetries;
        this.repair = builder.repair;
        this.fallback = builder.fallback;
    }

    public static Builder builder(LlmPort llmPort) {
        return new Builder(llmPort);
    }

    @Override
    public void streamJson(LlmRequest request, LlmStreamHandler handler) {
        LlmRequest enforced = enforce(request);
        LlmStreamHandler wrapped = handler == null ? null : new StreamingJsonHandler(handler);
        llmPort.stream(enforced, wrapped);
    }

    @Override
    public String completeJson(LlmRequest request) {
        LlmRequest enforced = enforce(request);
        String lastText = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            LlmResponse response = llmPort.complete(enforced);
            lastText = response == null ? null : response.getContent();
            String json = JsonSupport.extractJson(lastText);
            if (json != null) {
                return json;
            }
            if (attempt < maxRetries) {
                enforced = buildRequest(appendCorrection(enforced.getMessages(), lastText), enforced);
            }
        }
        // 重试仍失败：先用解析兜底（修复截断 / 多余逗号），再退回业务自定义 fallback
        if (repair) {
            String repaired = JsonSupport.repairJson(lastText);
            if (repaired != null) {
                return repaired;
            }
        }
        if (fallback != null) {
            return fallback.apply(request);
        }
        throw new JsonOutputException(
                "no parseable JSON in model output after " + (maxRetries + 1) + " attempt(s)");
    }

    /**
     * 注入 JSON 指令到 system 消息，并合并后端 JSON 模式参数。
     */
    private LlmRequest enforce(LlmRequest request) {
        List<ChatMessage> messages = new ArrayList<ChatMessage>(request.getMessages());
        boolean injected = false;
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).getRole() == ChatRole.SYSTEM) {
                String text = messages.get(i).getContent();
                String merged = (text == null ? "" : text + "\n\n") + instruction;
                messages.set(i, ChatMessage.system(merged));
                injected = true;
                break;
            }
        }
        if (!injected) {
            messages.add(0, ChatMessage.system(instruction));
        }
        return buildRequest(messages, request);
    }

    /**
     * 在对话尾部追加纠错提示，要求模型只输出 JSON 并说明上一轮为何失败。
     */
    private List<ChatMessage> appendCorrection(List<ChatMessage> messages, String badText) {
        List<ChatMessage> copy = new ArrayList<ChatMessage>(messages);
        copy.add(ChatMessage.user(correctionPrompt(badText)));
        return copy;
    }

    private static String correctionPrompt(String badText) {
        String snippet = badText == null ? "(empty)" : truncate(badText, MAX_SNIPPET);
        return "Your previous output was not valid JSON and could not be parsed:\n"
                + snippet + "\n\n"
                + "Please respond with ONLY a single valid JSON object or array, "
                + "no prose and no markdown code fences.";
    }

    /**
     * 以 source 的其它字段为蓝本，套用新的 messages 与 JSON 模式参数重建请求。
     */
    private LlmRequest buildRequest(List<ChatMessage> messages, LlmRequest source) {
        Map<String, Object> params = new LinkedHashMap<String, Object>(source.getExtraParams());
        params.putAll(jsonModeParams);
        LlmRequest.Builder b = LlmRequest.builder().messages(messages);
        b.model(source.getModel());
        if (source.getTemperature() != null) {
            b.temperature(source.getTemperature());
        }
        if (source.getTopP() != null) {
            b.topP(source.getTopP());
        }
        if (source.getMaxTokens() != null) {
            b.maxTokens(source.getMaxTokens());
        }
        b.extraParams(params);
        b.listeners(source.getListeners());
        b.tools(source.getTools());
        b.toolChoice(source.getToolChoice());
        return b.build();
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }

    /**
     * 流式 JSON 转发：剥离前导散文/围栏，仅透传 JSON 主体，闭合后停止。
     */
    private static final class StreamingJsonHandler implements LlmStreamHandler {

        private final LlmStreamHandler delegate;

        private boolean started;

        private boolean closed;

        private int depth;

        private boolean inString;

        private boolean escaped;

        StreamingJsonHandler(LlmStreamHandler delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onToken(String delta, String reasoningDelta) {
            if (delta == null || closed) {
                return;
            }
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < delta.length(); i++) {
                char c = delta.charAt(i);
                if (!started) {
                    if (c == '{' || c == '[') {
                        started = true;
                        depth = 1;
                        out.append(c);
                    }
                    // 跳过前导散文 / markdown 围栏，直到遇到首个括号
                    continue;
                }
                if (inString) {
                    out.append(c);
                    if (escaped) {
                        escaped = false;
                    } else if (c == '\\') {
                        escaped = true;
                    } else if (c == '"') {
                        inString = false;
                    }
                    continue;
                }
                if (c == '"') {
                    inString = true;
                    out.append(c);
                } else if (c == '{' || c == '[') {
                    depth++;
                    out.append(c);
                } else if (c == '}' || c == ']') {
                    depth--;
                    out.append(c);
                    if (depth == 0) {
                        closed = true;
                        break;
                    }
                } else {
                    out.append(c);
                }
            }
            if (out.length() > 0) {
                delegate.onToken(out.toString(), null);
            }
        }

        @Override
        public void onComplete() {
            delegate.onComplete();
        }

        @Override
        public void onError(String message) {
            delegate.onError(message);
        }
    }

    /**
     * 构造器：配置指令文本、后端 JSON 模式私有参数与纠错重试次数。
     */
    public static final class Builder {

        private final LlmPort llmPort;

        private String instruction;

        private final Map<String, Object> jsonModeParams = new LinkedHashMap<String, Object>();

        private int maxRetries = 1;

        private boolean repair = true;

        private Function<LlmRequest, String> fallback;

        private Builder(LlmPort llmPort) {
            if (llmPort == null) {
                throw new IllegalArgumentException("llmPort is required");
            }
            this.llmPort = llmPort;
        }

        /** 覆盖默认 JSON 指令文本（默认 {@link #DEFAULT_INSTRUCTION}）。 */
        public Builder instruction(String value) {
            this.instruction = value;
            return this;
        }

        /** 追加一个后端 JSON 模式的私有参数（adapter 原样并入请求体，如 response_format）。 */
        public Builder jsonModeParam(String key, Object value) {
            if (key != null) {
                this.jsonModeParams.put(key, value);
            }
            return this;
        }

        /** 批量设置后端 JSON 模式私有参数。 */
        public Builder jsonModeParams(Map<String, Object> value) {
            if (value != null) {
                this.jsonModeParams.putAll(value);
            }
            return this;
        }

        /** 提取失败时的最大纠错重试次数（默认 1，硬上限 {@link #MAX_RETRIES}）。 */
        public Builder maxRetries(int value) {
            if (value < 0 || value > MAX_RETRIES) {
                throw new IllegalArgumentException("maxRetries must be in [0, " + MAX_RETRIES + "]");
            }
            this.maxRetries = value;
            return this;
        }

        /** 重试后仍提取失败时是否启用解析兜底（修复截断/多余逗号），默认开启。 */
        public Builder repair(boolean value) {
            this.repair = value;
            return this;
        }

        /** 兜底仍失败时的最终回退（不抛异常）；不设置则抛 {@link JsonOutputException}。 */
        public Builder fallback(Function<LlmRequest, String> value) {
            this.fallback = value;
            return this;
        }

        public DefaultJsonOutputPort build() {
            return new DefaultJsonOutputPort(this);
        }
    }
}
