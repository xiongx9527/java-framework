package com.xiongx9527.fw.plus.ai.nativeimpl.memory;

/** 显式区分不同对话记忆后端的请求协议；厂商名称不能代替协议名称。 */
public enum ConversationMemoryProtocol {
    MEM0,
    VECTOR,
    IN_MEMORY
}
