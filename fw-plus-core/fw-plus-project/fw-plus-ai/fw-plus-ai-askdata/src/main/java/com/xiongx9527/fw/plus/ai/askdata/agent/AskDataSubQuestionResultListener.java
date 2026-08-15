package com.xiongx9527.fw.plus.ai.askdata.agent;

/** 接收多问题执行期间新产生的独立终态。 */
public interface AskDataSubQuestionResultListener {

	void onTerminal(AskDataSubQuestionResult result);
}
