package com.xiongx9527.fw.plus.ai.agentic;

/** 条件编排服务，镜像 langchain4j-agentic 的 {@code ConditionalAgentService<T>}。 */
public interface ConditionalAgentService<T> {
    ConditionalAgentService<T> subAgents(Object... agents);
    ConditionalAgentService<T> outputKey(String outputKey);
    T build();
}
