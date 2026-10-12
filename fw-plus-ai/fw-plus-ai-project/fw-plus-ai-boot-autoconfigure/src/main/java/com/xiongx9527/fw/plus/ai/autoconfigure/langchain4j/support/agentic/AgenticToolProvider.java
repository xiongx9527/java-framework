package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.ToolExecutionResult;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 把 langchain4j-agentic 编排产物（{@link UntypedAgent}）或 langgraph4j 编译图包装为
 * 标准 {@link ToolProvider} 的适配器（agent-as-tool）：主 LLM 依据工具描述自主决定
 * 何时调用编排，编排结果作为工具返回值回传。
 *
 * <p>本类是 langchain4j-agentic / langgraph4j 两个 beta 依赖的隔离层：所有对实验性
 * API 的引用都收口在本类与 {@link AgenticWorkflows} 中。</p>
 */
public final class AgenticToolProvider implements ToolProvider {

    /** 编排调用器：入参为工具参数（state），返回编排产出（Map 时可按 outputKey 提取）。 */
    private final Function<Map<String, Object>, Object> invoker;

    private final String toolName;

    private final String toolDescription;

    private final String outputKey;

    private final String[] inputKeys;

    private AgenticToolProvider(Function<Map<String, Object>, Object> invoker, String toolName,
            String toolDescription, String outputKey, String[] inputKeys) {
        this.invoker = invoker;
        this.toolName = toolName;
        this.toolDescription = toolDescription;
        this.outputKey = outputKey;
        this.inputKeys = inputKeys == null ? new String[0] : inputKeys.clone();
    }

    /** 把 agentic 编排产物包装为单工具的 ToolProvider。 */
    public static AgenticToolProvider agentic(UntypedAgent agent, String toolName, String toolDescription,
            String outputKey, String... inputKeys) {
        return new AgenticToolProvider(agent::invoke, toolName, toolDescription, outputKey, inputKeys);
    }

    /** 把 langgraph4j 编译图包装为单工具的 ToolProvider（工具入参即图的 state）。 */
    public static AgenticToolProvider graph(org.bsc.langgraph4j.CompiledGraph<? extends org.bsc.langgraph4j.state.AgentState> compiledGraph,
            String toolName, String toolDescription) {
        return new AgenticToolProvider(input -> invokeGraph(compiledGraph, input), toolName, toolDescription, null,
                new String[0]);
    }

    /** 执行编译图并抽出最终 state：图终止无输出时返回空 Map。 */
    private static Map<String, Object> invokeGraph(
            org.bsc.langgraph4j.CompiledGraph<? extends org.bsc.langgraph4j.state.AgentState> compiledGraph,
            Map<String, Object> input) {
        // langgraph4j 1.9 起 invoke(Map) 已弃用，统一走 GraphInput + RunnableConfig 入口
        java.util.Optional<? extends org.bsc.langgraph4j.state.AgentState> graphOutput = compiledGraph
                .invoke(org.bsc.langgraph4j.GraphInput.args(input), org.bsc.langgraph4j.RunnableConfig.empty());
        if (graphOutput == null || !graphOutput.isPresent()) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> stateData = graphOutput.get().data();
        if (stateData != null) {
            result.putAll(stateData);
        }
        return result;
    }

    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        ToolSpecification specification = ToolSpecification.builder()
                .name(toolName)
                .description(toolDescription)
                .parameters(buildParametersSchema())
                .build();
        ToolProviderResult.Builder result = ToolProviderResult.builder();
        result.add(specification, this::execute);
        return result.build();
    }

    /** 工具参数 schema：inputKeys 逐个声明为字符串属性；graph 模式为自由对象。 */
    private JsonObjectSchema buildParametersSchema() {
        JsonObjectSchema.Builder schema = JsonObjectSchema.builder();
        for (String key : inputKeys) {
            schema.addStringProperty(key);
        }
        return schema.build();
    }

    private String execute(ToolExecutionRequest request, Object memoryId) {
        Map<String, Object> arguments = parseArguments(request.arguments());
        Object output = invoker.apply(arguments);
        if (output == null) {
            return "";
        }
        if (outputKey != null && output instanceof Map<?, ?> result && result.containsKey(outputKey)) {
            Object value = result.get(outputKey);
            return value == null ? "" : String.valueOf(value);
        }
        return String.valueOf(output);
    }

    /** 解析工具调用入参（JSON 对象）；非对象或解析失败返回空 Map，由编排侧按缺参语义处理。 */
    private static Map<String, Object> parseArguments(String arguments) {
        if (arguments == null || arguments.trim().isEmpty()) {
            return Map.of();
        }
        try {
            Object parsed = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(arguments, Map.class);
            return parsed instanceof Map ? (Map<String, Object>) parsed : Map.of();
        } catch (Exception e) {
            return Map.of();
        }
    }
}
