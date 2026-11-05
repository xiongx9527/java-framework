package com.xiongx9527.fw.plus.ai.base.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/**
 * 验证 1.x 对齐后的流事件面：{@link LlmStreamHandler#onUnmappedRawEvent(Object)} 与
 * 带 {@link StreamEventContext} 的上下文重载能沿主链（adapter → Eventing → Guard → Composite）
 * 透传到用户 handler，且回调内可通过 {@link StreamEventContext#stream()} 取消。
 */
public class StreamEventSurfaceTest {

    private static final class ScriptedPort extends AbstractLlmPort {
        @Override
        protected void doStream(LlmRequest request, LlmStreamHandler handler) {
            handler.onToken("hello", null);
            handler.onPartialToolCall("t1", "calc", "{\"a\":1}");
            handler.onUnmappedRawEvent("RAW-FRAME");
            handler.onComplete();
        }
    }

    private static final class CapturingHandler implements LlmStreamHandler {
        final List<String> tokens = new ArrayList<String>();
        final List<String> partials = new ArrayList<String>();
        final List<Object> raws = new ArrayList<Object>();
        StreamEventContext lastCtx;

        @Override
        public void onToken(String content, String reasoningContent) {
            onToken(content, reasoningContent, null);
        }

        @Override
        public void onToken(String content, String reasoningContent, StreamEventContext context) {
            tokens.add(content);
            lastCtx = context;
        }

        @Override
        public void onComplete() {
        }

        @Override
        public void onPartialToolCall(String id, String name, String argumentsDelta, StreamEventContext context) {
            partials.add(id);
            lastCtx = context;
        }

        @Override
        public void onUnmappedRawEvent(Object rawEvent) {
            raws.add(rawEvent);
        }

        @Override
        public void onError(String message) {
        }
    }

    @Test
    public void contextAndRawEventReachUserHandler() {
        ScriptedPort port = new ScriptedPort();
        LlmRequest request = LlmRequest.builder()
                .model("m").addMessage(ChatMessage.user("x")).build();
        LlmTokenStream stream = port.stream(request);
        CapturingHandler handler = new CapturingHandler();
        stream.handler(handler).start();

        assertEquals(1, handler.tokens.size());
        assertEquals("hello", handler.tokens.get(0));
        assertEquals(1, handler.partials.size());
        assertEquals("t1", handler.partials.get(0));
        assertEquals(1, handler.raws.size());
        assertEquals("RAW-FRAME", handler.raws.get(0));

        // 上下文应携带真实流句柄，且与非流式路径不同（此处为流式）
        assertNotNull(handler.lastCtx);
        assertNotNull(handler.lastCtx.stream());
        assertSame(stream, handler.lastCtx.stream());
        assertNotNull(handler.lastCtx.request());
    }

    @Test
    public void cancelFromContextStopsFurtherEvents() {
        AbstractLlmPort port = new AbstractLlmPort() {
            @Override
            protected void doStream(LlmRequest request, LlmStreamHandler handler) {
                handler.onToken("before", null);
                handler.onUnmappedRawEvent("should-be-dropped");
                handler.onComplete();
            }
        };
        LlmRequest request = LlmRequest.builder()
                .model("m").addMessage(ChatMessage.user("x")).build();
        LlmTokenStream stream = port.stream(request);

        final List<Object> afterCancel = new ArrayList<Object>();
        LlmStreamHandler handler = new LlmStreamHandler() {
            @Override
            public void onToken(String content, String reasoningContent) {
            }

            @Override
            public void onToken(String content, String reasoningContent, StreamEventContext context) {
                context.stream().cancel();
            }

            @Override
            public void onComplete() {
            }

            @Override
            public void onUnmappedRawEvent(Object rawEvent) {
                afterCancel.add(rawEvent);
            }

            @Override
            public void onError(String message) {
            }
        };
        stream.handler(handler).start();

        assertTrue("cancel 之后的事件不应再派发", afterCancel.isEmpty());
        assertTrue(stream.isCancelled());
    }

    @Test
    public void defaultContextOverloadDelegatesToPlain() {
        // 仅重写基础签名时，上下文重载默认委托回基础签名，事件不丢
        final List<String> seen = new ArrayList<String>();
        LlmStreamHandler only = new LlmStreamHandler() {
            @Override
            public void onToken(String content, String reasoningContent) {
                seen.add(content);
            }

            @Override
            public void onComplete() {
            }

            @Override
            public void onError(String message) {
            }
        };
        only.onToken("a", null, new StreamEventContext(null, null));
        assertEquals(1, seen.size());
        assertFalse(seen.isEmpty());
    }
}
