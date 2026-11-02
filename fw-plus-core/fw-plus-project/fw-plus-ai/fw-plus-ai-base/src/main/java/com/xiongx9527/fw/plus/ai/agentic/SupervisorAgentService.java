package com.xiongx9527.fw.plus.ai.agentic;

import com.xiongx9527.fw.plus.ai.model.chat.ChatModel;

/** Supervisor 编排服务，镜像 langchain4j-agentic 的 {@code SupervisorAgentService<T>}。 */
public interface SupervisorAgentService<T> {
    SupervisorAgentService<T> supervisorModel(ChatModel model);
    SupervisorAgentService<T> subAgents(Object... agents);
    SupervisorAgentService<T> outputKey(String outputKey);
    T build();
}
