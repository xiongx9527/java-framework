package com.xiongx9527.fw.plus.ai.askdata.agent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 以稳定子问题 ID 为键的不可变聚合结果。 */
public final class AskDataMultiQuestionResult {

	private final Map<String, AskDataSubQuestionResult> results;

	public AskDataMultiQuestionResult(List<AskDataSubQuestionResult> values) {
		if (values == null) {
			throw new IllegalArgumentException("results 不能为空");
		}
		Map<String, AskDataSubQuestionResult> copy = new LinkedHashMap<String, AskDataSubQuestionResult>();
		for (AskDataSubQuestionResult value : values) {
			if (value == null || copy.put(value.getQuestionId(), value) != null) {
				throw new IllegalArgumentException("子问题结果不能为空且 ID 不可重复");
			}
		}
		this.results = Collections.unmodifiableMap(copy);
	}

	public String getAnswer(String questionId) {
		AskDataSubQuestionResult result = results.get(questionId);
		return result == null ? null : result.getAnswer();
	}

	public AskDataSubQuestionResult getResult(String questionId) {
		return results.get(questionId);
	}

	public Map<String, AskDataSubQuestionResult> getResults() { return results; }
}
