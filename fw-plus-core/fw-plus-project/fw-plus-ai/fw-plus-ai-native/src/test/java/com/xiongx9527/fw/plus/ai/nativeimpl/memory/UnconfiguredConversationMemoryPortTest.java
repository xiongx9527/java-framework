package com.xiongx9527.fw.plus.ai.nativeimpl.memory;

import com.xiongx9527.fw.plus.ai.base.memory.ConversationMemoryAddRequest;
import com.xiongx9527.fw.plus.ai.base.memory.ConversationMemoryException;
import com.xiongx9527.fw.plus.ai.base.memory.ConversationMemoryScope;
import com.xiongx9527.fw.plus.ai.base.memory.ConversationMemorySearchRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnconfiguredConversationMemoryPortTest {

    private final UnconfiguredConversationMemoryPort port = new UnconfiguredConversationMemoryPort();

    @Test
    void addThrows() {
        assertThatThrownBy(() -> port.add(ConversationMemoryAddRequest.builder().build()))
                .isInstanceOf(ConversationMemoryException.class);
    }

    @Test
    void searchThrows() {
        assertThatThrownBy(() -> port.search(ConversationMemorySearchRequest.builder().build()))
                .isInstanceOf(ConversationMemoryException.class);
    }

    @Test
    void listThrows() {
        assertThatThrownBy(() -> port.list(ConversationMemoryScope.builder().build()))
                .isInstanceOf(ConversationMemoryException.class);
    }

    @Test
    void deleteThrows() {
        assertThatThrownBy(() -> port.delete("id")).isInstanceOf(ConversationMemoryException.class);
    }

    @Test
    void deleteAllThrows() {
        assertThatThrownBy(() -> port.deleteAll(ConversationMemoryScope.builder().build()))
                .isInstanceOf(ConversationMemoryException.class);
    }
}
