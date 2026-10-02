package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.model;

import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.state.AgentState;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 脚本节点 → langgraph4j 图节点适配验证：StateGraph 中嵌入 MagicScriptAgent 节点，
 * state.data() 注入脚本上下文，脚本返回值写回 state。
 */
class MagicScriptGraphNodesTest {

	@Test
	void shouldEmbedScriptAgentAsGraphNode() throws Exception {
		Langchain4jAgenticScriptInfo info = new Langchain4jAgenticScriptInfo();
		info.setType("node");
		info.setPath("/test/echo");
		info.setName("echo-node");
		info.setInputKeys(java.util.List.of("question"));
		info.setOutputKey("answer");
		info.setScript("return \"echo:\" + question;");
		MagicScriptAgent agent = new MagicScriptAgent(info);

		StateGraph<AgentState> graph = new StateGraph<>(AgentState::new)
				.addNode("echo", MagicScriptGraphNodes.nodeAsync(agent))
				.addEdge(StateGraph.START, "echo")
				.addEdge("echo", StateGraph.END);
		CompiledGraph<AgentState> compiled = graph.compile(org.bsc.langgraph4j.CompileConfig.builder().build());

		Map<String, Object> result = compiled.invoke(Map.of("question", "你好")).orElseThrow().data();

		assertThat(result.get("answer")).isEqualTo("echo:你好");
	}

	@Test
	void shouldMergeMapResultIntoState() throws Exception {
		Langchain4jAgenticScriptInfo info = new Langchain4jAgenticScriptInfo();
		info.setType("node");
		info.setPath("/test/map");
		info.setInputKeys(java.util.List.of());
		MagicScriptAgent agent = new MapReturningAgent(info);

		StateGraph<AgentState> graph = new StateGraph<>(AgentState::new)
				.addNode("mapNode", MagicScriptGraphNodes.nodeAsync(agent))
				.addEdge(StateGraph.START, "mapNode")
				.addEdge("mapNode", StateGraph.END);
		CompiledGraph<AgentState> compiled = graph.compile(org.bsc.langgraph4j.CompileConfig.builder().build());

		Map<String, Object> result = compiled.invoke(Map.of("seed", "v")).orElseThrow().data();

		assertThat(result).containsEntry("k1", "v1").containsEntry("k2", "v2");
	}

	/** 返回 Map 的脚本节点（模拟脚本返回 Map 的场景）。 */
	private static class MapReturningAgent extends MagicScriptAgent {

		private MapReturningAgent(Langchain4jAgenticScriptInfo info) {
			super(info);
		}

		@Override
		public Object invoke(Map<String, Object> input) {
			return Map.of("k1", "v1", "k2", "v2");
		}
	}
}
