package com.xiongx9527.fw.plus.ai.agentic;

import com.xiongx9527.fw.plus.ai.model.chat.ChatModel;

/** 单 agent 构建器，镜像 langchain4j-agentic 的 {@code AgentBuilder}。 */
public interface AgentBuilder<T> {
    AgentBuilder<T> chatModel(ChatModel model);
    AgentBuilder<T> tools(Object... tools);
    AgentBuilder<T> outputKey(String outputKey);
    T build();
}
