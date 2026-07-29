package com.xiongx9527.fw.plus.ai.askdata.security;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

/**
 * AskData 授权判定请求。
 */
public final class AskDataAuthorizationRequest {

	private final AskDataProjectContext projectContext;
	private final String datasourceKey;
	private final String purpose;
	private final AskDataAdministrativeScope administrativeScope;

	public AskDataAuthorizationRequest(AskDataProjectContext projectContext) {
		this(projectContext, null, AskDataAuthorizationPurpose.QUERY, AskDataAdministrativeScope.none(), true);
	}

	public AskDataAuthorizationRequest(AskDataProjectContext projectContext, String datasourceKey, String purpose,
			AskDataAdministrativeScope administrativeScope) {
		this(projectContext, datasourceKey, purpose, administrativeScope, false);
	}

	private AskDataAuthorizationRequest(AskDataProjectContext projectContext, String datasourceKey, String purpose,
			AskDataAdministrativeScope administrativeScope, boolean legacy) {
		if (projectContext == null) {
			throw new IllegalArgumentException("projectContext 不能为空");
		}
		this.projectContext = projectContext;
		this.datasourceKey = legacy ? datasourceKey : requireNonBlank(datasourceKey, "datasourceKey");
		this.purpose = requireNonBlank(purpose, "purpose");
		if (administrativeScope == null) {
			throw new IllegalArgumentException("administrativeScope 不能为空");
		}
		this.administrativeScope = administrativeScope;
	}

	public AskDataProjectContext getProjectContext() {
		return projectContext;
	}

	public String getDatasourceKey() {
		return datasourceKey;
	}

	public String getPurpose() {
		return purpose;
	}

	public AskDataAdministrativeScope getAdministrativeScope() {
		return administrativeScope;
	}

	private static String requireNonBlank(String value, String fieldName) {
		if (value == null || value.trim().length() == 0) {
			throw new IllegalArgumentException(fieldName + " 不能为空白");
		}
		return value.trim();
	}
}
