package com.xiongx9527.fw.plus.ai.base.guard;

import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmResponse;
import java.util.List;

/** 护栏链的顺序执行器：返回第一个非 PASS 的结果，全部通过则返回 PASS。 */
public final class GuardrailExecutor {

    private GuardrailExecutor() {
    }

    public static GuardrailResult checkInput(List<InputGuardrail> guards, LlmRequest request) {
        if (guards == null || guards.isEmpty()) {
            return GuardrailResult.pass();
        }
        for (InputGuardrail guard : guards) {
            if (guard == null) {
                continue;
            }
            GuardrailResult result = guard.check(request);
            if (result != null && !result.isPass()) {
                return result;
            }
        }
        return GuardrailResult.pass();
    }

    public static GuardrailResult checkOutput(List<OutputGuardrail> guards, LlmResponse response) {
        if (guards == null || guards.isEmpty()) {
            return GuardrailResult.pass();
        }
        for (OutputGuardrail guard : guards) {
            if (guard == null) {
                continue;
            }
            GuardrailResult result = guard.check(response);
            if (result != null && !result.isPass()) {
                return result;
            }
        }
        return GuardrailResult.pass();
    }
}
