package com.xiongx9527.fw.plus.ai.askdata.agent;

import java.util.ArrayList;
import java.util.List;

/**
 * 将用户问题转换为可顺序执行的问题计划。
 */
public interface AskDataQuestionPlanner {

	AskDataQuestionPlan plan(String question);

	AskDataQuestionPlan plan(List<String> questions);

	static AskDataQuestionPlanner simple() {
		return new Simple();
	}

	/**
	 * 仅处理显式列表及中文、英文分号分隔的复合问题。
	 */
	final class Simple implements AskDataQuestionPlanner {

		@Override
		public AskDataQuestionPlan plan(String question) {
			String normalizedQuestion = AskDataQuestionPlan.requireNonBlank(question, "question");
			String[] parts = normalizedQuestion.split("[；;]");
			List<String> questions = new ArrayList<String>(parts.length);
			for (String part : parts) {
				if (part.trim().length() > 0) {
					questions.add(part);
				}
			}
			return new AskDataQuestionPlan(questions);
		}

		@Override
		public AskDataQuestionPlan plan(List<String> questions) {
			return new AskDataQuestionPlan(questions);
		}
	}
}
