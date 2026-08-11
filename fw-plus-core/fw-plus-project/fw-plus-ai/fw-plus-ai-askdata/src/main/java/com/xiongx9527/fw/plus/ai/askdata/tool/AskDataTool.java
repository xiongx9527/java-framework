package com.xiongx9527.fw.plus.ai.askdata.tool;

/**
 * AI 框架无关的工具执行 SPI。
 *
 * <p>工具 key 必须稳定且唯一，执行前由 Flow 完成项目、操作人和能力授权。实现可能并发执行，必须自行处理
 * 线程安全、超时和幂等；不得从参数中接受替代身份、绕过 SQL 安全边界，或把凭据和原始敏感结果写入日志。
 * 具有副作用的工具必须由业务显式声明并提供补偿策略，不能参与自动模型兜底重放。</p>
 */
public interface AskDataTool {

	String getKey();

	AskDataToolResult execute(AskDataToolRequest request);
}
