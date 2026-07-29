package com.xiongx9527.fw.plus.ai.askdata.security;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

/**
 * AskData 授权判定请求。
 */
public final class AskDataAuthorizationRequest {

	private final AskDataProjectContext projectContext;

	public AskDataAuthorizationRequest(AskDataProjectContext projectContext) {
		if (projectContext == null) {
			throw new IllegalArgumentException("projectContext 不能为空");
		}
		this.projectContext = projectContext;
	}

	public AskDataProjectContext getProjectContext() {
		return projectContext;
	}
}
