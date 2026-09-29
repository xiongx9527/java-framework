package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore.module.Langchain4jChatMemoryStoreModule;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import com.xiongx9527.fw.plus.common.util.StringUtils;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import java.util.List;

public class MagicDynamicChatMemoryStore implements ChatMemoryStore {

    private final Langchain4jChatMemoryStoreModule chatMemoryStore;

    private final String key;

    public MagicDynamicChatMemoryStore(Langchain4jChatMemoryStoreModule chatMemoryStore, String key) {
        this.chatMemoryStore = chatMemoryStore;
        this.key = key;
    }

    private ChatMemoryStore getChatMemoryStore() {
        try{
            return chatMemoryStore.getDynamicAttribute(key);
        } catch (Exception e) {
            if (StringUtils.isEmpty(key)) {
               return SpringUtils.getBean(ChatMemoryStore.class);
            }
            return SpringUtils.getBean(key,ChatMemoryStore.class);
        }
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        return getChatMemoryStore().getMessages(memoryId);
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        getChatMemoryStore().updateMessages(memoryId, messages);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        getChatMemoryStore().deleteMessages(memoryId);
    }
}
