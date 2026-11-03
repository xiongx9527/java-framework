package com.xiongx9527.fw.plus.ai.base.memory;

import com.xiongx9527.fw.plus.ai.base.event.AiListener;
import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;

/**
 * 对话记忆事件：追加消息 / 清空。
 */
public interface ChatMemoryListener extends AiListener {

    /** 追加一条消息后触发。 */
    default void onAdd(String memoryId, ChatMessage message) {
    }

    /** 会话清空后触发。 */
    default void onClear(String memoryId) {
    }
}
