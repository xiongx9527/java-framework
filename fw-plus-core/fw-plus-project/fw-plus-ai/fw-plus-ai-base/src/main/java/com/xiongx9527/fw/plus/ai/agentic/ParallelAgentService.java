package com.xiongx9527.fw.plus.ai.agentic;

/** 并行编排服务，镜像 langchain4j-agentic 的 {@code ParallelAgentService<T>} / {@code ParallelMapperService<T>}。 */
public interface ParallelAgentService<T> {
    ParallelAgentService<T> subAgents(Object... agents);
    ParallelAgentService<T> outputKey(String outputKey);
    T build();
}
