package com.xiongx9527.fw.plus.ai.askdata.llm;

import java.util.function.Function;

/**
 * 内存 / 回归测试用的 {@link LlmChatPort} 实现：记录入参调用并用给定函数生成响应。
 * 不依赖任何真实模型，仅用于单元测试与本地验证。
 */
public class StaticLlmChatPort implements LlmChatPort {
    private final Function<String, String> responder;

    public StaticLlmChatPort(String fixedResponse) {
        this(userPrompt -> fixedResponse);
    }

    public StaticLlmChatPort(Function<String, String> responder) {
        this.responder = responder;
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        return responder.apply(userPrompt);
    }
}
