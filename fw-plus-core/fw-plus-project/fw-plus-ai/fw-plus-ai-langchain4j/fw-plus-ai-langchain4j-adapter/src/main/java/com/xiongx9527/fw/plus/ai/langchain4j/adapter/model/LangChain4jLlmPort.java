package com.xiongx9527.fw.plus.ai.langchain4j.adapter.model;

import com.xiongx9527.fw.plus.ai.base.model.AbstractLlmPort;
import com.xiongx9527.fw.plus.ai.base.model.LlmCapabilities;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmStreamHandler;
import com.xiongx9527.fw.plus.ai.base.model.ModelValidation;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.support.LcMessages;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.support.LcTools;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import java.util.List;
import java.util.Map;

/**
 * 把 LangChain4j 0.35 的 {@link ChatLanguageModel} 与 {@link StreamingChatLanguageModel}
 * 适配成 base 的 {@code LlmPort}。
 *
 * <p>路由规则：</p>
 * <ul>
 *   <li>请求带工具且持有 {@code ChatLanguageModel} → 非流式 {@code generate(messages, toolSpecs)}
 *       （或带 JSON 模式时走 {@code chat(ChatRequest)}），0.35 的工具调用结果在完整
 *       {@code AiMessage} 里返回；</li>
 *   <li>持有 {@code StreamingChatLanguageModel} → 流式 {@code generate(messages, handler)}，
 *       {@code onNext} 逐段回调 {@code onToken}，结束时补用量 / 工具调用 / finishReason / {@code onComplete}；</li>
 *   <li>只有 {@code ChatLanguageModel} → 一次性生成，转成「一段完整 token + 结束」，
 *       与 {@link AbstractLlmPort} 对非流式后端的要求一致。</li>
 * </ul>
 *
 * <p><b>参数粒度</b>：0.35 的 modelName / temperature / topP / maxTokens / seed 是模型实例
 * 构造时固定的（0.35 的 {@code ChatRequest} 不携带 {@code ChatRequestParameters}），
 * 请求级无法切换，因此 {@link LlmRequest} 的这些字段在此适配器中被忽略，由注入的模型实例决定；
 * {@code extraParams} 同理。{@code LlmRequest.model} 仅用于目录/校验展示。</p>
 *
 * <p><b>已对齐的 base 能力</b>：</p>
 * <ul>
 *   <li>{@code responseFormat}（结构化 / JSON 模式）：非流式路径经 {@code ChatRequest.responseFormat}
 *       下发；0.35 流式模型无对应重载，无法下发（请求将退回普通模式）。</li>
 *   <li>{@code finishReason}：从 0.35 的 {@link FinishReason} 回填 {@link com.xiongx9527.fw.plus.ai.base.model.LlmResponse}。</li>
 *   <li>{@code structuredOutput} 能力：从 0.35 的 {@link ChatLanguageModel#supportedCapabilities()} 派生。</li>
 * </ul>
 *
 * <p><b>0.35 无法提供的 base 能力</b>（非本适配器缺陷，需 SB3 分支的 1.x 适配器）：
 * 响应级 id / modelName / metadata、reasoning/thinking 透传、未映射原始帧
 * （{@code onUnmappedRawEvent}）、增量工具调用事件、原生异步 IO、向量库 filter 原生下推。</p>
 */
public class LangChain4jLlmPort extends AbstractLlmPort {

    private final ChatLanguageModel chatModel;

    private final StreamingChatLanguageModel streamingModel;

    private final LlmCapabilities capabilities;

    public LangChain4jLlmPort(ChatLanguageModel chatModel) {
        this(chatModel, null);
    }

    public LangChain4jLlmPort(ChatLanguageModel chatModel, StreamingChatLanguageModel streamingModel) {
        if (chatModel == null && streamingModel == null) {
            throw new IllegalArgumentException("at least one LangChain4j chat model is required");
        }
        this.chatModel = chatModel;
        this.streamingModel = streamingModel;
        this.capabilities = LlmCapabilities.builder()
                .streaming(streamingModel != null)
                .toolCalling(chatModel != null)
                .usage(true)
                .structuredOutput(chatModel != null
                        && chatModel.supportedCapabilities().contains(Capability.RESPONSE_FORMAT_JSON_SCHEMA))
                .build();
    }

    @Override
    public LlmCapabilities capabilities() {
        return capabilities;
    }

    /** 0.35 无法按模型名切换实例，用一次最小真实补全同时验证端点、鉴权与当前模型。 */
    @Override
    public ModelValidation validateModel(String model) {
        if (model == null || model.trim().isEmpty()) {
            return ModelValidation.invalid(model, "model id is required", 0L);
        }
        return validateModelWithCompletion(model);
    }

