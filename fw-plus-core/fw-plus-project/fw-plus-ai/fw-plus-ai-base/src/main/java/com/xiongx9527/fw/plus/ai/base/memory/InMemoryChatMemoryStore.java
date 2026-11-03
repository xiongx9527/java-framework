package com.xiongx9527.fw.plus.ai.base.memory;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存存储：单机与测试用。多实例部署必须换成共享存储，否则会话在实例间不可见。
 */
public class InMemoryChatMemoryStore implements ChatMemoryStore {

    private final Map<String, List<ChatMessage>> messages = new ConcurrentHashMap<String, List<ChatMessage>>();

    @Override
    public List<ChatMessage> getMessages(String memoryId) {
        List<ChatMessage> stored = memoryId == null ? null : messages.get(memoryId);
        return stored == null ? new ArrayList<ChatMessage>() : new ArrayList<ChatMessage>(stored);
    }

    @Override
    public void updateMessages(String memoryId, List<ChatMessage> value) {
        if (memoryId == null) {
            return;
        }
        if (value == null || value.isEmpty()) {
            messages.remove(memoryId);
            return;
        }
        messages.put(memoryId, Collections.unmodifiableList(new ArrayList<ChatMessage>(value)));
    }
}
