package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.facts;

import dev.langchain4j.data.message.ChatMessage;

import java.util.List;

/**
 * 事实抽取策略：从本轮真实对话中抽取稳定事实，并**自含写路径**（决定写什么、怎么写）。
 *
 * <p>两种写形态由各策略自行选择：{@link FactStore#addDialogue}（真实原文交由服务端抽取，
 * 如 {@code mem0}）或 {@link FactStore#addFacts}（本地抽取后逐条直存，如 {@code llm} /
 * {@code rule} / {@code hybrid}）。</p>
 *
 * <p>失败语义：可预期的输入问题（模型输出不可解析、无可抽取内容）应静默或告警跳过；
 * 存储/网络异常允许向上抛出，由调用方统一隔离（写路径 {@code FactMaintenance}）。</p>
 */
public interface FactExtractor {

	/**
	 * 抽取本轮对话中的事实并写入事实通道。
	 *
	 * @param userId       memId 段[3]（显式用户段）；为空时实现应静默跳过（作用域守卫）
	 * @param turnMessages 本轮真实对话消息（用户消息 + AI 最终回复，按时间序）
	 */
	void extract(String userId, List<ChatMessage> turnMessages);

}
