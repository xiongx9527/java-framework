package com.xiongx9527.fw.plus.ai.nativeimpl.memory;

import com.xiongx9527.fw.plus.ai.base.memory.ConversationMemoryAddRequest;
import com.xiongx9527.fw.plus.ai.base.memory.ConversationMemoryException;
import com.xiongx9527.fw.plus.ai.base.memory.ConversationMemoryScope;
import com.xiongx9527.fw.plus.ai.base.memory.ConversationMemorySearchRequest;
import com.xiongx9527.fw.plus.ai.base.memory.MemoryRecord;
import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryConversationMemoryPortTest {

    private InMemoryConversationMemoryPort port = new InMemoryConversationMemoryPort();

    @Test
    void addStoresEachMessageAsMemory() {
        List<MemoryRecord> records = port.add(ConversationMemoryAddRequest.builder()
                .message(ChatMessage.user("我有花生过敏"))
                .message(ChatMessage.assistant("好的，已记录"))
                .userId("u1")
                .build());
        assertThat(records).hasSize(2);
        assertThat(records).allSatisfy(r -> {
            assertThat(r.getId()).isNotNull();
            assertThat(r.getUserId()).isEqualTo("u1");
        });
    }

    @Test
    void searchReturnsOverlappingMemoriesByScope() {
        port.add(ConversationMemoryAddRequest.builder()
                .message(ChatMessage.user("我对海鲜过敏，尤其是虾"))
                .userId("u1").build());
        port.add(ConversationMemoryAddRequest.builder()
                .message(ChatMessage.user("我喜欢跑步"))
                .userId("u1").build());

        List<MemoryRecord> hits = port.search(ConversationMemorySearchRequest.builder()
                .query("海鲜过敏").userId("u1").build());
        assertThat(hits).isNotEmpty();
        assertThat(hits.get(0).getMemory()).contains("海鲜");
    }

    @Test
    void searchFiltersByScope() {
        port.add(ConversationMemoryAddRequest.builder()
                .message(ChatMessage.user("secret")).userId("u1").build());
        List<MemoryRecord> hits = port.search(ConversationMemorySearchRequest.builder()
                .query("secret").userId("u2").build());
        assertThat(hits).isEmpty();
    }

    @Test
    void listReturnsMemoriesInScope() {
        port.add(ConversationMemoryAddRequest.builder()
                .message(ChatMessage.user("a")).userId("u1").build());
        port.add(ConversationMemoryAddRequest.builder()
                .message(ChatMessage.user("b")).userId("u2").build());
        List<MemoryRecord> list = port.list(ConversationMemoryScope.builder().userId("u1").build());
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getMemory()).isEqualTo("a");
    }

    @Test
    void deleteRemovesSingleMemory() {
        List<MemoryRecord> records = port.add(ConversationMemoryAddRequest.builder()
                .message(ChatMessage.user("x")).userId("u1").build());
        String id = records.get(0).getId();
        port.delete(id);
        assertThat(port.list(ConversationMemoryScope.builder().userId("u1").build())).isEmpty();
    }

    @Test
    void deleteAllRemovesScope() {
        port.add(ConversationMemoryAddRequest.builder().message(ChatMessage.user("a")).userId("u1").build());
        port.add(ConversationMemoryAddRequest.builder().message(ChatMessage.user("b")).userId("u1").build());
        port.deleteAll(ConversationMemoryScope.builder().userId("u1").build());
        assertThat(port.list(ConversationMemoryScope.builder().userId("u1").build())).isEmpty();
    }

    @Test
    void addRequiresAtLeastOneMessage() {
        assertThatThrownBy(() -> port.add(ConversationMemoryAddRequest.builder().userId("u1").build()))
                .isInstanceOf(ConversationMemoryException.class);
    }

    @Test
    void searchRequiresQuery() {
        assertThatThrownBy(() -> port.search(ConversationMemorySearchRequest.builder().userId("u1").build()))
                .isInstanceOf(ConversationMemoryException.class);
    }

    @Test
    void deleteAllRequiresScope() {
        assertThatThrownBy(() -> port.deleteAll(ConversationMemoryScope.builder().build()))
                .isInstanceOf(ConversationMemoryException.class);
    }
}
