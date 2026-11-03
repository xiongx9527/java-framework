package com.xiongx9527.fw.plus.ai.base.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.ChatRole;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChatMemoryTest {

    @Test
    void windowDropsOldestButKeepsSystemMessage() {
        ChatMemory memory = MessageWindowChatMemory.builder().id("c1").maxMessages(3).build();
        memory.add(ChatMessage.system("be nice"));
        memory.add(ChatMessage.user("q1"));
        memory.add(ChatMessage.assistant("a1"));
        memory.add(ChatMessage.user("q2"));

        List<ChatMessage> messages = memory.messages();

        assertEquals(3, messages.size());
        assertEquals(ChatRole.SYSTEM, messages.get(0).getRole());
        assertEquals("a1", messages.get(1).getContent());
        assertEquals("q2", messages.get(2).getContent());
    }

    @Test
    void sharedStoreMakesMemoryVisibleToOtherInstances() {
        ChatMemoryStore store = new InMemoryChatMemoryStore();
        ChatMemory first = MessageWindowChatMemory.builder().id("c2").store(store).build();
        ChatMemory second = MessageWindowChatMemory.builder().id("c2").store(store).build();

        first.add(ChatMessage.user("hello"));

        assertEquals(1, second.messages().size());
        second.clear();
        assertTrue(first.messages().isEmpty());
    }

    @Test
    void listenerSeesAddAndClear() {
        final int[] added = {0};
        final int[] cleared = {0};
        ChatMemory memory = new ListeningChatMemory(
                MessageWindowChatMemory.builder().id("c3").build(),
                java.util.Collections.singletonList(new ChatMemoryListener() {
                    @Override
                    public void onAdd(String memoryId, ChatMessage message) {
                        added[0]++;
                    }

                    @Override
                    public void onClear(String memoryId) {
                        cleared[0]++;
                    }
                }));

        memory.add(ChatMessage.user("x"));
        memory.clear();

        assertEquals(1, added[0]);
        assertEquals(1, cleared[0]);
    }
}
