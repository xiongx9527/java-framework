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
	private final String projectId;
	private final String operatorId;
	private final String datasourceKey;
	private final String purpose;
	private final String denialCode;
	private final String authorizationVersion;
	private final String authorizationSource;
	private final AskDataAdministrativeScope administrativeScope;

	private AskDataAuthorizationSummary(boolean allowed, List<AskDataAuthorizedObject> authorizedObjects,
			List<AskDataRowPredicate> rowPredicates, String projectId, String operatorId, String datasourceKey,
			String purpose, String denialCode, String authorizationVersion, String authorizationSource,
			AskDataAdministrativeScope administrativeScope) {
		this.allowed = allowed;
		this.authorizedObjects = immutableCopy(authorizedObjects, "authorizedObjects");
		this.rowPredicates = immutableCopy(rowPredicates, "rowPredicates");
		validatePredicates(this.authorizedObjects, this.rowPredicates);
		this.projectId = projectId;
		this.operatorId = operatorId;
		this.datasourceKey = datasourceKey;
		this.purpose = purpose;
		this.denialCode = denialCode;
		this.authorizationVersion = requireNonBlank(authorizationVersion, "authorizationVersion");
		this.authorizationSource = requireNonBlank(authorizationSource, "authorizationSource");
		this.administrativeScope = administrativeScope == null ? AskDataAdministrativeScope.none()
				: administrativeScope;
	}

	/**
	 * 兼容旧调用方的未绑定拒绝摘要。受保护执行路径必须拒绝未绑定摘要。
	 */
	public static AskDataAuthorizationSummary deny(String reason) {
		return new AskDataAuthorizationSummary(false, Collections.<AskDataAuthorizedObject>emptyList(),
				Collections.<AskDataRowPredicate>emptyList(), null, null, null, null,
				requireNonBlank(reason, "reason"), "legacy", "legacy", AskDataAdministrativeScope.none());
	}

	/**
	 * 兼容旧调用方的未绑定允许摘要。受保护执行路径必须拒绝未绑定摘要。
	 */
	public static AskDataAuthorizationSummary allow(List<AskDataAuthorizedObject> authorizedObjects,
			List<AskDataRowPredicate> rowPredicates) {
		return new AskDataAuthorizationSummary(true, authorizedObjects, rowPredicates, null, null, null, null,
				null, "legacy", "legacy", AskDataAdministrativeScope.none());
	}

	public static AskDataAuthorizationSummary allow(AskDataAuthorizationRequest request,
			List<AskDataAuthorizedObject> authorizedObjects, List<AskDataRowPredicate> rowPredicates,
			String authorizationVersion, String authorizationSource, AskDataAdministrativeScope administrativeScope) {
		AskDataAuthorizationRequest checkedRequest = requireRequest(request);
		if (!checkedRequest.getAdministrativeScope().equals(administrativeScope)) {
			throw new IllegalArgumentException("administrativeScope 必须与授权请求绑定");
		}
		return new AskDataAuthorizationSummary(true, authorizedObjects, rowPredicates,
				checkedRequest.getProjectContext().getProjectId(), checkedRequest.getProjectContext().getOperatorId(),
				checkedRequest.getDatasourceKey(), checkedRequest.getPurpose(), null, authorizationVersion,
				authorizationSource, administrativeScope);
	}

	public static AskDataAuthorizationSummary deny(AskDataAuthorizationRequest request, String denialCode,
			String authorizationVersion, String authorizationSource) {
		AskDataAuthorizationRequest checkedRequest = requireRequest(request);
		return new AskDataAuthorizationSummary(false, Collections.<AskDataAuthorizedObject>emptyList(),
				Collections.<AskDataRowPredicate>emptyList(), checkedRequest.getProjectContext().getProjectId(),
				checkedRequest.getProjectContext().getOperatorId(), checkedRequest.getDatasourceKey(),
				checkedRequest.getPurpose(), requireNonBlank(denialCode, "denialCode"), authorizationVersion,
				authorizationSource, checkedRequest.getAdministrativeScope());
	}

	public boolean isAllowed() { return allowed; }
	public List<AskDataAuthorizedObject> getAuthorizedObjects() { return authorizedObjects; }
	public List<AskDataRowPredicate> getRowPredicates() { return rowPredicates; }
	public String getProjectId() { return projectId; }
	public String getOperatorId() { return operatorId; }
	public String getDatasourceKey() { return datasourceKey; }
	public String getPurpose() { return purpose; }
	public String getDenialCode() { return denialCode; }
	public String getAuthorizationVersion() { return authorizationVersion; }
	public String getAuthorizationSource() { return authorizationSource; }
	public AskDataAdministrativeScope getAdministrativeScope() { return administrativeScope; }

	public boolean containsObject(AskDataAuthorizedObject authorizedObject) {
		return authorizedObject != null && authorizedObjects.contains(authorizedObject);
	}

	public boolean isBoundTo(AskDataAuthorizationRequest request) {
		return request != null && equals(projectId, request.getProjectContext().getProjectId())
				&& equals(operatorId, request.getProjectContext().getOperatorId())
				&& equals(datasourceKey, request.getDatasourceKey()) && equals(purpose, request.getPurpose())
				&& administrativeScope.equals(request.getAdministrativeScope());
	}

	public boolean hasSameBinding(AskDataAuthorizationSummary other) {
		return other != null && equals(projectId, other.projectId) && equals(operatorId, other.operatorId)
				&& equals(datasourceKey, other.datasourceKey) && equals(purpose, other.purpose)
				&& equals(authorizationVersion, other.authorizationVersion)
				&& equals(authorizationSource, other.authorizationSource)
				&& administrativeScope.equals(other.administrativeScope);
	}

	private static <T> List<T> immutableCopy(List<T> values, String fieldName) {
		if (values == null) {
			throw new IllegalArgumentException(fieldName + " 不能为空");
		}
		return Collections.unmodifiableList(new ArrayList<T>(values));
	}

	private static void validatePredicates(List<AskDataAuthorizedObject> objects,
			List<AskDataRowPredicate> predicates) {
		for (AskDataRowPredicate predicate : predicates) {
			if (predicate == null || !objects.contains(predicate.getAuthorizedObject())) {
				throw new IllegalArgumentException("rowPredicates 必须作用于 authorizedObjects");
			}
		}
	}

	private static AskDataAuthorizationRequest requireRequest(AskDataAuthorizationRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("request 不能为空");
		}
		return request;
	}

	private static String requireNonBlank(String value, String fieldName) {
		if (value == null || value.trim().length() == 0) {
			throw new IllegalArgumentException(fieldName + " 不能为空白");
		}
		return value.trim();
	}

	private static boolean equals(Object first, Object second) {
		return first == null ? second == null : first.equals(second);
	}
}
