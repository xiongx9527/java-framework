package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LangChain4jModernChatContractTest {

    @Test
    public void shouldUseModernChatModelContract() {
        ChatModel model = new ChatModel() {
            @Override
            public ChatResponse chat(ChatRequest request) {
                return ChatResponse.builder().aiMessage(AiMessage.from("answer")).build();
            }
        };

        assertEquals("answer", model.chat("question"));
    }

    @Test
    public void shouldUseModernStreamingChatModelContract() {
        StreamingChatModel model = new StreamingChatModel() {
            @Override
            public void chat(ChatRequest request, StreamingChatResponseHandler handler) {
                handler.onPartialResponse("partial");
                handler.onCompleteResponse(ChatResponse.builder().aiMessage(AiMessage.from("complete")).build());
            }
        };
        AtomicReference<String> completion = new AtomicReference<String>();

        model.chat("question", new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
            }

            @Override
            public void onCompleteResponse(ChatResponse response) {
                completion.set(response.aiMessage().text());
            }

            @Override
            public void onError(Throwable error) {
                throw new AssertionError(error);
            }
        });

        assertEquals("complete", completion.get());
    }

    @Test
    public void shouldNotExposeRemovedLegacyChatContracts() {
        ClassLoader classLoader = getClass().getClassLoader();
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("dev.langchain4j.model.chat.ChatLanguageModel", false, classLoader));
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("dev.langchain4j.model.chat.StreamingChatLanguageModel", false, classLoader));
    }
}
