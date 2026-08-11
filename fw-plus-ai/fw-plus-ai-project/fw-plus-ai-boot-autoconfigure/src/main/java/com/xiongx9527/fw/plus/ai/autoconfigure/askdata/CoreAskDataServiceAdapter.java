package com.xiongx9527.fw.plus.ai.autoconfigure.askdata;

import com.xiongx9527.fw.plus.ai.askdata.api.AskDataError;

/**
 * Core AskData 服务与旧问数流程之间的适配器。
 *
 * <p>Core 请求在构造时已经校验项目和操作人上下文；本适配器会将该上下文传递给旧流程请求，且不会伪造该边界。
 * 旧流程的真实权限校验、审计和恢复授权仍由后续桥接或业务覆盖实现完成；本默认适配器仅用于兼容既有单项目流程，
 * 不得被用作跨项目资源访问的授权依据。</p>
 *
 * <p>适配结果保留 Core 可公开的答案、稳定错误码以及脱敏后的模型 key/尝试顺序，避免旧流程的 SQL、模型原始响应、
 * 会话令牌以及存储实现细节逸出到 Core 契约。恢复令牌和其他敏感执行状态仍由旧流程在受控内部边界处理。</p>
 */
public class CoreAskDataServiceAdapter implements com.xiongx9527.fw.plus.ai.askdata.api.AskDataService {

	private final AskDataService legacyService;

	/**
	 * 创建 Core 服务适配器。
	 *
	 * @param legacyService 旧问数服务，不能为空
	 */
	public CoreAskDataServiceAdapter(AskDataService legacyService) {
		if (legacyService == null) {
			throw new IllegalArgumentException("legacyService 不能为空");
		}
		this.legacyService = legacyService;
	}

	/**
	 * 执行 Core 请求并将旧流程结果收敛为 Core 结果。
	 *
	 * <p>Core 请求为空时返回受控错误而非调用旧流程；Core 请求中的项目和操作人上下文会传递给旧流程，
	 * 供后续桥接或业务覆盖实现执行权限和审计。默认适配不暴露旧流程异常或执行细节。</p>
	 *
	 * @param request 已校验的 Core 请求
	 * @return 不含框架、模型或存储实现类型的 Core 结果
	 */
	@Override
	public com.xiongx9527.fw.plus.ai.askdata.api.AskDataResult ask(
			com.xiongx9527.fw.plus.ai.askdata.api.AskDataRequest request) {
		if (request == null) {
			return failure(AskDataErrorCode.MODEL_RESPONSE_INVALID, "请求不能为空");
		}

		AskDataRequest legacyRequest = new AskDataRequest();
		legacyRequest.setQuestion(request.getQuestion());
		legacyRequest.setProjectContext(request.getProjectContext());
		AskDataResult legacyResult = legacyService.ask(legacyRequest);
		if (legacyResult == null) {
			return failure(AskDataErrorCode.MODEL_RESPONSE_INVALID, "旧问数流程未返回结果");
		}
		if (!legacyResult.isSuccess()) {
			return failure(defaultValue(legacyResult.getErrorCode(), AskDataErrorCode.MODEL_RESPONSE_INVALID),
					"问数执行失败，请查看监控日志", legacyResult);
		}
		return new com.xiongx9527.fw.plus.ai.askdata.api.AskDataResult(legacyResult.getBrief(), null,
				legacyResult.getActualModelKey(), legacyResult.getModelAttempts(),
				legacyResult.getActualEmbeddingModelKey(), legacyResult.getEmbeddingModelAttempts());
	}

	private com.xiongx9527.fw.plus.ai.askdata.api.AskDataResult failure(String code, String message) {
		return com.xiongx9527.fw.plus.ai.askdata.api.AskDataResult.failure(new AskDataError(code, message));
	}

	private com.xiongx9527.fw.plus.ai.askdata.api.AskDataResult failure(String code, String message,
			AskDataResult legacyResult) {
		return com.xiongx9527.fw.plus.ai.askdata.api.AskDataResult.failure(new AskDataError(code, message),
				legacyResult.getActualModelKey(), legacyResult.getModelAttempts(),
				legacyResult.getActualEmbeddingModelKey(), legacyResult.getEmbeddingModelAttempts());
	}

	private String defaultValue(String value, String defaultValue) {
		return value == null || value.trim().isEmpty() ? defaultValue : value;
	}
}
