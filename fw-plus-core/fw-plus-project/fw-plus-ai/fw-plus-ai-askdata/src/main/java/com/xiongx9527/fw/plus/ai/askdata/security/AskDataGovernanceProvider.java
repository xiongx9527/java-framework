package com.xiongx9527.fw.plus.ai.askdata.security;

/**
 * AskData 数据治理授权 SPI。
 */
public interface AskDataGovernanceProvider {

	/**
	 * 根据项目和操作人上下文判定数据访问范围。
	 *
	 * @param request 授权请求
	 * @return 非空授权结果
	 */
	AskDataAuthorizationSummary authorize(AskDataAuthorizationRequest request);
}
