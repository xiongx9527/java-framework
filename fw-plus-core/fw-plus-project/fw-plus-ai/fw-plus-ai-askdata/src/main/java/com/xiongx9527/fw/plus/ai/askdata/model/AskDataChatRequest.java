package com.xiongx9527.fw.plus.ai.askdata.model;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

/** 对话模型的隔离请求。 */
public final class AskDataChatRequest {
	private final AskDataProjectContext context;
	private final String question;
	public AskDataChatRequest(AskDataProjectContext context, String question) {
		if (context == null || question == null || question.trim().length() == 0) throw new IllegalArgumentException("context 和 question 不能为空");
		this.context = context; this.question = question.trim();
	}
	public AskDataProjectContext getContext() { return context; }
	public String getQuestion() { return question; }
}
