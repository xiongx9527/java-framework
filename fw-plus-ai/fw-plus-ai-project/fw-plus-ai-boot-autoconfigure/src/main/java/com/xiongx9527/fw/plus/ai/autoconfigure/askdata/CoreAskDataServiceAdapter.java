package com.xiongx9527.fw.plus.ai.autoconfigure.askdata;

import com.xiongx9527.fw.plus.ai.askdata.api.AskDataError;

/**
 * Core AskData 服务与旧问数流程之间的适配器。
 *
 * <p>Core 请求在构造时已经校验项目和操作人上下文；本适配器不会丢弃或伪造该边界。旧流程当前只接受问题文本，
 * 因此项目隔离、权限校验、审计和恢复授权必须由业务优先注册的 Core 服务实现，或由后续承载项目上下文的旧流程
 * 扩展完成。本默认适配器仅用于兼容既有单项目流程，不得被用作跨项目资源访问的授权依据。</p>
 *
 * <p>适配结果仅保留 Core 可公开的答案或稳定错误码，避免旧流程的 SQL、模型响应、会话令牌以及存储实现细节
 * 逸出到 Core 契约。恢复令牌和其他敏感执行状态仍由旧流程在受控内部边界处理。</p>
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
	 * <p>Core 请求为空时返回受控错误而非调用旧流程；Core 请求中的项目和操作人上下文由请求对象保证存在，
	 * 业务覆盖实现可据此执行权限和审计。默认适配不暴露旧流程异常或执行细节。</p>
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
		AskDataResult legacyResult = legacyService.ask(legacyRequest);
		if (legacyResult == null) {
			return failure(AskDataErrorCode.MODEL_RESPONSE_INVALID, "旧问数流程未返回结果");
		}
		if (!legacyResult.isSuccess()) {
			return failure(defaultValue(legacyResult.getErrorCode(), AskDataErrorCode.MODEL_RESPONSE_INVALID),
					defaultValue(legacyResult.getErrorMessage(), "旧问数流程执行失败"));
		}
		return com.xiongx9527.fw.plus.ai.askdata.api.AskDataResult.success(legacyResult.getBrief());
	}

	private com.xiongx9527.fw.plus.ai.askdata.api.AskDataResult failure(String code, String message) {
		return com.xiongx9527.fw.plus.ai.askdata.api.AskDataResult.failure(new AskDataError(code, message));
	}

	private String defaultValue(String value, String defaultValue) {
		return value == null || value.trim().isEmpty() ? defaultValue : value;
	}
}
