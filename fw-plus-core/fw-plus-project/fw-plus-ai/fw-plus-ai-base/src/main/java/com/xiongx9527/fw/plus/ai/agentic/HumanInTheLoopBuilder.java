package com.xiongx9527.fw.plus.ai.agentic;

/** 人工介入构建器，镜像 langchain4j-agentic 的 {@code HumanInTheLoopBuilder}。 */
public interface HumanInTheLoopBuilder {
    HumanInTheLoopBuilder agent(Object agent);
    HumanInTheLoopBuilder responseStrategy(Object responseStrategy);
    Object build();
}
