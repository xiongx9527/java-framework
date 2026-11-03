/**
 * 对话记忆：多轮上下文的消息窗口与持久化契约。
 *
 * <p>{@link com.xiongx9527.fw.plus.ai.base.memory.ChatMemory} 面向「一次会话」，
 * {@link com.xiongx9527.fw.plus.ai.base.memory.ChatMemoryStore} 面向「消息怎么存」，
 * 两者解耦：同一套窗口策略既能配内存存储（单机 / 测试），也能配数据库或 Redis（多实例）。</p>
 */
package com.xiongx9527.fw.plus.ai.base.memory;
