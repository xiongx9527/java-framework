package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore;

/**
 * ChatMemoryStore 存储层默认值。
 */
final class ChatMemoryStoreDefaults {

	/** 租户维度未解析出时的默认值（与 memId 三段构建入口一致） */
	static final String DEFAULT_TENANT_ID = ChatMemoryIds.DEFAULT_TENANT_ID;

	private ChatMemoryStoreDefaults() {
	}
}
