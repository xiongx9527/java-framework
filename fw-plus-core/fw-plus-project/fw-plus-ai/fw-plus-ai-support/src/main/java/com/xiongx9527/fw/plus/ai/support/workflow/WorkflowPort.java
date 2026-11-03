package com.xiongx9527.fw.plus.ai.support.workflow;

/**
 * 编排端口：给定目标，由实现决定怎么拆步骤、怎么循环、怎么收敛。
 */
public interface WorkflowPort {

    WorkflowResult run(WorkflowRequest request);
}
