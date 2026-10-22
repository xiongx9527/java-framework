package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.facts;

import dev.langchain4j.data.message.ChatMessage;

import java.util.List;

/**
 * 事实存储抽象（跨会话事实通道）。
 *
 * <p>与消息通道（{@code Mem0ChatMemoryStore}）物理隔离：以用户维度（memId 段[3]）划分作用域，
 * 写入/读取均<b>不带</b>会话 {@code run_id}；正文为真实对话文本，禁止复用消息通道的
 * {@code {"seq","msg"}} 包装串。</p>
 *
 * <p>失败语义：实现内部不吞异常，统一由上层隔离（写路径 {@code FactMaintenance}、
 * 读路径 {@code FactContextProvider}）。</p>
 */
public interface FactStore {

	/**
	 * 抽取式写入：提交真实对话消息，由记忆服务（{@code infer=true}）服务端抽取并落库。
	 *
	 * @param turnMessages 本轮真实对话消息（用户消息 + AI 最终回复，按时间序）
	 * @param userId       memId 段[3]（显式用户段），不允许为空
	 */
	void addDialogue(List<ChatMessage> turnMessages, String userId);

	/**
	 * 直存写入：本地已抽取事实逐条落库（{@code infer=false}，不做服务端抽取）。
	 *
	 * @param facts  事实文本列表（空白条目跳过）
	 * @param userId memId 段[3]（显式用户段），不允许为空
	 */
	void addFacts(List<String> facts, String userId);

	/**
	 * 语义检索：按查询返回该用户的相关事实文本（服务端排序）。
	 *
	 * @param query  检索查询串
	 * @param userId memId 段[3]（显式用户段），不允许为空
	 * @param topK   返回上限
	 */
	List<String> search(String query, String userId, int topK);

	/**
	 * 列表：返回该用户最近 {@code limit} 条事实文本（供本地关键词检索使用）。
	 *
	 * @param userId memId 段[3]（显式用户段），不允许为空
	 * @param limit  返回上限
	 */
	List<String> listAll(String userId, int limit);
}
