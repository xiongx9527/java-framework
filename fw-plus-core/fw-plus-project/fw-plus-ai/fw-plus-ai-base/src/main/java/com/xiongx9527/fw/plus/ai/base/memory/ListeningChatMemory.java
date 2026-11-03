package com.xiongx9527.fw.plus.ai.base.memory;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import java.util.ArrayList;
import java.util.List;

/**
 * 给任意 {@link ChatMemory} 挂监听器（装饰器）。
 */
public class ListeningChatMemory implements ChatMemory {

    private final ChatMemory delegate;

    private final List<ChatMemoryListener> listeners;

    public ListeningChatMemory(ChatMemory delegate, List<ChatMemoryListener> listeners) {
        if (delegate == null) {
            throw new IllegalArgumentException("chat memory is required");
        }
        this.delegate = delegate;
        this.listeners = listeners == null ? new ArrayList<ChatMemoryListener>() : listeners;
    }

    @Override
    public String id() {
        return delegate.id();
    }

    @Override
    public void add(ChatMessage message) {
        delegate.add(message);
        for (ChatMemoryListener listener : listeners) {
            try {
                listener.onAdd(delegate.id(), message);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }

    @Override
    public List<ChatMessage> messages() {
        return delegate.messages();
    }

    @Override
    public void clear() {
        delegate.clear();
        for (ChatMemoryListener listener : listeners) {
            try {
                listener.onClear(delegate.id());
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }
}
