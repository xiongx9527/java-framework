package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore;

/**
 * ChatMemoryStore 存储层默认值。
 */
final class ChatMemoryStoreDefaults {

    /** 租户/业务类型未解析出时的默认值 */
    static final String DEFAULT_TENANT_ID = ChatMemoryIds.DEFAULT_BIZ_TYPE;

    private ChatMemoryStoreDefaults() {
    }
}
