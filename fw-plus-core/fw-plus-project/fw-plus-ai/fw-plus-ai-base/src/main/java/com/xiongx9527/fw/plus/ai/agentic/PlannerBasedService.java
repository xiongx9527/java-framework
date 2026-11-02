package com.xiongx9527.fw.plus.ai.agentic;

import com.xiongx9527.fw.plus.ai.model.chat.ChatModel;

/** Planner 编排服务，镜像 langchain4j-agentic 的 {@code PlannerBasedService<T>}。 */
public interface PlannerBasedService<T> {
    PlannerBasedService<T> plannerModel(ChatModel model);
    PlannerBasedService<T> subAgents(Object... agents);
    PlannerBasedService<T> outputKey(String outputKey);
    T build();
}
