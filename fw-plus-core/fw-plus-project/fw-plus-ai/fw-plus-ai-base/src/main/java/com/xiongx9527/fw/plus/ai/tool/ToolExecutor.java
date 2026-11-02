package com.xiongx9527.fw.plus.ai.tool;

/** 工具执行器，镜像 langchain4j 1.23.0 的 {@code dev.langchain4j.service.tool.ToolExecutor}。 */
public interface ToolExecutor {
    String execute(ToolExecutionRequest request, Object memoryId);
}
