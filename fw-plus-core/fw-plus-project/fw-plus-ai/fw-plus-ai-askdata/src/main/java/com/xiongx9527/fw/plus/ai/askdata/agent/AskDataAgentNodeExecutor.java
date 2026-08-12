package com.xiongx9527.fw.plus.ai.askdata.agent;

/**
 * 单一 Agent 图节点执行 SPI。
 *
 * <p>图运行时只负责在依赖满足后调用本接口，真正的节点行为、治理上下文和状态持久化仍由 AskData Flow
 * 控制。实现可能被并发调用，必须自行保证线程安全；不得重复提交同一节点、绕过 SQL 最终校验，或把节点
 * 输出写入未按项目和操作人隔离的共享状态。</p>
 */
public interface AskDataAgentNodeExecutor {

	/**
	 * 执行一个已满足依赖的节点。
	 *
	 * @param node 当前图中的不可变节点定义
	 * @return 供运行时关联的脱敏结果摘要
	 */
	String execute(AskDataAgentNode node);
}
