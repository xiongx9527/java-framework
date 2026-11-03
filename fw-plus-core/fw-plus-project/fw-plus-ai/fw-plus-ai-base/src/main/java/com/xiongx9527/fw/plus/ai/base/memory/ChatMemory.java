package com.xiongx9527.fw.plus.ai.base.memory;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import java.util.List;

/**
 * 对话记忆：按会话 id 维护多轮消息。
 *
 * <p>只描述「记什么、取什么」，不限制存储介质与裁剪策略——窗口大小、系统消息是否常驻、
 * 摘要压缩等由实现决定（见 {@link MessageWindowChatMemory}）。</p>
 */
public interface ChatMemory {

    /** 会话标识。 */
    String id();

    /** 追加一条消息（写入前由实现按策略裁剪）。 */
    void add(ChatMessage message);

    /** 当前上下文消息（按时间正序，可直接作为 {@code LlmRequest.messages}）。 */
    List<ChatMessage> messages();

    /** 清空当前会话的消息。 */
    void clear();
}
