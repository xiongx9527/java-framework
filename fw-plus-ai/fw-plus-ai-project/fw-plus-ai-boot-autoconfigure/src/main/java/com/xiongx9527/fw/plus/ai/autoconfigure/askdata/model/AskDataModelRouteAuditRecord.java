package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model;

import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelResult;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRoutePolicy;

/**
 * 自动配置层记录的一次模型路由执行结果。
 *
 * <p>记录只保存 Core 已决策的逻辑模型 key、路由层级和稳定错误码，不写入提示词、项目数据、模型原始异常
 * 或厂商响应。这样调用方可以审计降级路径，同时不会将模型厂商实现细节或可能敏感的请求内容泄漏到日志。</p>
 */
public final class AskDataModelRouteAuditRecord {

	private final String modelKey;
	private final String provider;
	private final AskDataModelRoutePolicy routePolicy;
	private final String errorCode;
	private final boolean retryable;

	/**
	 * 从 Core 路由或失败结果创建不可变审计记录。
	 *
	 * @param result Core 路由或已分类失败结果，不能为空
	 */
	public AskDataModelRouteAuditRecord(AskDataModelResult result, String provider) {
		if (result == null) {
			throw new IllegalArgumentException("result 不能为空");
		}
		this.modelKey = result.getModelKey();
		this.provider = provider;
		this.routePolicy = result.getRoutePolicy();
		this.errorCode = result.getErrorCode();
		this.retryable = result.isRetryable();
	}

	public String getModelKey() {
		return modelKey;
	}

	public String getProvider() {
		return provider;
	}

	public AskDataModelRoutePolicy getRoutePolicy() {
		return routePolicy;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public boolean isRetryable() {
		return retryable;
	}
}
