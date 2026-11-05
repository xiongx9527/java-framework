package com.xiongx9527.fw.plus.ai.support.moderation;

import com.xiongx9527.fw.plus.ai.base.guard.GuardrailException;
import com.xiongx9527.fw.plus.ai.base.guard.GuardedLlmPort;
import com.xiongx9527.fw.plus.ai.base.guard.InputGuardrail;
import com.xiongx9527.fw.plus.ai.base.guard.OutputGuardrail;
import com.xiongx9527.fw.plus.ai.base.model.LlmPort;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmResponse;
import com.xiongx9527.fw.plus.ai.base.model.LlmTokenStream;
import com.xiongx9527.fw.plus.ai.base.model.ModelInfo;
import com.xiongx9527.fw.plus.ai.base.model.ModelValidation;
import com.xiongx9527.fw.plus.ai.base.model.moderation.ModerationException;
import com.xiongx9527.fw.plus.ai.base.model.moderation.ModerationModel;
import java.util.ArrayList;
import java.util.List;

/**
 * 给 {@link LlmPort} 接内容审核的装饰器：命中违规时抛 {@link ModerationException}，
 * 不命中则透传。输入审核对两条通道都生效；输出审核仅非流式生效。
 *
 * <p>本类不再自造编排逻辑，而是把 {@link ModerationModel} 适配成 base 的
 * {@link InputGuardrail} / {@link OutputGuardrail}，再委托给 {@link GuardedLlmPort}
 * 统一编排（REJECT 拦截、REWRITE 改写、RETRY 重试）。对外契约（构造参数与抛
 * {@code ModerationException}）保持不变，仅把编排能力收敛到 base 的护栏框架。</p>
 */
public final class ModeratedLlmPort implements LlmPort {

    private final LlmPort delegate;

    public ModeratedLlmPort(LlmPort delegate, ModerationModel moderation, ModerationMode mode) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate llm port is required");
        }
        if (moderation == null) {
            throw new IllegalArgumentException("moderation model is required");
        }
        List<InputGuardrail> inputGuards = new ArrayList<InputGuardrail>();
        List<OutputGuardrail> outputGuards = new ArrayList<OutputGuardrail>();
        boolean input = mode == null || mode == ModerationMode.INPUT || mode == ModerationMode.BOTH;
        boolean output = mode == null || mode == ModerationMode.OUTPUT || mode == ModerationMode.BOTH;
        if (input) inputGuards.add(new ModerationInputGuard(moderation));
        if (output) outputGuards.add(new ModerationOutputGuard(moderation));
        this.delegate = new GuardedLlmPort(delegate, inputGuards, outputGuards, 0);
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        try {
            return delegate.complete(request);
        } catch (GuardrailException ex) {
            throw new ModerationException(ex.getMessage());
        }
    }

    @Override
    public LlmTokenStream stream(LlmRequest request) {
        try {
            return delegate.stream(request);
        } catch (GuardrailException ex) {
            return LlmTokenStream.create(handler -> handler.onError("content rejected by moderation"));
        }
    }

    @Override
    public List<ModelInfo> listModels() {
        return delegate.listModels();
    }

    @Override
    public ModelValidation validateModel(String model) {
        return delegate.validateModel(model);
    }
}
