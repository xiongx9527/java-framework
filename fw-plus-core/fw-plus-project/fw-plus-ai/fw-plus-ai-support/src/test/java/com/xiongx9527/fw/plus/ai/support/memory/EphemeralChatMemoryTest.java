package com.xiongx9527.fw.plus.ai.support.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.base.memory.ChatMemory;
import com.xiongx9527.fw.plus.ai.base.memory.ChatMemoryStore;
import com.xiongx9527.fw.plus.ai.base.memory.InMemoryChatMemoryStore;
import com.xiongx9527.fw.plus.ai.base.memory.MessageWindowChatMemory;
import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.ChatRole;
import com.xiongx9527.fw.plus.ai.base.tool.ToolExecutionRequest;

import org.junit.jupiter.api.Test;

import java.util.List;

class EphemeralChatMemoryTest {

    private static EphemeralChatMemory build(ChatMemoryStore store) {
        ChatMemory delegate = MessageWindowChatMemory.builder().id("m1").store(store).build();
        return EphemeralChatMemory.builder(delegate, store).build();
    }

    private static String textOf(ChatMessage m) {
        return m.getContent() == null ? "" : m.getContent();
    }

    @Test
    void nonToolTurnPersistsFinalAnswerAcrossNewUser() {
        ChatMemoryStore store = new InMemoryChatMemoryStore();
        EphemeralChatMemory memory = build(store);

        memory.add(ChatMessage.system("sys"));
        memory.add(ChatMessage.user("q1"));
        memory.add(ChatMessage.assistant("a1"));
        // 新一轮开始
        memory.add(ChatMessage.user("q2"));

        List<ChatMessage> messages = memory.messages();
        assertEquals(ChatRole.SYSTEM, messages.get(0).getRole());
        assertEquals("q1", textOf(messages.get(1)));
        // 上一轮的最终回答被沉淀进持久历史
        assertEquals("a1", textOf(messages.get(2)));
        assertEquals("q2", textOf(messages.get(3)));
    }

    @Test
    void toolRoundTripDropsFinalAnswerFromNextTurn() {
        ChatMemoryStore store = new InMemoryChatMemoryStore();
        EphemeralChatMemory memory = build(store);

        memory.add(ChatMessage.system("sys"));
        memory.add(ChatMessage.user("q1"));
        memory.add(ChatMessage.assistantWithToolCalls(
                java.util.Collections.singletonList(ToolExecutionRequest.of("search", "{}")), "thinking"));
        memory.add(ChatMessage.tool("c1", "search", "tool-result"));
        memory.add(ChatMessage.assistant("a2 final"));
        // 新一轮开始：上一轮用过工具，最终回答不应沉淀
        memory.add(ChatMessage.user("q2"));

        List<ChatMessage> messages = memory.messages();
        boolean hasA2 = false;
        for (ChatMessage m : messages) {
            if ("a2 final".equals(textOf(m))) {
                hasA2 = true;
            }
        }
        assertFalse(hasA2, "tool round-trip final answer must not be persisted");
        assertEquals("q1", textOf(messages.get(1)));
        assertEquals("q2", textOf(messages.get(messages.size() - 1)));
    }

    @Test
    void stripRagFromHistoryRemovesInjectedSegment() {
        ChatMemoryStore store = new InMemoryChatMemoryStore();
        EphemeralChatMemory memory = build(store);

        String rag = "what is X?" + EphemeralChatMemory.DEFAULT_RAG_SEPARATOR + "RAG context here";
        memory.add(ChatMessage.system("sys"));
        memory.add(ChatMessage.user(rag));
        memory.add(ChatMessage.assistant("a1"));
        memory.add(ChatMessage.user("q2"));

        List<ChatMessage> messages = memory.messages();
        // 历史用户消息被剥离 RAG 段
        assertEquals("what is X?", textOf(messages.get(1)));
        assertTrue(textOf(messages.get(1)).indexOf("RAG context here") < 0);
    }

    @Test
    void dropAllAiStrategyKeepsNoAssistant() {
        ChatMemoryStore store = new InMemoryChatMemoryStore();
        ChatMemory delegate = MessageWindowChatMemory.builder().id("m1").store(store).build();
        EphemeralChatMemory memory = EphemeralChatMemory.builder(delegate, store)
                .aiPersistenceStrategy(AiPersistenceStrategy.DROP_ALL_AI)
                .build();

        memory.add(ChatMessage.system("sys"));
        memory.add(ChatMessage.user("q1"));
        memory.add(ChatMessage.assistant("a1"));
        memory.add(ChatMessage.user("q2"));

        List<ChatMessage> messages = memory.messages();
        for (ChatMessage m : messages) {
            assertFalse(m.getRole() == ChatRole.ASSISTANT, "DROP_ALL_AI must not persist any assistant message");
        }
    }
}
