package com.xiongx9527.fw.plus.ai.base.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.xiongx9527.fw.plus.ai.base.model.AbstractLlmPort;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmStreamHandler;
import com.xiongx9527.fw.plus.ai.base.model.moderation.ModerationCategory;
import com.xiongx9527.fw.plus.ai.base.model.moderation.ModerationException;
import com.xiongx9527.fw.plus.ai.base.model.moderation.ModerationModel;
import com.xiongx9527.fw.plus.ai.base.model.moderation.ModerationResult;
import com.xiongx9527.fw.plus.ai.base.output.DefaultJsonOutputPort;
import com.xiongx9527.fw.plus.ai.base.output.JsonOutputPort;
import org.junit.jupiter.api.Test;

class AiServicesModerationJsonTest {

    /** 记录请求并返回固定回复。 */
    static class RecordingLlm extends AbstractLlmPort {
        @Override
        protected void doStream(LlmRequest request, LlmStreamHandler handler) {
            handler.onToken("answer", null);
            handler.onComplete();
        }
    }

    /** 命中即拒绝的审核模型。 */
    static class FlaggingModeration implements ModerationModel {
        private final boolean inputFlagged;
        private final boolean outputFlagged;

        FlaggingModeration(boolean inputFlagged, boolean outputFlagged) {
            this.inputFlagged = inputFlagged;
            this.outputFlagged = outputFlagged;
        }

        @Override
        public ModerationResult moderate(String text) {
            if (inputFlagged && "BAD".equals(text)) {
                return ModerationResult.flagged(ModerationCategory.HATE, "input rejected");
            }
            if (outputFlagged && "answer".equals(text)) {
                return ModerationResult.flagged(ModerationCategory.HATE, "output rejected");
            }
            return ModerationResult.clean();
        }
    }

    /** 直接返回固定 JSON 的输出端口（测试用替身）。 */
    static class FixedJson implements JsonOutputPort {
        private final String value;

        FixedJson(String value) {
            this.value = value;
        }

        @Override
        public String completeJson(LlmRequest request) {
            return value;
        }

        @Override
        public void streamJson(LlmRequest request, LlmStreamHandler handler) {
            if (handler != null) {
                handler.onToken(value, null);
                handler.onComplete();
            }
        }
    }

    interface Chat {
        @UserMessage("{{it}}")
        String ask(String question);
    }

    interface Extractor {
        @JsonResult
        @UserMessage("{{it}}")
        String extract(String text);
    }

    @Test
    void moderationBlocksFlaggedInput() {
        Chat chat = AiServices.builder(Chat.class)
                .llm(new RecordingLlm())
                .moderation(new FlaggingModeration(true, false))
                .build();
        assertThrows(ModerationException.class, () -> chat.ask("BAD"));
    }

    @Test
    void moderationBlocksFlaggedOutput() {
        Chat chat = AiServices.builder(Chat.class)
                .llm(new RecordingLlm())
                .moderation(new FlaggingModeration(false, true))
                .build();
        assertThrows(ModerationException.class, () -> chat.ask("ok"));
    }

    @Test
    void moderationPassesCleanContent() {
        Chat chat = AiServices.builder(Chat.class)
                .llm(new RecordingLlm())
                .moderation(new FlaggingModeration(false, false))
                .build();
        assertEquals("answer", chat.ask("ok"));
    }

    @Test
    void jsonResultRoutesThroughJsonOutputPort() {
        Extractor extractor = AiServices.builder(Extractor.class)
                .llm(new RecordingLlm())
                .jsonOutput(new FixedJson("{\"a\":1}"))
                .build();
        assertEquals("{\"a\":1}", extractor.extract("anything"));
    }

    @Test
    void jsonResultIgnoredWithoutAnnotation() {
        Chat chat = AiServices.builder(Chat.class)
                .llm(new RecordingLlm())
                .jsonOutput(new FixedJson("{\"a\":1}"))
                .build();
        // 没有 @JsonResult 的方法仍走普通文本返回，不会替换为固定 JSON
        assertEquals("answer", chat.ask("ok"));
    }

    interface StreamExtractor {
        @JsonResult
        @UserMessage("{{it}}")
        String extract(String text, LlmStreamHandler handler);
    }

    @Test
    void jsonResultStreamingForwardsOnlyJsonBody() {
        final StringBuilder received = new StringBuilder();
        LlmStreamHandler handler = new LlmStreamHandler() {
            @Override
            public void onToken(String content, String reasoningContent) {
                if (content != null) {
                    received.append(content);
                }
            }

            @Override
            public void onComplete() {
            }

            @Override
            public void onError(String message) {
            }
        };
        StreamExtractor extractor = AiServices.builder(StreamExtractor.class)
                .llm(new RecordingLlm())
                .jsonOutput(new FixedJson("{\"a\":1}"))
                .build();
        extractor.extract("anything", handler);
        assertEquals("{\"a\":1}", received.toString());
    }

    /** 流式 JSON 会剥离前导散文与 markdown 围栏，仅透传 JSON 主体。 */
    @Test
    void streamingJsonStripsPreambleAndFences() {
        // 一次性吐出「散文 + 围栏 + JSON」，验证透传过滤
        RecordingLlm llm = new RecordingLlm() {
            @Override
            protected void doStream(LlmRequest request, LlmStreamHandler h) {
                h.onToken("Sure:\n```json\n{\"a\":1}\n```", null);
                h.onComplete();
            }
        };
        final StringBuilder received = new StringBuilder();
        LlmStreamHandler handler = new LlmStreamHandler() {
            @Override
            public void onToken(String content, String reasoningContent) {
                if (content != null) {
                    received.append(content);
                }
            }

            @Override
            public void onComplete() {
            }

            @Override
            public void onError(String message) {
            }
        };
        DefaultJsonOutputPort.builder(llm).build().streamJson(LlmRequest.systemUser("s", "u"), handler);
        assertEquals("{\"a\":1}", received.toString());
    }
}
