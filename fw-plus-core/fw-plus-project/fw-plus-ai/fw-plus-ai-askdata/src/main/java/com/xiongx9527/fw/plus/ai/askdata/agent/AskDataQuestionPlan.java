package com.xiongx9527.fw.plus.ai.askdata.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AskData 问题执行计划。
 */
public final class AskDataQuestionPlan {

	private final List<String> questions;
	private final List<AskDataSubQuestion> subQuestions;

	public AskDataQuestionPlan(List<String> questions) {
		if (questions == null || questions.isEmpty()) {
			throw new IllegalArgumentException("questions 不能为空");
		}
		List<String> normalizedQuestions = new ArrayList<String>(questions.size());
		List<AskDataSubQuestion> plannedQuestions = new ArrayList<AskDataSubQuestion>(questions.size());
		for (int index = 0; index < questions.size(); index++) {
			String normalized = requireNonBlank(questions.get(index), "question");
			normalizedQuestions.add(normalized);
			plannedQuestions.add(new AskDataSubQuestion("question-" + (index + 1), normalized,
					Collections.<String>emptyList()));
		}
		this.questions = Collections.unmodifiableList(normalizedQuestions);
		this.subQuestions = Collections.unmodifiableList(plannedQuestions);
	}

	private AskDataQuestionPlan(List<AskDataSubQuestion> subQuestions, boolean structured) {
		if (subQuestions == null || subQuestions.isEmpty()) {
			throw new IllegalArgumentException("subQuestions 不能为空");
		}
		List<AskDataSubQuestion> copied = new ArrayList<AskDataSubQuestion>(subQuestions.size());
		List<String> questionTexts = new ArrayList<String>(subQuestions.size());
		for (AskDataSubQuestion subQuestion : subQuestions) {
			if (subQuestion == null) {
				throw new IllegalArgumentException("subQuestions 不能包含 null");
			}
			copied.add(subQuestion);
			questionTexts.add(subQuestion.getQuestion());
		}
		this.subQuestions = Collections.unmodifiableList(copied);
		this.questions = Collections.unmodifiableList(questionTexts);
	}

	public static AskDataQuestionPlan single(String question) {
		return new AskDataQuestionPlan(Collections.singletonList(question));
	}

	public static AskDataQuestionPlan ofSubQuestions(List<AskDataSubQuestion> subQuestions) {
		return new AskDataQuestionPlan(subQuestions, true);
	}

	public List<String> getQuestions() {
		return questions;
	}

	public List<AskDataSubQuestion> getSubQuestions() {
		return subQuestions;
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
