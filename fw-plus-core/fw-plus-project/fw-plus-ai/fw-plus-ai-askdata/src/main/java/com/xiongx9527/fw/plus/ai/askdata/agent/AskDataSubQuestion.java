package com.xiongx9527.fw.plus.ai.askdata.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 具有稳定标识、前置依赖、失败策略和超时的子问题。 */
public final class AskDataSubQuestion {

	public enum FailurePolicy {
		SKIP,
		CONTINUE
	}

	private final String id;
	private final String question;
	private final List<String> dependsOn;
	private final FailurePolicy failurePolicy;
	private final long timeoutMillis;

	public AskDataSubQuestion(String id, String question, List<String> dependsOn) {
		this(id, question, dependsOn, FailurePolicy.SKIP, 0L);
	}

	public AskDataSubQuestion(String id, String question, List<String> dependsOn, FailurePolicy failurePolicy,
			long timeoutMillis) {
		if (blank(id) || blank(question) || timeoutMillis < 0L) {
			throw new IllegalArgumentException("id、question 不能为空且 timeoutMillis 不能为负数");
		}
		this.id = id.trim();
		this.question = question.trim();
		this.dependsOn = immutableDependencies(this.id, dependsOn);
		this.failurePolicy = failurePolicy == null ? FailurePolicy.SKIP : failurePolicy;
		this.timeoutMillis = timeoutMillis;
	}

	public String getId() { return id; }
	public String getQuestion() { return question; }
	public List<String> getDependsOn() { return dependsOn; }
	public FailurePolicy getFailurePolicy() { return failurePolicy; }
	public long getTimeoutMillis() { return timeoutMillis; }

	private static List<String> immutableDependencies(String id, List<String> dependencies) {
		if (dependencies == null || dependencies.isEmpty()) {
			return Collections.emptyList();
		}
		Set<String> normalized = new LinkedHashSet<String>();
		for (String dependency : dependencies) {
			if (blank(dependency)) {
				throw new IllegalArgumentException("dependsOn 不能包含空白 ID");
			}
			String dependencyId = dependency.trim();
			if (id.equals(dependencyId)) {
				throw new IllegalArgumentException("子问题不能依赖自身: " + id);
			}
			if (!normalized.add(dependencyId)) {
				throw new IllegalArgumentException("dependsOn 不能包含重复 ID: " + dependencyId);
			}
		}
		return Collections.unmodifiableList(new ArrayList<String>(normalized));
	}

	private static boolean blank(String value) {
		return value == null || value.trim().length() == 0;
	}
}
