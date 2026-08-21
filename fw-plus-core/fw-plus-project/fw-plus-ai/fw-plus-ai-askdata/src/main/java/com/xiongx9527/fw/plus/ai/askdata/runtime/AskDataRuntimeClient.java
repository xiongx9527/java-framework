package com.xiongx9527.fw.plus.ai.askdata.runtime;

/**
 * Java 17 独立运行时适配服务的 Core SPI。
 *
 * <p>Core 只定义请求、结果、错误映射和审计上下文，不引入 HTTP client、Spring AI 或
 * AgentScope。适配服务负责传输、认证和将其基础设施异常映射为可重试或不可重试结果。</p>
 */
public interface AskDataRuntimeClient {

	/**
	 * 将请求交给远程运行时执行。
	 *
	 * @param request 包含项目、操作人、会话、请求和审计关联标识的请求，不能为空
	 * @return 无框架类型的运行时结果
	 */
	AskDataRemoteRuntimeResult execute(AskDataRemoteRuntimeRequest request);
}