    @Override
    protected void doStream(LlmRequest request, LlmStreamHandler handler) {
        List<dev.langchain4j.data.message.ChatMessage> messages =
                LcMessages.toUpstream(request.getMessages());
        ResponseFormat responseFormat = toResponseFormat(request.getResponseFormat());
        try {
            if (request.hasTools() && chatModel != null) {
                if (responseFormat != null) {
                    ChatResponse cr = chatModel.chat(ChatRequest.builder()
                            .messages(messages)
                            .toolSpecifications(LcTools.toUpstreamSpecs(request.getTools()))
                            .responseFormat(responseFormat)
                            .build());
                    emitChat(cr, handler);
                } else {
                    emitFull(chatModel.generate(messages,
                            LcTools.toUpstreamSpecs(request.getTools())), handler);
                }
            } else if (streamingModel != null) {
                // 0.35 流式模型无 chat(ChatRequest) 重载，responseFormat 无法下发
                streamingModel.generate(messages, new StreamingResponseHandler<AiMessage>() {
                    @Override
                    public void onNext(String token) {
                        handler.onToken(token, null);
                    }

                    @Override
                    public void onComplete(Response<AiMessage> response) {
                        // 正文已由 onNext 逐段投递，完成时不再重投，仅补工具 / 用量 / finishReason
                        emitCompletion(response == null ? null : response.content(),
                                response == null ? null : response.tokenUsage(),
                                response == null ? null : response.finishReason(), handler);
                    }

                    @Override
                    public void onError(Throwable error) {
                        handler.onError(messageOf(error));
                    }
                });
            } else {
                if (responseFormat != null) {
                    ChatResponse cr = chatModel.chat(ChatRequest.builder()
                            .messages(messages)
                            .responseFormat(responseFormat)
                            .build());
                    emitChat(cr, handler);
                } else {
                    emitFull(chatModel.generate(messages), handler);
                }
            }
        } catch (RuntimeException e) {
            handler.onError(messageOf(e));
        }
    }

    /** 非流式：把完整正文作为一段 token 交给聚合器。 */
    private static void emitFull(Response<AiMessage> response, LlmStreamHandler handler) {
        emitPieces(response == null ? null : response.content(),
                response == null ? null : response.tokenUsage(),
                response == null ? null : response.finishReason(), handler);
    }

    /** 走 {@code chat(ChatRequest)} 的响应：结构与 {@link Response} 一致，单独抽取。 */
    private static void emitChat(ChatResponse response, LlmStreamHandler handler) {
        emitPieces(response == null ? null : response.aiMessage(),
                response == null ? null : response.tokenUsage(),
                response == null ? null : response.finishReason(), handler);
    }

    /** 收尾：正文 + 工具调用 + 用量 + finishReason + onComplete。用于非流式路径。 */
    private static void emitPieces(AiMessage ai, TokenUsage usage, FinishReason finishReason,
                                   LlmStreamHandler handler) {
        if (ai != null && ai.text() != null && !ai.text().isEmpty()) {
            handler.onToken(ai.text(), null);
        }
        emitCompletion(ai, usage, finishReason, handler);
    }

    /** 仅收尾：工具调用 + 用量 + finishReason + onComplete，不重投正文。用于流式完成（正文已逐段投递）。 */
    private static void emitCompletion(AiMessage ai, TokenUsage usage, FinishReason finishReason,
                                        LlmStreamHandler handler) {
        if (ai != null && ai.hasToolExecutionRequests()) {
            for (dev.langchain4j.agent.tool.ToolExecutionRequest call : ai.toolExecutionRequests()) {
                handler.onToolCall(LcTools.toBaseCall(call));
            }
        }
        if (usage != null) {
            long input = longValue(usage.inputTokenCount());
            long output = longValue(usage.outputTokenCount());
            long total = usage.totalTokenCount() == null
                    ? input + output : longValue(usage.totalTokenCount());
            handler.onUsageDetail(input, output, total);
        }
        if (finishReason != null) {
            handler.onFinish(finishReason.name());
        }
        handler.onComplete();
    }

    private static ResponseFormat toResponseFormat(Map<String, Object> responseFormat) {
        if (responseFormat == null || responseFormat.isEmpty()) {
            return null;
        }
        // 0.35 仅提供 TEXT / JSON 两类；base 的结构化输出提示统一降级为 JSON 模式。
        return ResponseFormat.JSON;
    }

    private static long longValue(Integer value) {
        return value == null ? 0L : value.longValue();
    }

    private static String messageOf(Throwable error) {
        if (error == null) {
            return "unknown error";
        }
        return error.getMessage() == null || error.getMessage().isEmpty()
                ? error.getClass().getSimpleName() : error.getMessage();
    }
}
