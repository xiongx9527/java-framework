package com.xiongx9527.fw.plus.ai.base.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.base.memory.ChatMemoryProvider;
import com.xiongx9527.fw.plus.ai.base.memory.InMemoryChatMemoryStore;
import com.xiongx9527.fw.plus.ai.base.memory.MessageWindowChatMemory;
import com.xiongx9527.fw.plus.ai.base.model.AbstractLlmPort;
import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmStreamHandler;
import com.xiongx9527.fw.plus.ai.base.model.LlmTokenStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class AiServicesTest {

    /** 记录请求并返回固定回复。 */
    static class RecordingLlm extends AbstractLlmPort {

        private final List<LlmRequest> requests = new ArrayList<LlmRequest>();

        @Override
        protected void doStream(LlmRequest request, LlmStreamHandler handler) {
            requests.add(request);
            handler.onToken("answer", null);
            handler.onComplete();
        }
    }

    interface Assistant {

        @SystemMessage("You are a translator.")
        @UserMessage("Translate {{text}} into {{lang}}")
        String translate(@V("text") String text, @V("lang") String lang);
    }

    interface Chatty {

        @UserMessage("{{it}}")
        String ask(@MemoryId String memoryId, String question);
    }

    interface StreamingAssistant {

        @UserMessage("{{it}}")
        LlmTokenStream ask(String question);
    }

    @Test
    void fillsTemplateVariablesAndSystemMessage() {
        RecordingLlm llm = new RecordingLlm();
        Assistant assistant = AiServices.builder(Assistant.class).llm(llm).build();

        assertEquals("answer", assistant.translate("hello", "zh"));

        List<ChatMessage> messages = llm.requests.get(0).getMessages();
        assertEquals(2, messages.size());
        assertEquals("You are a translator.", messages.get(0).getContent());
        assertEquals("Translate hello into zh", messages.get(1).getContent());
    }

    @Test
    void memoryKeepsConversation() {
        RecordingLlm llm = new RecordingLlm();
        final InMemoryChatMemoryStore store = new InMemoryChatMemoryStore();
        ChatMemoryProvider provider = memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId).store(store).build();
        Chatty chatty = AiServices.builder(Chatty.class).llm(llm).chatMemoryProvider(provider).build();

        chatty.ask("c1", "first");
        chatty.ask("c1", "second");

        List<ChatMessage> second = llm.requests.get(1).getMessages();
        assertTrue(second.size() >= 3);
        assertEquals("first", second.get(0).getContent());
        assertEquals("answer", second.get(1).getContent());
        assertEquals("second", second.get(second.size() - 1).getContent());
    }

    @Test
    void returnsDeferredTokenStreamAndDeliversCallbacksAfterStart() {
        final AtomicBoolean requested = new AtomicBoolean();
        AbstractLlmPort llm = new AbstractLlmPort() {
            @Override
            protected void doStream(LlmRequest request, LlmStreamHandler handler) {
                requested.set(true);
                handler.onToken("answer", "reasoning");
                handler.onComplete();
            }
        };
        StreamingAssistant assistant = AiServices.builder(StreamingAssistant.class).llm(llm).build();
        List<String> events = new ArrayList<String>();

        LlmTokenStream stream = assistant.ask("question")
                .onPartialResponse(content -> events.add(content))
                .onPartialThinking(reasoning -> events.add(reasoning))
                .onComplete(() -> events.add("complete"));

        assertEquals(false, requested.get());
        stream.start();

        assertEquals(true, requested.get());
        assertEquals(java.util.Arrays.asList("answer", "reasoning", "complete"), events);
    }
}
