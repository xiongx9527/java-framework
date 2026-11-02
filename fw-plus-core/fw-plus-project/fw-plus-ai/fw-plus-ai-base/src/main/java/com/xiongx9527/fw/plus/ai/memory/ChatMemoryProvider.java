package com.xiongx9527.fw.plus.ai.memory;

/** 按会话 id 提供记忆，镜像 langchain4j 1.23.0 的 {@code dev.langchain4j.memory.chat.ChatMemoryProvider}。 */
public interface ChatMemoryProvider {
    ChatMemory get(Object memoryId);
}
