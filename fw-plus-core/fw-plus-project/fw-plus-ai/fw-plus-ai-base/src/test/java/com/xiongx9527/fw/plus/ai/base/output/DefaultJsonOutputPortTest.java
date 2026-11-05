package com.xiongx9527.fw.plus.ai.base.output;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.ChatRole;
import com.xiongx9527.fw.plus.ai.base.model.LlmPort;
import com.xiongx9527.fw.plus.ai.base.model.ModelInfo;
import com.xiongx9527.fw.plus.ai.base.model.ModelValidation;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmStreamHandler;
import com.xiongx9527.fw.plus.ai.base.model.LlmTokenStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import org.junit.jupiter.api.Test;

class DefaultJsonOutputPortTest {

    /** 按脚本依次返回正文的假模型，并记录每次请求。 */
    private static class ScriptedPort implements LlmPort {


        private final Queue<String> contents = new ArrayDeque<String>();

        private final List<LlmRequest> calls = new ArrayList<LlmRequest>();

        ScriptedPort(String... responses) {
            for (String r : responses) {
                contents.add(r);
            }
        }

        @Override
        public LlmTokenStream stream(LlmRequest request) {
            calls.add(request);
            return LlmTokenStream.create(handler -> {
                String c = contents.poll();
                if (c != null) {
                    handler.onToken(c, null);
                }
                handler.onComplete();
            });
        }
    }

    @Test
    void returnsCleanJsonDirectly() {
        ScriptedPort port = new ScriptedPort("{\"a\":1}");
        String json = DefaultJsonOutputPort.builder(port).build().completeJson(LlmRequest.systemUser("s", "u"));
        assertEquals("{\"a\":1}", json);
    }

    @Test
    void extractsJsonFromFencedProse() {
        ScriptedPort port = new ScriptedPort("Here:\n```json\n{\"a\":1}\n```\nDone.");
        String json = DefaultJsonOutputPort.builder(port).build().completeJson(LlmRequest.systemUser("s", "u"));
        assertEquals("{\"a\":1}", json);
    }

    @Test
    void injectsJsonInstructionIntoSystemMessage() {
        ScriptedPort port = new ScriptedPort("{\"a\":1}");
        DefaultJsonOutputPort.builder(port).build().completeJson(LlmRequest.systemUser("base system", "u"));

        LlmRequest sent = port.calls.get(0);
        ChatMessage system = sent.getMessages().get(0);
        assertEquals(ChatRole.SYSTEM, system.getRole());
        assertTrue(system.getContent().contains(DefaultJsonOutputPort.DEFAULT_INSTRUCTION),
                "system message should carry the JSON instruction");
        assertTrue(system.getContent().startsWith("base system"),
                "original system text must be preserved");
    }

    @Test
    void prependsSystemMessageWhenMissing() {
        ScriptedPort port = new ScriptedPort("{\"a\":1}");
        DefaultJsonOutputPort.builder(port).build()
                .completeJson(LlmRequest.builder().addMessage(ChatMessage.user("u")).build());

        assertEquals(ChatRole.SYSTEM, port.calls.get(0).getMessages().get(0).getRole());
    }

    @Test
    void mergesJsonModeParams() {
        ScriptedPort port = new ScriptedPort("{\"a\":1}");
        DefaultJsonOutputPort.builder(port)
                .jsonModeParam("response_format", "json_object")
                .build()
                .completeJson(LlmRequest.systemUser("s", "u"));

        assertEquals("json_object", port.calls.get(0).getExtraParams().get("response_format"));
    }

    @Test
    void retriesWithCorrectionWhenParseFails() {
        // 第一次返回散文，第二次返回合法 JSON
        ScriptedPort port = new ScriptedPort("Sorry, I cannot.", "{\"a\":1}");
        String json = DefaultJsonOutputPort.builder(port).maxRetries(1).build()
                .completeJson(LlmRequest.systemUser("s", "u"));

        assertEquals("{\"a\":1}", json);
        assertEquals(2, port.calls.size());
        // 第二次请求应追加纠错提示
        assertTrue(port.calls.get(1).getMessages().get(port.calls.get(1).getMessages().size() - 1)
                .getContent().toLowerCase().contains("not valid json"));
    }

    @Test
    void throwsWhenJsonNeverProduced() {
        ScriptedPort port = new ScriptedPort("nope", "still nope");
        assertThrows(JsonOutputException.class,
                () -> DefaultJsonOutputPort.builder(port).maxRetries(1).build()
                        .completeJson(LlmRequest.systemUser("s", "u")));
    }

    @Test
    void repairFallbackFixesTruncatedOutput() {
        // 一次返回被截断的 JSON，重试仍失败，兜底修复补上缺失括号
        ScriptedPort port = new ScriptedPort("{\"a\":1,\"b\":2");
        String json = DefaultJsonOutputPort.builder(port).maxRetries(0).build()
                .completeJson(LlmRequest.systemUser("s", "u"));
        assertEquals("{\"a\":1,\"b\":2}", json);
    }

    @Test
    void fallbackHookReturnsCustomWhenRepairFails() {
        ScriptedPort port = new ScriptedPort("not json");
        String json = DefaultJsonOutputPort.builder(port)
                .maxRetries(0)
                .fallback(request -> "{\"fallback\":true}")
                .build()
                .completeJson(LlmRequest.systemUser("s", "u"));
        assertEquals("{\"fallback\":true}", json);
    }

    @Test
    void enforcesRetryUpperBound() {
        ScriptedPort port = new ScriptedPort("x");
        assertThrows(IllegalArgumentException.class,
                () -> DefaultJsonOutputPort.builder(port).maxRetries(DefaultJsonOutputPort.MAX_RETRIES + 1));
    }
}
