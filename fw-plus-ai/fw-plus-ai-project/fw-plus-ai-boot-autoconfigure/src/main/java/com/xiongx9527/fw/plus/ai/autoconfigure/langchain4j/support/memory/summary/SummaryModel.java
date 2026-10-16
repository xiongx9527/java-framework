package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary;

/**
 * 摘要模型：把一段提示词压缩为摘要文本。生产环境由便宜小模型（或回退主模型）实现；
 * 未配置模型时，各内置策略回退为「抽取式截断」，保证功能可用且可离线测试。
 */
public interface SummaryModel {

	/** 把提示词交给模型并返回摘要文本；模型侧异常按调用方语义向上抛出 */
	String summarize(String prompt);

}