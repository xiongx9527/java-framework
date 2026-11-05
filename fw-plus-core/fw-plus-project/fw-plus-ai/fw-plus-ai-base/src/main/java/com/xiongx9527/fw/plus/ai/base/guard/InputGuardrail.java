package com.xiongx9527.fw.plus.ai.base.guard;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.ChatRole;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;

/** 输入护栏：在请求发给模型前检查用户输入。 */
@FunctionalInterface
public interface InputGuardrail {

    GuardrailResult check(String input);

    /** 默认取请求里最后一条 user 消息的文本。 */
    default GuardrailResult check(LlmRequest request) {
        String text = null;
        if (request != null) {
            for (ChatMessage message : request.getMessages()) {
                if (message != null && message.getRole() == ChatRole.USER) {
                    text = message.getContent();
                }
            }
        }
        return check(text == null ? "" : text);
    }
}
