package com.xiongx9527.fw.plus.ai.askdata.model;

/**
 * AI 框架和模型厂商无关的对话模型 SPI。
 *
 * <p>LangChain4j、Spring AI 等 Adapter 负责把自身类型映射为 Core 请求和结果。实现可能在多个请求线程中
 * 复用，必须遵守底层客户端的线程安全要求；不得保存跨项目对话状态、记录凭据或原始敏感 Prompt。只有
 * 可重试基础设施故障可标记为可切换，业务拒绝、权限失败和无效响应不得伪装为可重试。</p>
 */
public interface AskDataChatModel {

	/**
	 * 执行一次类型化聊天调用。
	 *
	 * @param request 不可变的模型请求
	 * @return 含实际模型 key、用量和中立错误语义的结果
	 */
	AskDataChatResult chat(AskDataChatRequest request);
}
