package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic;

import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.state.AgentState;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgenticToolProviderTest {

    @Test
    void shouldExposeToolSpecificationFromConfiguredNameAndDescription() {
        UntypedAgent pipeline = mock(UntypedAgent.class);
        AgenticToolProvider provider = AgenticToolProvider.agentic(pipeline, "query_data", "按自然语言查询业务数据",
                "sql", "question");

        ToolProviderResult result = provider.provideTools(new ToolProviderRequest("m1", UserMessage.from("问数")));

        assertThat(result.aiServiceTools()).hasSize(1);
        ToolSpecification specification = result.aiServiceTools().get(0).toolSpecification();
        assertThat(specification.name()).isEqualTo("query_data");
        assertThat(specification.description()).isEqualTo("按自然语言查询业务数据");
        assertThat(specification.parameters().properties()).containsKey("question");
    }

    @Test
    void shouldReturnOutputKeyValueAsToolResult() {
        UntypedAgent pipeline = mock(UntypedAgent.class);
        when(pipeline.invoke(anyMap())).thenReturn(Map.of("sql", "select 1", "extra", "x"));
        AgenticToolProvider provider = AgenticToolProvider.agentic(pipeline, "query_data", "查询", "sql", "question");

        String output = execute(provider, "{\"question\":\"上月订单总额\"}");

        assertThat(output).isEqualTo("select 1");
    }

    @Test
    void shouldReturnRawOutputWhenOutputKeyAbsent() {
        UntypedAgent pipeline = mock(UntypedAgent.class);
        when(pipeline.invoke(anyMap())).thenReturn("plain-result");
        AgenticToolProvider provider = AgenticToolProvider.agentic(pipeline, "query_data", "查询", null, "question");

        assertThat(execute(provider, "{}")).isEqualTo("plain-result");
    }

    @Test
    void shouldReturnEmptyStringWhenAgentReturnsNull() {
        UntypedAgent pipeline = mock(UntypedAgent.class);
        when(pipeline.invoke(anyMap())).thenReturn(null);
        AgenticToolProvider provider = AgenticToolProvider.agentic(pipeline, "query_data", "查询", "sql", "question");

        assertThat(execute(provider, "{}")).isEmpty();
    }

    @Test
    void shouldWrapCompiledGraphAsTool() throws Exception {
        StateGraph<AgentState> graph = new StateGraph<>(AgentState::new)
                .addNode("echo", org.bsc.langgraph4j.action.AsyncNodeAction.node_async(
                        state -> Map.of("answer", "graph-done")))
                .addEdge(StateGraph.START, "echo")
                .addEdge("echo", StateGraph.END);
        CompiledGraph<AgentState> compiled = graph.compile(org.bsc.langgraph4j.CompileConfig.builder().build());

        ToolProvider provider = AgenticToolProviders.graphToolProvider(compiled, "graph_tool", "图编排工具");

        assertThat(execute(provider, "{\"input\":\"x\"}")).contains("graph-done");
    }

    private String execute(ToolProvider provider, String arguments) {
        ToolProviderResult result = provider.provideTools(new ToolProviderRequest("m1", UserMessage.from("问数")));
        AiServiceTool tool = result.aiServiceTools().get(0);
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name(tool.toolSpecification().name())
                .arguments(arguments)
                .build();
        return tool.toolExecutor().execute(request, "m1");
    }
}
