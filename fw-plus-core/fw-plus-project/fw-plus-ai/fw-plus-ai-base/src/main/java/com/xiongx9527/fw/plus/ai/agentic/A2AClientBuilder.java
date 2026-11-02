package com.xiongx9527.fw.plus.ai.agentic;

/** A2A 跨进程客户端构建器，镜像 langchain4j-agentic 的 {@code A2AClientBuilder<T>}。 */
public interface A2AClientBuilder<T> {
    T build();
}
