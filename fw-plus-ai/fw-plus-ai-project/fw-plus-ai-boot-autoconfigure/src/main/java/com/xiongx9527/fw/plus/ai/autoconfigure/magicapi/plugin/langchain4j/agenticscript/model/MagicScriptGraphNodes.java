package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.model;

import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.action.NodeAction;
import org.bsc.langgraph4j.state.AgentState;

import java.util.Map;

/**
 * {@link MagicScriptAgent} → langgraph4j 图节点的适配：脚本节点可作为 StateGraph 里的
 * 一个节点参与图编排（state.data() 注入脚本上下文，脚本返回值写回 state）。
 *
 * <p>用法：</p>
 * <pre>{@code
 * var agent = langchain4jAgenticScript.node("/nl2sql/表匹配");
 * var graph = AgenticWorkflows.graph(AgentState::new)
 *         .addNode("表匹配", MagicScriptGraphNodes.nodeAsync(agent))
 *         .addEdge(StateGraph.START, "表匹配")
 *         .addEdge("表匹配", StateGraph.END);
 * }</pre>
 *
 * <p>脚本返回值为 Map 时按键合并进 state；其他类型写入 outputKey（未配置时写 "output"）。</p>
 */
public final class MagicScriptGraphNodes {

	private MagicScriptGraphNodes() {
	}

	/** 默认结果键：脚本返回值非 Map 时写入该键。 */
	public static final String DEFAULT_OUTPUT_KEY = "output";

	/** 脚本节点 → langgraph4j 节点动作（同步形态，内部异步包装）。 */
	public static <T extends AgentState> AsyncNodeAction<T> nodeAsync(MagicScriptAgent agent) {
		return AsyncNodeAction.node_async(nodeAction(agent));
	}

	/** 脚本节点 → langgraph4j 节点动作（同步形态）。 */
	public static <T extends AgentState> NodeAction<T> nodeAction(MagicScriptAgent agent) {
		return state -> execute(agent, state.data());
	}

	private static Map<String, Object> execute(MagicScriptAgent agent, Map<String, Object> input) {
		Object result = agent.invoke(input == null ? Map.of() : input);
		if (result == null) {
			return Map.of();
		}
		// 脚本返回 Map：按键合并进 state（脚本可直接控制 state 增量）
		if (result instanceof Map<?, ?> resultMap) {
			Map<String, Object> merged = new java.util.LinkedHashMap<>();
			resultMap.forEach((k, v) -> merged.put(String.valueOf(k), v));
			return merged;
		}
		// 非 Map：写入 outputKey（未配置时写默认键）
		String key = agent.getOutputKey() != null && !agent.getOutputKey().trim().isEmpty()
				? agent.getOutputKey().trim() : DEFAULT_OUTPUT_KEY;
		return Map.of(key, result);
	}
}
