package com.xiongx9527.fw.plus.ai.base.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 对话记忆端口的抽象基类：统一收口监听器派发，具体提供商只需实现原子操作。
 *
 * <p>对齐 {@code AbstractLlmPort} 的思路——把「事件通知」从每个端口方法里抽出来，
 * 提供商调用 {@link #notifyAdd} / {@link #notifySearch} / {@link #notifyDelete} /
 * {@link #notifyError} 即可，监听器实现（日志 / 计费 / 追踪）只需写一次。</p>
 */
public abstract class AbstractConversationMemoryPort implements ConversationMemoryPort {

    protected final List<ConversationMemoryListener> listeners;

    protected AbstractConversationMemoryPort(List<ConversationMemoryListener> listeners) {
        this.listeners = listeners == null ? Collections.<ConversationMemoryListener>emptyList()
                : Collections.unmodifiableList(new ArrayList<ConversationMemoryListener>(listeners));
    }

    @Override
    public List<ConversationMemoryListener> listeners() {
        return listeners;
    }

    protected void notifyAdd(ConversationMemoryScope scope, List<MemoryRecord> records) {
        for (ConversationMemoryListener l : listeners) {
            try {
                l.onAdd(scope, records);
            } catch (RuntimeException ignore) {
                // 监听器不应阻断主流程
            }
        }
    }

    protected void notifySearch(ConversationMemoryScope scope, List<MemoryRecord> records) {
        for (ConversationMemoryListener l : listeners) {
            try {
                l.onSearch(scope, records);
            } catch (RuntimeException ignore) {
                // 监听器不应阻断主流程
            }
        }
    }

    protected void notifyDelete(ConversationMemoryScope scope, String memoryId) {
        for (ConversationMemoryListener l : listeners) {
            try {
                l.onDelete(scope, memoryId);
            } catch (RuntimeException ignore) {
                // 监听器不应阻断主流程
            }
        }
    }

    protected void notifyError(String message, ConversationMemoryScope scope) {
        for (ConversationMemoryListener l : listeners) {
            try {
                l.onError(message, scope);
            } catch (RuntimeException ignore) {
                // 监听器不应阻断主流程
            }
        }
    }
}
