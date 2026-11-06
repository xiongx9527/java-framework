package com.xiongx9527.fw.plus.ai.base.memory;

import java.util.List;

/**
 * 对话记忆端口监听器：与 {@code LlmListener} 一样，让日志 / 计费 / 追踪只实现一次。
 *
 * <p>每个回调都带 {@code scope}（用户 / 智能体 / 运行标识）便于按租户归集；失败走
 * {@link #onError(String, ConversationMemoryScope)}，不抛异常（监听器不应阻断主流程）。</p>
 */
public interface ConversationMemoryListener {

    /** 写入记忆后触发；{@code records} 为本端口返回的本次实际变更。 */
    default void onAdd(ConversationMemoryScope scope, List<MemoryRecord> records) {
    }

    /** 检索记忆后触发；{@code records} 为命中的记忆（含分数）。 */
    default void onSearch(ConversationMemoryScope scope, List<MemoryRecord> records) {
    }

    /** 删除（单条或整作用域）后触发。 */
    default void onDelete(ConversationMemoryScope scope, String memoryId) {
    }

    /** 端口调用失败；{@code message} 为可读原因，不抛异常。 */
    default void onError(String message, ConversationMemoryScope scope) {
    }
}
