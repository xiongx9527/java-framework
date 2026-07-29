package com.xiongx9527.fw.plus.ai.askdata.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AskData 授权判定结果。
 */
public final class AskDataAuthorizationSummary {

	private final boolean allowed;
	private final List<AskDataAuthorizedObject> authorizedObjects;
	private final List<AskDataRowPredicate> rowPredicates;

	private AskDataAuthorizationSummary(boolean allowed, List<AskDataAuthorizedObject> authorizedObjects,
									List<AskDataRowPredicate> rowPredicates) {
		this.allowed = allowed;
		this.authorizedObjects = immutableCopy(authorizedObjects, "authorizedObjects");
		this.rowPredicates = immutableCopy(rowPredicates, "rowPredicates");
	}

	public static AskDataAuthorizationSummary deny(String reason) {
		if (reason == null || reason.trim().length() == 0) {
			throw new IllegalArgumentException("reason 不能为空白");
		}
		return new AskDataAuthorizationSummary(false, Collections.<AskDataAuthorizedObject>emptyList(),
				Collections.<AskDataRowPredicate>emptyList());
	}

	public static AskDataAuthorizationSummary allow(List<AskDataAuthorizedObject> authorizedObjects,
															 List<AskDataRowPredicate> rowPredicates) {
		return new AskDataAuthorizationSummary(true, authorizedObjects, rowPredicates);
	}

	public boolean isAllowed() {
		return allowed;
	}

	public List<AskDataAuthorizedObject> getAuthorizedObjects() {
		return authorizedObjects;
	}

	public List<AskDataRowPredicate> getRowPredicates() {
		return rowPredicates;
	}

	public boolean containsObject(AskDataAuthorizedObject authorizedObject) {
		return authorizedObject != null && authorizedObjects.contains(authorizedObject);
	}

	private static <T> List<T> immutableCopy(List<T> values, String fieldName) {
		if (values == null) {
			throw new IllegalArgumentException(fieldName + " 不能为空");
		}
		return Collections.unmodifiableList(new ArrayList<T>(values));
	}
}
