package com.xiongx9527.fw.plus.ai.support.moderation;

import com.xiongx9527.fw.plus.ai.base.guard.GuardrailResult;
import com.xiongx9527.fw.plus.ai.base.guard.InputGuardrail;
import com.xiongx9527.fw.plus.ai.base.model.moderation.ModerationModel;

/**
 * 把 {@link ModerationModel} 适配成 base 的 {@link InputGuardrail}。
 *
 * <p>命中违规（{@code flagged}）映射为 {@link GuardrailResult#reject(String)}，
 * 由 {@code GuardedLlmPort} 统一在请求发出前拦截；未命中放行。
 * 这样内容审核不再自造编排逻辑，而是作为护栏规则接入 {@code GuardedLlmPort}。</p>
 */
public final class ModerationInputGuard implements InputGuardrail {

    private final ModerationModel moderation;

    public ModerationInputGuard(ModerationModel moderation) {
        if (moderation == null) {
            throw new IllegalArgumentException("moderation model is required");
        }
        this.moderation = moderation;
    }

    @Override
    public GuardrailResult check(String input) {
        if (input == null) {
            return GuardrailResult.pass();
        }
        return toResult(moderation.moderate(input));
    }

    static GuardrailResult toResult(com.xiongx9527.fw.plus.ai.base.model.moderation.ModerationResult result) {
        if (result == null || !result.isFlagged()) {
            return GuardrailResult.pass();
        }
        return GuardrailResult.reject(result.getReason() == null ? "content flagged by moderation" : result.getReason());
    }
}
