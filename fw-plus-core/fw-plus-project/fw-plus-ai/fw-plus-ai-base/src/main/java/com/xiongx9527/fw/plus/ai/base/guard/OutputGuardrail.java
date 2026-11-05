package com.xiongx9527.fw.plus.ai.base.guard;

import com.xiongx9527.fw.plus.ai.base.model.LlmResponse;

/** 输出护栏：在模型返回后检查生成内容，可要求改写或重试。 */
@FunctionalInterface
public interface OutputGuardrail {

    GuardrailResult check(String output);

    /** 默认检查响应正文。 */
    default GuardrailResult check(LlmResponse response) {
        return check(response == null || response.getContent() == null ? "" : response.getContent());
    }
}
