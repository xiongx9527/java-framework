package com.xiongx9527.fw.plus.ai.support.moderation;

import com.xiongx9527.fw.plus.ai.base.guard.GuardrailResult;
import com.xiongx9527.fw.plus.ai.base.guard.OutputGuardrail;
import com.xiongx9527.fw.plus.ai.base.model.moderation.ModerationModel;

/**
 * 把 {@link ModerationModel} 适配成 base 的 {@link OutputGuardrail}。
 *
 * <p>命中违规映射为 {@link GuardrailResult#reject(String)}，由 {@code GuardedLlmPort}
 * 在 {@code complete} 返回后拦截（流式通道不跑输出护栏，与旧 {@code ModeratedLlmPort} 行为一致）。</p>
 */
public final class ModerationOutputGuard implements OutputGuardrail {

    private final ModerationModel moderation;

    public ModerationOutputGuard(ModerationModel moderation) {
        if (moderation == null) {
            throw new IllegalArgumentException("moderation model is required");
        }
        this.moderation = moderation;
    }

    @Override
    public GuardrailResult check(String output) {
        if (output == null) {
            return GuardrailResult.pass();
        }
        return ModerationInputGuard.toResult(moderation.moderate(output));
    }
}
