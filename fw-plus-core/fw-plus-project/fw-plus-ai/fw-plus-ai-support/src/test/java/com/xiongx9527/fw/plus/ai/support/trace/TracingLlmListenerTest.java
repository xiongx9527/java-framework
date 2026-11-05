package com.xiongx9527.fw.plus.ai.support.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.trace.Span;
import com.xiongx9527.fw.plus.ai.base.trace.TracePort;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** 校验 TracingLlmListener 把 onFinish 的结束原因记录进 span 属性。 */
class TracingLlmListenerTest {

    @Test
    void finishReasonIsRecordedAsSpanAttribute() {
        AtomicReference<Span> captured = new AtomicReference<Span>();
        TracePort port = new TracePort() {
            @Override public Span.Builder start(String name, com.xiongx9527.fw.plus.ai.base.trace.SpanKind kind) {
                return Span.start("t", name, kind);
            }
            @Override public void end(Span.Builder span) {
                captured.set(span.build());
            }
            @Override public void flush() { }
        };
        TracingLlmListener listener = new TracingLlmListener(port);

        LlmRequest request = LlmRequest.builder().addMessage(ChatMessage.user("hi")).build();
        listener.onRequest(request);
        listener.onFinish("STOP");
        listener.onComplete();

        Span span = captured.get();
        assertEquals("STOP", span.getAttributes().get("finish_reason"));
    }
}
