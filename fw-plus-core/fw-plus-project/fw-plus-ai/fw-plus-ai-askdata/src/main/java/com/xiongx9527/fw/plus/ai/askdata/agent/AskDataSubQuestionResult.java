package com.xiongx9527.fw.plus.ai.askdata.agent;
/** 单个子问题的可聚合结果。 */
public final class AskDataSubQuestionResult { private final String questionId; private final String answer;
	public AskDataSubQuestionResult(String questionId,String answer){if(questionId==null||questionId.trim().length()==0)throw new IllegalArgumentException("questionId 不能为空");this.questionId=questionId.trim();this.answer=answer;} public String getQuestionId(){return questionId;} public String getAnswer(){return answer;} }
