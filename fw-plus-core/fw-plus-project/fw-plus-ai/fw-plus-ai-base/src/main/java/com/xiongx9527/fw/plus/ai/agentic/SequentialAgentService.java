package com.xiongx9527.fw.plus.ai.agentic;

/** 顺序编排服务，镜像 langchain4j-agentic 的 {@code SequentialAgentService<T>}。 */
public interface SequentialAgentService<T> {
    SequentialAgentService<T> subAgents(Object... agents);
    SequentialAgentService<T> outputKey(String outputKey);
    T build();
}
