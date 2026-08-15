package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow;

import com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgentGraph;
import com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgentGraphExecutor;
import com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgentNode;
import com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgentNodeExecutor;
import com.xiongx9527.fw.plus.ai.askdata.capability.AskDataCapabilityType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LangGraph4jAskDataGraphAdapterTest {

	@Test
	void 应通过Core节点执行器调度图且以运行时能力注册() {
		LangGraph4jAskDataGraphAdapter adapter = new LangGraph4jAskDataGraphAdapter("langgraph4j", transport(true));
		AskDataAgentGraph graph = new AskDataAgentGraph("askdata", "1", Arrays.asList(
				new AskDataAgentNode("first", Collections.<String>emptyList()),
				new AskDataAgentNode("second", Collections.singletonList("first"))));

		List<String> results = adapter.execute(graph, new AskDataAgentNodeExecutor() {
			@Override
			public String execute(AskDataAgentNode node) {
				return node.getId() + "-completed";
			}
		});

		assertEquals(Arrays.asList("first-completed", "second-completed"), results);
		assertEquals(AskDataCapabilityType.RUNTIME_ADAPTER, adapter.getDescriptor().getType());
		assertEquals("fw.plus.ai.askdata.flow.langgraph4j",
				adapter.getDescriptor().getConfigurationNamespace());
		assertSame(adapter, adapter.getCapability());
	}

	@Test
	void 运行时不可用时应在执行节点前失败() {
		LangGraph4jAskDataGraphAdapter adapter = new LangGraph4jAskDataGraphAdapter("langgraph4j", transport(false));
		AskDataAgentGraph graph = new AskDataAgentGraph("askdata", "1", Collections.singletonList(
				new AskDataAgentNode("first", Collections.<String>emptyList())));

		IllegalStateException failure = assertThrows(IllegalStateException.class,
				() -> adapter.execute(graph, node -> node.getId()));

		assertEquals("LangGraph4j runtime is unavailable", failure.getMessage());
	}

	private LangGraph4jAskDataTransport transport(final boolean available) {
		return new LangGraph4jAskDataTransport() {
			@Override
			public boolean isAvailable() {
				return available;
			}

			@Override
			public List<String> execute(AskDataAgentGraph graph, AskDataAgentNodeExecutor executor) {
				return new AskDataAgentGraphExecutor().execute(graph, executor);
			}
		};
	}
}
