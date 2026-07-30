package com.xiongx9527.fw.plus.ai.askdata.model;

/** 已脱敏的模型结果。 */
public final class AskDataChatResult {
	private final String answer;
	private AskDataChatResult(String answer) { this.answer = answer; }
	public static AskDataChatResult success(String answer) { return new AskDataChatResult(answer); }
	public String getAnswer() { return answer; }
}
