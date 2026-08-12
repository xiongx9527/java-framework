package com.xiongx9527.fw.plus.ai.askdata.agent;

import java.util.List;

/**
 * AskData Java 图编排运行时 SPI。
 *
 * <p>实现只能调度 Core 图节点和节点执行器，不得改变项目权限、检查点或节点幂等边界。业务可以提供 LangGraph4j
 * Adapter；默认实现为当前线程内的确定性顺序/DAG 执行器。</p>
 */
public interface AskDataAgentGraphRuntime {

	/** 返回稳定的运行时能力 key。 */
	String getKey();

	/**
	 * 返回运行时当前是否可调用。
	 *
	 * <p>返回 {@code false} 时，调用方可在尚未执行任何节点的前提下回退到默认顺序运行时。</p>
	 */
	boolean isAvailable();

	/**
	 * 调度图节点。
	 *
	 * <p>运行时必须只使用传入的 {@code executor} 执行节点，并且同一节点最多执行一次。若已经回调执行过任一节点，
	 * 后续失败必须直接向调用方报告，禁止在运行时内部从头重放图节点。</p>
	 *
	 * @param graph 不可变图定义
	 * @param executor 保持权限、幂等、状态和审计边界的节点执行器
	 * @return 按完成顺序返回的脱敏节点结果
	 */
	List<String> execute(AskDataAgentGraph graph, AskDataAgentNodeExecutor executor);
}
