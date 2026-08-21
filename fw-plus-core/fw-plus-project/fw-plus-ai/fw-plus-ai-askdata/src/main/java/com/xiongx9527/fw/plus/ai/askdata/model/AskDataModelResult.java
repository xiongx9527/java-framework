package com.xiongx9527.fw.plus.ai.askdata.model;

/**
 * 模型路由或模型执行失败的不可变结果。
 *
 * <p>Core 使用稳定错误码区分基础设施故障和不可重试拒绝，远程 Java 17 适配服务应将其
 * 自身异常映射为此语义，而不是将具体 SDK 或网络异常泄漏给调用方。</p>
 */
public final class AskDataModelResult {

	private final String modelKey;
	private final AskDataModelRoutePolicy routePolicy;
	private final int candidateIndex;
	private final String routeReason;
	private final String errorCode;
	private final String errorMessage;
	private final boolean retryable;

	private AskDataModelResult(String modelKey, AskDataModelRoutePolicy routePolicy, int candidateIndex,
			String routeReason, String errorCode, String errorMessage, boolean retryable) {
		this.modelKey = modelKey;
		this.routePolicy = routePolicy;
		this.candidateIndex = candidateIndex;
		this.routeReason = routeReason;
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
		this.retryable = retryable;
	}

	public static AskDataModelResult routed(String modelKey, AskDataModelRoutePolicy routePolicy) {
		if (modelKey == null || modelKey.length() == 0 || routePolicy == null) {
			throw new IllegalArgumentException("modelKey 和 routePolicy 不能为空");
		}
		return routed(modelKey, routePolicy, -1, routePolicy.name());
	}

	static AskDataModelResult routed(String modelKey, AskDataModelRoutePolicy routePolicy,
			int candidateIndex, String routeReason) {
		if (modelKey == null || modelKey.length() == 0 || routePolicy == null
				|| routeReason == null || routeReason.length() == 0) {
			throw new IllegalArgumentException("modelKey、routePolicy 和 routeReason 不能为空");
		}
		return new AskDataModelResult(modelKey, routePolicy, candidateIndex, routeReason, null, null, true);
	}

	/**
	 * 创建基础设施失败结果。
	 *
	 * <p>此兼容入口不包含失败候选的路由层级。调用 {@link AskDataModelRouter#fallback(AskDataModelRequest, AskDataModelResult)}
	 * 时只能按首次匹配的模型 key 定位后继候选；配置层可能存在相同 key 时，应使用
	 * {@link #infrastructureFailure(AskDataModelResult, String)} 保留实际命中位置。</p>
	 *
	 * <p>错误码固定为 {@code MODEL_INFRASTRUCTURE_FAILURE}，表示网络、网关、限流等可重试故障。
	 * Java 17 Remote Runtime 适配服务必须在其边界完成异常分类，不能将 SDK 或传输层异常直接暴露给 Core
	 * 或审计调用方。</p>
	 *
	 * @param modelKey 失败模型 key，不能为空
	 * @param message 面向调用方的受控错误信息，不能为空
	 * @return 可参与兼容降级的失败结果
	 */
	public static AskDataModelResult infrastructureFailure(String modelKey, String message) {
		return failure(modelKey, "MODEL_INFRASTRUCTURE_FAILURE", message, true);
	}

	/**
	 * 根据已选路由创建基础设施失败结果。
	 *
	 * <p>结果保留 {@code routedResult} 的 {@link AskDataModelRoutePolicy}，使降级从实际失败候选的
	 * 下一个配置层开始，即使多个层级配置了相同模型 key 也不会重复选择同一失败层级。该层级同时是
	 * Core 路由审计记录的一部分；Java 17 Remote Runtime 仅报告执行结果，不得改写该路由决定。</p>
	 *
	 * @param routedResult 已成功选中的模型路由结果，不能为空
	 * @param message 面向调用方的受控错误信息，不能为空
	 * @return 保留失败位置的可重试基础设施失败结果
	 */
	public static AskDataModelResult infrastructureFailure(AskDataModelResult routedResult, String message) {
		if (routedResult == null || !routedResult.isRouted()) {
			throw new IllegalArgumentException("routedResult 必须是已路由结果");
		}
		return failure(routedResult.getModelKey(), routedResult.getRoutePolicy(), routedResult.getCandidateIndex(),
				routedResult.getRouteReason(),
				"MODEL_INFRASTRUCTURE_FAILURE", message, true);
	}

	/**
	 * 创建不可重试的模型请求失败结果。
	 *
	 * <p>错误码固定为 {@code MODEL_REQUEST_REJECTED}，用于鉴权、参数校验、策略拒绝等不应切换模型的失败。
	 * 适配层应将远程异常先映射为该稳定语义，避免将 Java 17 服务实现、模型 SDK 或敏感错误细节写入
	 * Core 的审计输出。</p>
	 *
	 * @param modelKey 失败模型 key，不能为空
	 * @param message 面向调用方的受控错误信息，不能为空
	 * @return 不可重试的失败结果
	 */
	public static AskDataModelResult nonRetryableFailure(String modelKey, String message) {
		return failure(modelKey, "MODEL_REQUEST_REJECTED", message, false);
	}

	private static AskDataModelResult failure(String modelKey, String errorCode, String message, boolean retryable) {
		return failure(modelKey, null, -1, null, errorCode, message, retryable);
	}

	private static AskDataModelResult failure(String modelKey, AskDataModelRoutePolicy routePolicy,
			int candidateIndex, String routeReason, String errorCode, String message, boolean retryable) {
		if (modelKey == null || modelKey.length() == 0 || message == null || message.length() == 0) {
			throw new IllegalArgumentException("modelKey 和 message 不能为空");
		}
		return new AskDataModelResult(modelKey, routePolicy, candidateIndex, routeReason,
				errorCode, message, retryable);
	}

	public boolean isRouted() {
		return errorCode == null;
	}

	public String getModelKey() {
		return modelKey;
	}

	public AskDataModelRoutePolicy getRoutePolicy() {
		return routePolicy;
	}

	public int getCandidateIndex() { return candidateIndex; }

	public String getRouteReason() { return routeReason; }

	public String getErrorCode() {
		return errorCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public boolean isRetryable() {
		return retryable;
	}
}
