package com.xiongx9527.fw.plus.ai.support.trace;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmResponse;
import com.xiongx9527.fw.plus.ai.base.model.LlmUsage;
import com.xiongx9527.fw.plus.ai.base.tool.ToolExecutionRequest;
import com.xiongx9527.fw.plus.ai.base.tool.ToolSpecification;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class LoggingLlmListenerTest {

    private static LlmRequest sampleRequest() {
        List<ChatMessage> messages = new ArrayList<ChatMessage>();
        messages.add(ChatMessage.system("sys"));
        messages.add(ChatMessage.user("hello"));
        List<ToolSpecification> tools = new ArrayList<ToolSpecification>();
        tools.add(ToolSpecification.builder().name("search").description("search the web").build());
        return LlmRequest.builder().messages(messages).tools(tools).build();
    }

    private static LlmResponse sampleResponseWithTools() {
        return LlmResponse.builder()
                .toolCalls(Collections.singletonList(ToolExecutionRequest.of("search", "{\"q\":\"x\"}")))
                .usage(new LlmUsage(10, 20, 30))
                .build();
    }

    private static LlmResponse sampleResponseText() {
        return LlmResponse.builder()
                .content("a plain answer")
                .usage(LlmUsage.ZERO)
                .build();
    }

    @Test
    void requestResponseAndErrorDoNotThrow() {
        LoggingLlmListener listener = new LoggingLlmListener();

        listener.onRequest(sampleRequest());
        listener.onResponse(sampleResponseText());
        listener.onError("boom");

        // 带工具调用的响应也不应抛异常
        listener.onRequest(sampleRequest());
        listener.onResponse(sampleResponseWithTools());
    }

    @Test
    void responseWithoutRequestStillSafe() {
        LoggingLlmListener listener = new LoggingLlmListener();
        // 未经过 onRequest 直接 onResponse / onError 也不应 NPE
        listener.onResponse(sampleResponseText());
        listener.onError("no request context");
    }
}
