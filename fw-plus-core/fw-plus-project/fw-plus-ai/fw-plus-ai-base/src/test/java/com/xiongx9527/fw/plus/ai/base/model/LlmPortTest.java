package com.xiongx9527.fw.plus.ai.base.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class LlmPortTest {

    /** 流式底座：两个正文片段 + 一个思考片段 + 用量。 */
    private static class FakePort extends AbstractLlmPort {

        private final boolean fail;

        FakePort(boolean fail) {
            this.fail = fail;
        }

        @Override
        protected void doStream(LlmRequest request, LlmStreamHandler handler) {
            if (fail) {
                handler.onError("upstream down");
                return;
            }
            handler.onToken("hello", "think-1");
            handler.onToken(" world", null);
            handler.onUsageDetail(3L, 4L, 7L);
            handler.onComplete();
        }
    }

    @Test
    void completeAggregatesStreamingDeltas() {
        LlmResponse response = new FakePort(false).complete(LlmRequest.systemUser("sys", "user"));

        assertEquals("hello world", response.getContent());
        assertEquals("think-1", response.getReasoningContent());
        assertEquals(7L, response.getUsage().getTotalTokens());
        assertEquals(3L, response.getUsage().getInputTokens());
    }

    @Test
    void streamDeliversDeltasAndCompletion() {
        final List<String> tokens = new ArrayList<String>();
        final boolean[] completed = {false};

        new FakePort(false).stream(LlmRequest.systemUser("s", "u"), new LlmStreamHandler() {
            @Override
            public void onToken(String content, String reasoningContent) {
                tokens.add(content);
            }

            @Override
            public void onComplete() {
                completed[0] = true;
            }

            @Override
            public void onError(String message) {
            }
        });

        assertEquals(Arrays.asList("hello", " world"), tokens);
        assertEquals(true, completed[0]);
    }

    @Test
    void bothChannelsNotifyListeners() {
        final List<String> events = new ArrayList<String>();
        LlmListener listener = new LlmListener() {
            @Override
            public void onRequest(LlmRequest request) {
                events.add("request");
            }

            @Override
            public void onToken(String content, String reasoningContent) {
                events.add("token:" + content);
            }

            @Override
            public void onUsage(LlmUsage usage) {
                events.add("usage:" + usage.getTotalTokens());
            }

            @Override
            public void onComplete() {
                events.add("complete");
            }

            @Override
            public void onError(String message) {
                events.add("error:" + message);
            }
        };

        new FakePort(false).complete(LlmRequest.builder().addMessage(ChatMessage.user("u")).listener(listener).build());
        new FakePort(false).stream(LlmRequest.builder().addMessage(ChatMessage.user("u")).listener(listener).build(), null);

        List<String> expected = Arrays.asList(
                "request", "token:hello", "token: world", "usage:7", "complete",
                "request", "token:hello", "token: world", "usage:7", "complete");
        assertEquals(expected, events);
    }

    @Test
    void completeThrowsAndNotifiesOnError() {
        final List<String> errors = new ArrayList<String>();
        LlmRequest request = LlmRequest.builder()
                .addMessage(ChatMessage.user("u"))
                .listener(new LlmListener() {
                    @Override
                    public void onError(String message) {
                        errors.add(message);
                    }
                })
                .build();

        assertThrows(LlmException.class, () -> new FakePort(true).complete(request));
        assertEquals(Arrays.asList("upstream down"), errors);
    }

    @Test
    void completeReturnsNullContentWhenNothingProduced() {
        AbstractLlmPort silent = new AbstractLlmPort() {
            @Override
            protected void doStream(LlmRequest request, LlmStreamHandler handler) {
                handler.onComplete();
            }
        };

        LlmResponse response = silent.complete(LlmRequest.systemUser("s", "u"));

        assertNull(response.getContent());
        assertEquals(0L, response.getUsage().getTotalTokens());
    }
}
