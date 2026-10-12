package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.AgenticPipelineRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.AgenticWorkflows;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.V;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.state.AgentState;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AssistantBuilder} 接入 agentic 编排（agent-as-tool）端到端：
 * {@code agentic(UntypedAgent...)} / {@code agenticByKey(registry, key...)} / {@code agenticGraph(...)}
 * 三条挂载路径——mock 模型驱动两轮工具循环，断言编排结果作为工具返回值回传给主 LLM。
 */
class AssistantBuilderAgenticTest {

	/** 纯 {@code @Agent} 映射（不依赖 LLM）：返回固定 SQL */
	public static class SqlMapper {
		@Agent(name = "sqlGen", description = "生成 SQL", outputKey = "sql")
		public String gen(@V("question") String question) {
			return "select 1";
		}
	}

	private static UntypedAgent sqlPipeline() {
		return AgenticWorkflows.sequence()
				.subAgents(new SqlMapper())
				.outputKey("sql")
				.build();
	}

	/** 第一轮要求调 query_data（带 question），第二轮收尾 */
	private ChatModel modelCallingQueryData() {
		ToolExecutionRequest request = ToolExecutionRequest.builder().id("t1").name("query_data")
				.arguments("{\"question\":\"上月订单总额\"}")
				.build();

		ChatModel model = mock(ChatModel.class);
		when(model.chat(any(ChatRequest.class))).thenReturn(
				ChatResponse.builder().aiMessage(AiMessage.from(request)).build(),
				ChatResponse.builder().aiMessage(AiMessage.from("已查询")).build());
		return model;
	}

	private static List<Content> contents(String text) {
		return Collections.singletonList(TextContent.from(text));
	}

	/** 第二轮发给模型的消息中的工具结果文本（即编排回传的内容） */
	private List<String> secondRoundToolResultTexts(ChatModel model) {
		ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
		verify(model, times(2)).chat(captor.capture());

		List<ChatMessage> secondRound = captor.getAllValues().get(1).messages();
		List<String> texts = new ArrayList<String>();
		for (ChatMessage message : secondRound) {
			if (message instanceof ToolExecutionResultMessage toolResult) {
				texts.add(toolResult.text());
			}
		}
		return texts;
	}

	@Test
	void agenticShouldRegisterPipelineAsToolAndReturnOutputKey() {
		ChatModel model = modelCallingQueryData();
		Assistant assistant = new AssistantBuilder().chatModel(model)
				.agentic(sqlPipeline(), "query_data", "按自然语言查询业务数据", "sql", "question")
				.toAssistant();

		String answer = assistant.chat("default", "", contents("上月订单总额"));

		assertThat(answer).isEqualTo("已查询");
		assertThat(secondRoundToolResultTexts(model)).containsExactly("select 1");
	}

	@Test
	void agenticByKeyShouldResolvePipelineFromRegistry() {
		AgenticPipelineRegistry registry = new AgenticPipelineRegistry();
		registry.register("nl2sql", sqlPipeline());
		ChatModel model = modelCallingQueryData();

		Assistant assistant = new AssistantBuilder().chatModel(model)
				.agenticByKey(registry, "nl2sql", "query_data", "按自然语言查询业务数据", "sql", "question")
				.toAssistant();

		assertThat(assistant.chat("default", "", contents("上月订单总额"))).isEqualTo("已查询");
		assertThat(secondRoundToolResultTexts(model)).containsExactly("select 1");
	}

	@Test
	void agenticByKeyShouldRejectUnknownKey() {
		AgenticPipelineRegistry registry = new AgenticPipelineRegistry();

		assertThatThrownBy(() -> new AssistantBuilder()
				.agenticByKey(registry, "missing", "query_data", "查询", "sql", "question"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("missing");
	}

	@Test
	void agenticGraphShouldWrapCompiledGraphAsTool() throws Exception {
		StateGraph<AgentState> graph = new StateGraph<>(AgentState::new)
				.addNode("echo", org.bsc.langgraph4j.action.AsyncNodeAction.node_async(
						state -> Map.of("answer", "graph-done")))
				.addEdge(StateGraph.START, "echo")
				.addEdge("echo", StateGraph.END);
		CompiledGraph<AgentState> compiled = graph.compile(org.bsc.langgraph4j.CompileConfig.builder().build());

		ToolExecutionRequest request = ToolExecutionRequest.builder().id("t1").name("graph_tool")
				.arguments("{\"input\":\"x\"}")
				.build();
		ChatModel model = mock(ChatModel.class);
		when(model.chat(any(ChatRequest.class))).thenReturn(
				ChatResponse.builder().aiMessage(AiMessage.from(request)).build(),
				ChatResponse.builder().aiMessage(AiMessage.from("图完成")).build());

		Assistant assistant = new AssistantBuilder().chatModel(model)
				.agenticGraph(compiled, "graph_tool", "图编排工具")
				.toAssistant();

		assertThat(assistant.chat("default", "", contents("走图"))).isEqualTo("图完成");
		assertThat(secondRoundToolResultTexts(model)).hasSize(1);
		assertThat(secondRoundToolResultTexts(model).get(0)).contains("graph-done");
	}

	@Test
	void agenticShouldRejectNullPipeline() {
		assertThatThrownBy(() -> new AssistantBuilder()
				.agentic(null, "query_data", "查询", "sql", "question"))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
