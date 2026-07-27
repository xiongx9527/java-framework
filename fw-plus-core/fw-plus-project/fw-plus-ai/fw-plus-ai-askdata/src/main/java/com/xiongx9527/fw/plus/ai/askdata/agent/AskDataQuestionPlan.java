package com.xiongx9527.fw.plus.ai.askdata.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AskData 问题执行计划。
 */
public final class AskDataQuestionPlan {

	private final List<String> questions;

	public AskDataQuestionPlan(List<String> questions) {
		if (questions == null || questions.isEmpty()) {
			throw new IllegalArgumentException("questions 不能为空");
		}
		List<String> normalizedQuestions = new ArrayList<String>(questions.size());
		for (String question : questions) {
			normalizedQuestions.add(requireNonBlank(question, "question"));
		}
		this.questions = Collections.unmodifiableList(normalizedQuestions);
	}

	public static AskDataQuestionPlan single(String question) {
		return new AskDataQuestionPlan(Collections.singletonList(question));
	}

	public List<String> getQuestions() {
		return questions;
	}

	public boolean isMultiQuestion() {
		return questions.size() > 1;
	}

	static String requireNonBlank(String value, String name) {
		if (value == null || value.trim().length() == 0) {
			throw new IllegalArgumentException(name + " 不能为空白");
		}
		return value.trim();
	}
}
