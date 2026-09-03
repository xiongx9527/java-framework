package dev.langchain4j.service.memory;

import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;

import dev.langchain4j.Internal;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Internal
public class ChatMemoryService {

    public static final String DEFAULT = "default";

    private ChatMemory defaultChatMemory;
    private Map<Object, ChatMemory> chatMemories;
    private ChatMemoryProvider chatMemoryProvider;

    public ChatMemoryService(ChatMemoryProvider chatMemoryProvider) {
        this(new ConcurrentHashMap<Object, ChatMemory>(), chatMemoryProvider);
    }

    public ChatMemoryService(Map<Object, ChatMemory> chatMemories, ChatMemoryProvider chatMemoryProvider) {
        this.chatMemories = ensureNotNull(chatMemories, "chatMemories");
        this.chatMemoryProvider = ensureNotNull(chatMemoryProvider, "chatMemoryProvider");
    }

    public ChatMemoryService(ChatMemory chatMemory) {
        this(new ConcurrentHashMap<Object, ChatMemory>(), chatMemory);
    }

    public ChatMemoryService(Map<Object, ChatMemory> chatMemories, ChatMemory defaultChatMemory) {
        this.chatMemories = ensureNotNull(chatMemories, "chatMemories");
        this.defaultChatMemory = ensureNotNull(defaultChatMemory, "defaultChatMemory");
    }

    public ChatMemory getOrCreateChatMemory(Object memoryId) {
        if (chatMemoryProvider != null) {
            return chatMemories.computeIfAbsent(memoryId, chatMemoryProvider::get);
        }
        ChatMemory mappedMemory = chatMemories.get(memoryId);
        return mappedMemory != null ? mappedMemory : defaultChatMemory;
    }

    public ChatMemory getChatMemory(Object memoryId) {
        if (chatMemoryProvider != null) {
            return chatMemories.get(memoryId);
        }
        ChatMemory mappedMemory = chatMemories.get(memoryId);
        return mappedMemory != null ? mappedMemory : memoryId == DEFAULT ? defaultChatMemory : null;
    }

    public ChatMemory evictChatMemory(Object memoryId) {
        return chatMemories.remove(memoryId);
    }

    public void clearAll() {
        chatMemories.values().forEach(ChatMemory::clear);
        chatMemories.clear();
    }

    public Collection<Object> getChatMemoryIDs() {
        return chatMemories.keySet();
    }

    public Collection<ChatMemory> getChatMemories() {
        return chatMemories.values();
    }
}
