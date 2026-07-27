package com.xiongx9527.fw.plus.ai.askdata.api;

/**
 * AskData 的核心查询服务契约。
 *
 * <p>调用方必须通过 {@link AskDataRequest} 同时传入项目与操作人上下文，具体实现据此隔离项目资源、审计操作人行为。
 * Core 模块只约束输入输出，不绑定 Spring、模型 SDK 或数据访问技术；二次开发可在适配层实现本接口，但不得绕过请求上下文。</p>
 */
public interface AskDataService {

	/**
	 * 根据已校验的项目上下文和自然语言问题执行数据问答。
	 *
	 * <p>请求对象构造时已拒绝 {@code null} 或空白问题、项目标识及操作人标识。实现方应始终返回结果对象，
	 * 通过 {@link AskDataResult#getError()} 表达可预期的业务失败，而不是向调用方暴露具体框架异常。</p>
	 *
	 * @param request 含项目隔离和操作人隔离信息的请求，不能为空
	 * @return 查询结果或业务错误
	 */
	AskDataResult ask(AskDataRequest request);
}
