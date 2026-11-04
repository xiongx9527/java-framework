package com.xiongx9527.fw.plus.ai.support.moderation;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.LlmListener;
import com.xiongx9527.fw.plus.ai.base.model.LlmPort;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmResponse;
import com.xiongx9527.fw.plus.ai.base.model.LlmStreamHandler;
import com.xiongx9527.fw.plus.ai.base.model.ModelInfo;
import com.xiongx9527.fw.plus.ai.base.model.ModelValidation;
import com.xiongx9527.fw.plus.ai.base.model.moderation.ModerationException;
import com.xiongx9527.fw.plus.ai.base.model.moderation.ModerationModel;
import com.xiongx9527.fw.plus.ai.base.model.moderation.ModerationResult;
import java.util.List;

/**
 * 给 {@link LlmPort} 接内容审核的装饰器：命中违规时抛 {@link ModerationException}，
 * 不命中则透传。输入审核对两条通道都生效；输出审核仅非流式生效。
 *
 * <p>这是 moderation 能力的「组合 / 接线」部分——base 只定义审核模型端口，本类把它落到链路里。</p>
 */
public final class ModeratedLlmPort implements LlmPort {

    private final LlmPort delegate;

    private final ModerationModel moderation;

    private final ModerationMode mode;

    public ModeratedLlmPort(LlmPort delegate, ModerationModel moderation, ModerationMode mode) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate llm port is required");
        }
        this.delegate = delegate;
        this.moderation = moderation;
        this.mode = mode == null ? ModerationMode.BOTH : mode;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        if (shouldModerateInput()) {
            ModerationResult result = moderation.moderate(inputText(request));
            if (result != null && result.isFlagged()) {
                throw new ModerationException(result);
            }
        }
        LlmResponse response = delegate.complete(request);
        if (shouldModerateOutput()) {
            ModerationResult result = moderation.moderate(response == null ? null : response.getContent());
            if (result != null && result.isFlagged()) {
                throw new ModerationException(result);
            }
        }
        return response;
    }

    @Override
    public void stream(LlmRequest request, LlmStreamHandler handler) {
        if (shouldModerateInput()) {
            ModerationResult result = moderation.moderate(inputText(request));
            if (result != null && result.isFlagged()) {
                handler.onError("content rejected by moderation");
                return;
            }
        }
        // 流式输出无法先审后发，故只审输入
        delegate.stream(request, handler);
    }

    @Override
    public List<ModelInfo> listModels() {
        return delegate.listModels();
    }

    @Override
    public ModelValidation validateModel(String model) {
        return delegate.validateModel(model);
    }

    private boolean shouldModerateInput() {
        return moderation != null && (mode == ModerationMode.INPUT || mode == ModerationMode.BOTH);
    }

    private boolean shouldModerateOutput() {
        return moderation != null && (mode == ModerationMode.OUTPUT || mode == ModerationMode.BOTH);
    }

    private static String inputText(LlmRequest request) {
        if (request == null || request.getMessages() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ChatMessage message : request.getMessages()) {
            if (message.getRole() != null) {
                sb.append(message.getRole().wireName()).append(": ");
            }
            String text = message.getContent();
            if (text != null) {
                sb.append(text).append('\n');
            }
        }
        return sb.toString();
    }
}
