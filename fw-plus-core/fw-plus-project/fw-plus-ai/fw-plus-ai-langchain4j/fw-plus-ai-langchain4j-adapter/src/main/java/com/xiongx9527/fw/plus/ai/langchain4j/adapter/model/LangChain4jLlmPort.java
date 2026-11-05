package com.xiongx9527.fw.plus.ai.langchain4j.adapter.model;

import com.xiongx9527.fw.plus.ai.base.model.AbstractLlmPort;
import com.xiongx9527.fw.plus.ai.base.model.LlmCapabilities;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmStreamHandler;
import com.xiongx9527.fw.plus.ai.base.model.ModelValidation;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.support.LcMessages;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.support.LcTools;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import java.util.List;

/**
 * 把 LangChain4j 0.35 的 {@link ChatLanguageModel} 与 {@link StreamingChatLanguageModel}
 * 适配成 base 的 {@code LlmPort}。
 *
 * <p>路由规则：</p>
 * <ul>
 *   <li>请求带工具且持有 {@code ChatLanguageModel} → 走非流式 {@code generate(messages, toolSpecs)}，
 *       0.35 的工具调用结果在完整 {@code AiMessage} 里返回；</li>
 *   <li>持有 {@code StreamingChatLanguageModel} → 走流式 {@code generate(messages, handler)}，
 *       {@code onNext} 逐段回调 {@code onToken}，结束时只补用量 / 工具调用 / {@code onComplete}；</li>
 *   <li>只有 {@code ChatLanguageModel} → 一次性生成，转成「一段完整 token + 结束」，
 *       与 {@link AbstractLlmPort} 对非流式后端的要求一致。</li>
 * </ul>
 *
 * <p><b>参数粒度</b>：0.35 的 modelName / temperature / topP / maxTokens / seed 是模型实例
 * 构造时固定的，请求级无法切换，因此 {@link LlmRequest} 的这些字段在此适配器中被忽略，
 * 由注入的模型实例决定；{@code extraParams} 同理。{@code LlmRequest.model} 仅用于目录/校验展示。</p>
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
        try {
            if (request.hasTools() && chatModel != null) {
                Response<AiMessage> response = chatModel.generate(messages,
                        LcTools.toUpstreamSpecs(request.getTools()));
                emitFull(response, handler);
            } else if (streamingModel != null) {
                streamingModel.generate(messages, new StreamingResponseHandler<AiMessage>() {
                    @Override
                    public void onNext(String token) {
                        handler.onToken(token, null);
                    }

                    @Override
                    public void onComplete(Response<AiMessage> response) {
                        emitTail(response, handler);
                    }

                    @Override
                    public void onError(Throwable error) {
                        handler.onError(messageOf(error));
                    }
                });
            } else {
                emitFull(chatModel.generate(messages), handler);
            }
        } catch (RuntimeException e) {
            handler.onError(messageOf(e));
        }
    }

    /** 非流式：把完整正文作为一段 token 交给聚合器。 */
    private static void emitFull(Response<AiMessage> response, LlmStreamHandler handler) {
        AiMessage ai = response == null ? null : response.content();
        if (ai != null && ai.text() != null && !ai.text().isEmpty()) {
            handler.onToken(ai.text(), null);
        }
        emitTail(response, handler);
    }

    /** 收尾：工具调用 + 用量 + onComplete；流式通道不重复投递正文。 */
    private static void emitTail(Response<AiMessage> response, LlmStreamHandler handler) {
        AiMessage ai = response == null ? null : response.content();
        if (ai != null && ai.hasToolExecutionRequests()) {
            for (dev.langchain4j.agent.tool.ToolExecutionRequest call : ai.toolExecutionRequests()) {
                handler.onToolCall(LcTools.toBaseCall(call));
            }
        }
        TokenUsage usage = response == null ? null : response.tokenUsage();
        if (usage != null) {
            long input = longValue(usage.inputTokenCount());
            long output = longValue(usage.outputTokenCount());
            long total = usage.totalTokenCount() == null
                    ? input + output : longValue(usage.totalTokenCount());
            handler.onUsageDetail(input, output, total);
        }
        handler.onComplete();
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
