package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic;

import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.service.tool.ToolProvider;

/**
 * {@link AgenticToolProvider} 的工厂入口：脚本与业务侧经本类把 agentic 编排产物 /
 * langgraph4j 编译图包装为 {@link ToolProvider}，再用
 * {@code AssistantBuilder.toolProvider(...)} 挂入助手（agent-as-tool）。
 *
 * <p>用法示例：</p>
 * <pre>{@code
 * UntypedAgent pipeline = AgenticWorkflows.sequence()
 *         .subAgents(extractor, matcher, generator)
 *         .outputName("sql")
 *         .build();
 *
 * assistantBuilder.toolProvider(
 *         AgenticToolProviders.agenticToolProvider(pipeline, "query_data",
 *                 "按自然语言查询业务数据", "sql", "question"));
 * }</pre>
 */
public final class AgenticToolProviders {

    private AgenticToolProviders() {
    }

    /** 把 agentic 编排产物包装为 ToolProvider。 */
    public static ToolProvider agenticToolProvider(UntypedAgent pipeline, String toolName, String toolDescription,
            String outputKey, String... inputKeys) {
        return AgenticToolProvider.agentic(pipeline, toolName, toolDescription, outputKey, inputKeys);
    }

    /** 把 langgraph4j 编译图包装为 ToolProvider。 */
    public static <T extends org.bsc.langgraph4j.state.AgentState> ToolProvider graphToolProvider(
            org.bsc.langgraph4j.CompiledGraph<T> compiledGraph, String toolName, String toolDescription) {
        return AgenticToolProvider.graph(compiledGraph, toolName, toolDescription);
    }

    /**
     * 按注册中心 key 把编排包装为 ToolProvider（隔离层：调用方无需接触 agentic 类型）。
     * 编排未注册时抛出 IllegalArgumentException（错误信息列出已注册 key）。
     */
    public static ToolProvider agenticToolProviderByKey(AgenticPipelineRegistry registry, String agenticKey,
            String toolName, String toolDescription, String outputKey, String... inputKeys) {
        UntypedAgent pipeline = registry.get(agenticKey);
        if (pipeline == null) {
            throw new IllegalArgumentException(String.format(
                    "编排注册中心中不存在 agenticKey '%s'，已注册: %s", agenticKey, registry.keys()));
        }
        return agenticToolProvider(pipeline, toolName, toolDescription, outputKey, inputKeys);
    }
}
