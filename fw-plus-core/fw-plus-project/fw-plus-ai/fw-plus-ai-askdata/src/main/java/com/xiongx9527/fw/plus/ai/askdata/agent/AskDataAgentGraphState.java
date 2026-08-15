package com.xiongx9527.fw.plus.ai.askdata.agent;

/**
 * Agent 图的稳定执行状态。
 *
 * <p>状态用于 Core、外部图运行时、检查点和监控之间传递终态，不映射任何具体框架枚举。外部运行时不得把
 * 超时或取消伪装为完成；集群恢复只可从已持久化的稳定状态继续，不能依据节点本地瞬时状态推断。</p>
 */
public enum AskDataAgentGraphState {
	PENDING,
	RUNNING,
	COMPLETED,
	FAILED,
	CANCELLED,
	TIMED_OUT
}
