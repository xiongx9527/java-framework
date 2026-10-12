package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent;

/**
 * 意图判定来源：标记一条 {@link IntentResult} 由哪一级判定得出，便于观测与排障。
 *
 * <p>级联判定按 {@link #RULE} → {@link #EMBEDDING} → {@link #LLM} 顺序短路，
 * 全部未置信时返回 {@link #FALLBACK}（此时 {@link IntentResult#intent()} 为 null）。</p>
 */
public enum IntentSource {

	/** L0 规则命中（确定性，最高优先级） */
	RULE,

	/** L1 向量相似度判定 */
	EMBEDDING,

	/** L2 大模型结构化输出判定 */
	LLM,

	/** 全层级均未置信，兜底结果（{@link IntentResult#intent()} 为 null） */
	FALLBACK
}
