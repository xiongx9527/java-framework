package com.xiongx9527.fw.plus.ai.base.memory;

/**
 * 会话记忆提供者：按会话 id 取（或创建）一份 {@link ChatMemory}。
 *
 * <p>同一个 provider 既能返回共享存储上的窗口记忆（多实例），也能返回内存记忆（单机 / 测试）。</p>
 */
public interface ChatMemoryProvider {

    ChatMemory get(String memoryId);
}
