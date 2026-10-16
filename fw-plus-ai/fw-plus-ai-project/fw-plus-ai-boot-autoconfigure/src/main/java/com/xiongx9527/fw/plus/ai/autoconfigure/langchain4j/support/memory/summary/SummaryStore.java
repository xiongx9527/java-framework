package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary;

/**
 * 一级会话摘要存储：以会话键（5 段 run_id）为作用域，{@code session_key} 唯一。
 * 实现对同一 sessionKey 只维护一行（「覆盖」只发生在这一行）。
 */
public interface SummaryStore {

	/** 按会话键读取摘要记录；无记录返回 {@link SummaryRecord#EMPTY}（非 null） */
	SummaryRecord get(String sessionKey);

	/** upsert 写入摘要记录 */
	void put(String sessionKey, SummaryRecord record);

	/** 删除会话摘要（会话结束时清理；不影响消息存储原文） */
	void remove(String sessionKey);

}