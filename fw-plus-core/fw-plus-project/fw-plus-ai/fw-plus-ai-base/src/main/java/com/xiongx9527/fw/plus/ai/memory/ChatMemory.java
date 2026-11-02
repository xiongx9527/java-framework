package com.xiongx9527.fw.plus.ai.memory;

import com.xiongx9527.fw.plus.ai.message.ChatMessage;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** 对话记忆端口，镜像 langchain4j 1.23.0 的 {@code dev.langchain4j.memory.ChatMemory}（Spring AI 等价 ChatMemory / ChatMemoryRepository）。 */
public interface ChatMemory {
    Object id();
    void add(ChatMessage message);
    default void add(ChatMessage... messages) { if (messages != null) for (ChatMessage m : messages) add(m); }
    default void add(Iterable<ChatMessage> messages) { if (messages != null) for (ChatMessage m : messages) add(m); }
    default void set(ChatMessage... messages) { clear(); add(messages); }
    default void set(Iterable<ChatMessage> messages) { clear(); add(messages); }
    List<ChatMessage> messages();
    default CompletableFuture<Void> addAsync(List<ChatMessage> messages) { add(messages); return CompletableFuture.completedFuture(null); }
    default CompletableFuture<Void> setAsync(List<ChatMessage> messages) { set(messages); return CompletableFuture.completedFuture(null); }
    default CompletableFuture<List<ChatMessage>> messagesAsync() { return CompletableFuture.completedFuture(messages()); }
    void clear();
}
