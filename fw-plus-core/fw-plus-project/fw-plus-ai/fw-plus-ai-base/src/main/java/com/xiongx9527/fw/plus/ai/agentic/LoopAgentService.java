package com.xiongx9527.fw.plus.ai.agentic;

/** 循环编排服务，镜像 langchain4j-agentic 的 {@code LoopAgentService<T>}。 */
public interface LoopAgentService<T> {
    LoopAgentService<T> subAgents(Object... agents);
    LoopAgentService<T> maxIterations(int maxIterations);
    LoopAgentService<T> outputKey(String outputKey);
    T build();
}
