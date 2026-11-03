package com.xiongx9527.fw.plus.ai.askdata.llm;

import com.xiongx9527.fw.plus.ai.base.model.AbstractLlmPort;
import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.ChatRole;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmStreamHandler;
import java.util.List;
import java.util.function.Function;

/**
 * 内存 / 回归测试用的 {@link com.xiongx9527.fw.plus.ai.base.model.LlmPort} 实现：
 * 用给定函数生成响应，不依赖任何真实模型。
 */
public class StaticLlmPort extends AbstractLlmPort {

    private final Function<String, String> responder;

    public StaticLlmPort(String fixedResponse) {
        this(userPrompt -> fixedResponse);
    }

    public StaticLlmPort(Function<String, String> responder) {
        this.responder = responder;
    }

    @Override
    protected void doStream(LlmRequest request, LlmStreamHandler handler) {
        String output;
        try {
            output = responder.apply(userPrompt(request));
        } catch (RuntimeException e) {
            handler.onError(e.getMessage());
            return;
        }
        if (output != null) {
            handler.onToken(output, null);
        }
        handler.onComplete();
    }

    /** 取最后一条用户消息的文本，作为 responder 的入参。 */
    static String userPrompt(LlmRequest request) {
        List<ChatMessage> messages = request.getMessages();
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage message = messages.get(i);
            if (message.getRole() == ChatRole.USER) {
                return message.getContent();
            }
        }
        return messages.isEmpty() ? null : messages.get(messages.size() - 1).getContent();
    }
}
