package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary;

/**
 * 可插拔摘要策略：把给定输入压缩为摘要文本。
 *
 * <p>实现约定：返回非 null（可返回空串表示无摘要）；异常向上抛出，由滚动引擎按「摘要失败」回退处理。</p>
 */
public interface Summarizer {

	/** 策略名（用于日志与诊断） */
	String name();

	/** 生成摘要 */
	String summarize(SummarizeRequest request);

}