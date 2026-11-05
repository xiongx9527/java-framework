package com.xiongx9527.fw.plus.ai.base.guard;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.ChatRole;
import com.xiongx9527.fw.plus.ai.base.model.LlmCapabilities;
import com.xiongx9527.fw.plus.ai.base.model.LlmPort;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmResponse;
import com.xiongx9527.fw.plus.ai.base.model.LlmTokenStream;
import com.xiongx9527.fw.plus.ai.base.model.ModelInfo;
import com.xiongx9527.fw.plus.ai.base.model.ModelValidation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 把输入 / 输出护栏接到 {@link LlmPort} 上的统一入口。
 *
 * <p>输入护栏在请求发出前执行：REJECT 抛 {@link GuardrailException}，REWRITE 改写最后一条
 * user 消息，RETRY 视为提示追加。输出护栏在 {@code complete} 后执行：REJECT 抛异常、
 * REWRITE 改写响应正文、RETRY 带提示重新调用（最多 {@code maxRetries} 次）。
 * 流式通道只跑输入护栏（逐 token 输出检查需业务侧自行处理）。</p>
 *
 * <p>与 {@link OutboundUrlGuard}（SSRF）、{@code ModerationModel}（内容审核）互补：
 * 那两者是具体规则，本类是承载任意规则的编排框架。</p>
 */
public final class GuardedLlmPort implements LlmPort {

    private final LlmPort delegate;

    private final List<InputGuardrail> inputGuards;

    private final List<OutputGuardrail> outputGuards;

    private final int maxRetries;

    public GuardedLlmPort(LlmPort delegate, List<InputGuardrail> inputGuards,
                          List<OutputGuardrail> outputGuards, int maxRetries) {
        if (delegate == null) {
            throw new IllegalArgumentException("llm port is required");
        }
        this.delegate = delegate;
        this.inputGuards = immutable(inputGuards);
        this.outputGuards = immutable(outputGuards);
        this.maxRetries = maxRetries < 0 ? 0 : maxRetries;
    }

    private static <T> List<T> immutable(List<T> source) {
        return source == null ? Collections.<T>emptyList()
                : Collections.unmodifiableList(new ArrayList<T>(source));
    }

    @Override
    public LlmCapabilities capabilities() {
        return delegate.capabilities();
    }

    @Override
    public List<ModelInfo> listModels() {
        return delegate.listModels();
    }

    @Override
    public ModelValidation validateModel(String model) {
        return delegate.validateModel(model);
    }

    @Override
    public LlmTokenStream stream(LlmRequest request) {
        return delegate.stream(applyInput(request));
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        LlmRequest effective = applyInput(request);
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            LlmResponse response = delegate.complete(effective);
            GuardrailResult result = GuardrailExecutor.checkOutput(outputGuards, response);
            if (result.isPass()) {
                return response;
            }
            if (result.isTerminal()) {
                throw new GuardrailException(result.getReason());
            }
            if (result.getAction() == GuardrailResult.Action.REWRITE) {
                return rewriteResponse(response, result.getContent());
            }
            if (result.getAction() == GuardrailResult.Action.RETRY && result.getContent() != null) {
                effective = effective.toBuilder().addMessage(ChatMessage.user(result.getContent())).build();
            }
        }
        return delegate.complete(effective);
    }

    private LlmRequest applyInput(LlmRequest request) {
        GuardrailResult result = GuardrailExecutor.checkInput(inputGuards, request);
        if (result.isTerminal()) {
            throw new GuardrailException(result.getReason());
        }
        if (result.getAction() == GuardrailResult.Action.REWRITE && result.getContent() != null) {
            List<ChatMessage> messages = new ArrayList<ChatMessage>(request.getMessages());
            for (int i = messages.size() - 1; i >= 0; i--) {
                if (messages.get(i) != null && messages.get(i).getRole() == ChatRole.USER) {
                    messages.set(i, ChatMessage.user(result.getContent()));
                    break;
                }
            }
            return request.toBuilder().messages(messages).build();
        }
        return request;
    }

    private static LlmResponse rewriteResponse(LlmResponse response, String content) {
        return LlmResponse.builder()
                .content(content)
                .reasoningContent(response.getReasoningContent())
                .usage(response.getUsage())
                .toolCalls(response.getToolCalls())
                .id(response.getId())
                .modelName(response.getModelName())
                .finishReason(response.getFinishReason())
                .metadata(response.getMetadata())
                .images(response.getImages())
                .audios(response.getAudios())
                .build();
    }
}
