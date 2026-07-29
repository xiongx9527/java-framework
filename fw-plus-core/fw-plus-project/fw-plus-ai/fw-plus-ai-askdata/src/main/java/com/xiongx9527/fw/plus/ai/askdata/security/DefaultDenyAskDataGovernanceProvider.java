package com.xiongx9527.fw.plus.ai.askdata.security;

/**
 * 未接入业务治理服务时的默认拒绝实现。
 */
public final class DefaultDenyAskDataGovernanceProvider implements AskDataGovernanceProvider {

	public static final String DENIAL_CODE = "ASKDATA_GOVERNANCE_DENIED";
	public static final String AUTHORIZATION_VERSION = "default-deny-v1";
	public static final String AUTHORIZATION_SOURCE = "fw-plus-default";

	@Override
	public AskDataAuthorizationSummary authorize(AskDataAuthorizationRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("request 不能为空");
		}
		return AskDataAuthorizationSummary.deny(request, DENIAL_CODE, AUTHORIZATION_VERSION, AUTHORIZATION_SOURCE);
	}
}
