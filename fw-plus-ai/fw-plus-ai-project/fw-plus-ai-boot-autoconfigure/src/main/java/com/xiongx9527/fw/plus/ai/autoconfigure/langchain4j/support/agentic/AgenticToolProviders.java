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
    public static ToolProvider graphToolProvider(
            org.bsc.langgraph4j.CompiledGraph<? extends org.bsc.langgraph4j.state.AgentState> compiledGraph,
            String toolName, String toolDescription) {
        return AgenticToolProvider.graph(compiledGraph, toolName, toolDescription);
    }
}
