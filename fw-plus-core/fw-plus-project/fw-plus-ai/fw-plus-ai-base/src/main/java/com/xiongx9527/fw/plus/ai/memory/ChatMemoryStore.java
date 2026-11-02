package com.xiongx9527.fw.plus.ai.memory;

import com.xiongx9527.fw.plus.ai.message.ChatMessage;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** 对话记忆存储端口，镜像 langchain4j 1.23.0 的 {@code dev.langchain4j.store.memory.chat.ChatMemoryStore}（Spring AI 等价 ChatMemoryRepository）。 */
public interface ChatMemoryStore {
    List<ChatMessage> getMessages(Object memoryId);
    void updateMessages(Object memoryId, List<ChatMessage> messages);
    void deleteMessages(Object memoryId);
    default CompletableFuture<List<ChatMessage>> getMessagesAsync(Object memoryId) { return CompletableFuture.completedFuture(getMessages(memoryId)); }
    default CompletableFuture<Void> updateMessagesAsync(Object memoryId, List<ChatMessage> messages) { updateMessages(memoryId, messages); return CompletableFuture.completedFuture(null); }
    default CompletableFuture<Void> deleteMessagesAsync(Object memoryId) { deleteMessages(memoryId); return CompletableFuture.completedFuture(null); }
}
