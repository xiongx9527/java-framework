package com.xiongx9527.fw.plus.ai.base.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.base.memory.ChatMemoryProvider;
import com.xiongx9527.fw.plus.ai.base.memory.InMemoryChatMemoryStore;
import com.xiongx9527.fw.plus.ai.base.memory.MessageWindowChatMemory;
import com.xiongx9527.fw.plus.ai.base.model.AbstractLlmPort;
import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmResponse;
import com.xiongx9527.fw.plus.ai.base.model.LlmStreamHandler;
import com.xiongx9527.fw.plus.ai.base.service.AiServices.ChatCall;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AiServicesChatCallTest {

    static class RecordingLlm extends AbstractLlmPort {
        final List<LlmRequest> requests = new ArrayList<LlmRequest>();

        @Override
        protected void doStream(LlmRequest request, LlmStreamHandler handler) {
            requests.add(request);
            handler.onToken("answer", null);
            handler.onComplete();
        }
    }

    static class CapturingHandler implements LlmStreamHandler {
        final StringBuilder content = new StringBuilder();
        boolean completed;

        @Override
        public void onToken(String c, String reasoning) {
            if (c != null) {
                content.append(c);
            }
        }

        @Override
        public void onComplete() {
            completed = true;
        }

        @Override
        public void onError(String message) {
        }
    }

    @Test
    void callWithRawStringUsesDirectUserTextAndRendersSystemVars() {
        RecordingLlm llm = new RecordingLlm();
        ChatCall call = AiServices.fluent()
                .llm(llm)
                .systemText("You are a bot. now={{now}}")
                .buildChatCall();

        String answer = call.call("hello world");
        assertEquals("answer", answer);

        List<ChatMessage> messages = llm.requests.get(0).getMessages();
        assertTrue(messages.get(0).getContent().startsWith("You are a bot. now="));
        assertTrue(!messages.get(0).getContent().contains("{{now}}"));
        assertEquals("hello world", messages.get(1).getContent());
    }

    @Test
    void callWithMapFillsUserTemplateAndCustomVariables() {
        RecordingLlm llm = new RecordingLlm();
        ChatCall call = AiServices.fluent()
                .llm(llm)
                .systemText("System")
                .userTemplate("Q: {{q}} from {{name}}")
                .variable("name", () -> "Bob")
                .buildChatCall();

        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("q", "1+1=?");
        String answer = call.call(vars);
        assertEquals("answer", answer);

        List<ChatMessage> messages = llm.requests.get(0).getMessages();
        assertEquals("Q: 1+1=? from Bob", messages.get(1).getContent());
    }

    @Test
    void callRawReturnsLlmResponseWithUsage() {
        RecordingLlm llm = new RecordingLlm();
        ChatCall call = AiServices.fluent()
                .llm(llm)
                .systemText("System")
                .returnType(LlmResponse.class)
                .buildChatCall();

        Object raw = call.callRaw("hi");
        assertNotNull(raw);
        assertTrue(raw instanceof LlmResponse);
        assertEquals("answer", ((LlmResponse) raw).getContent());
    }

    @Test
    void memoryKeptAcrossCallsViaSupplier() {
        RecordingLlm llm = new RecordingLlm();
        final InMemoryChatMemoryStore store = new InMemoryChatMemoryStore();
        ChatMemoryProvider provider = memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId).store(store).build();
        ChatCall call = AiServices.fluent()
                .llm(llm)
                .systemText("System")
                .chatMemoryProvider(provider)
                .memoryId(() -> "c1")
                .buildChatCall();

        call.call("first");
        call.call("second");

        List<ChatMessage> second = llm.requests.get(1).getMessages();
        assertTrue(second.size() >= 4);
        assertEquals("System", second.get(0).getContent());
        assertEquals("first", second.get(1).getContent());
        assertEquals("answer", second.get(2).getContent());
        assertEquals("second", second.get(second.size() - 1).getContent());
    }

    @Test
    void memoryIdResolvedDynamicallyFromBusinessInput() {
        RecordingLlm llm = new RecordingLlm();
        final InMemoryChatMemoryStore store = new InMemoryChatMemoryStore();
        ChatMemoryProvider provider = memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId).store(store).build();
        ChatCall call = AiServices.fluent()
                .llm(llm)
                .systemText("System")
                .chatMemoryProvider(provider)
                .memoryIdFromVar("sessionId")
                .buildChatCall();

        Map<String, Object> varsA = new HashMap<String, Object>();
        varsA.put("sessionId", "user-1");
        varsA.put("q", "first");
        call.call(varsA);

        Map<String, Object> varsB = new HashMap<String, Object>();
        varsB.put("sessionId", "user-2");
        varsB.put("q", "second");
        call.call(varsB);

        // 两个不同业务输入对应两个不同会话，记忆互不干扰
        List<ChatMessage> second = llm.requests.get(1).getMessages();
        assertEquals("second", second.get(second.size() - 1).getContent());
        // user-2 是全新会话，历史里不应出现 user-1 的首轮内容
        boolean sawUser1First = false;
        for (ChatMessage m : second) {
            if ("first".equals(m.getContent())) {
                sawUser1First = true;
            }
        }
        assertTrue(!sawUser1First);
    }

    @Test
    void streamDispatchesTokensToHandler() {
        RecordingLlm llm = new RecordingLlm();
        ChatCall call = AiServices.fluent()
                .llm(llm)
                .systemText("System")
                .buildChatCall();

        CapturingHandler handler = new CapturingHandler();
        call.stream("stream me", handler);

        assertEquals("answer", handler.content.toString());
        assertTrue(handler.completed);
    }
}
