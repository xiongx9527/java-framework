package com.xiongx9527.fw.plus.ai.base.memory;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import java.util.List;

/**
 * 对话记忆存储端口：消息的持久化位置。
 *
 * <p>由产品注入具体实现（内存 / 数据库 / Redis / 对象存储）。{@link ChatMemory} 只负责
 * 窗口裁剪与读写编排，因此换存储不需要改任何会话逻辑。</p>
 */
public interface ChatMemoryStore {

    /** 读取会话消息；不存在时返回空列表（不返回 {@code null}）。 */
    List<ChatMessage> getMessages(String memoryId);

    /** 覆盖写入会话消息；空列表等价于清空该会话。 */
    void updateMessages(String memoryId, List<ChatMessage> messages);

    /** 删除会话消息；默认实现为写入空列表。 */
    default void deleteMessages(String memoryId) {
        updateMessages(memoryId, java.util.Collections.<ChatMessage>emptyList());
    }
}
