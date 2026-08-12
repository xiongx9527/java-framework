package com.xiongx9527.fw.plus.ai.askdata.agent;

/** 执行一个已满足依赖的子问题。 */
public interface AskDataSubQuestionExecutor {

	AskDataSubQuestionResult execute(AskDataSubQuestion question) throws Exception;
}
