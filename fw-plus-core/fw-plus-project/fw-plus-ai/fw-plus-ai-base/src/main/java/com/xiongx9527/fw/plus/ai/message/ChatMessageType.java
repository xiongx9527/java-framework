package com.xiongx9527.fw.plus.ai.message;

/** 聊天消息类型，镜像 langchain4j 同名枚举。 */
public enum ChatMessageType {
    SYSTEM,
    USER,
    AI,
    TOOL_EXECUTION_RESULT,
    CUSTOM
}
