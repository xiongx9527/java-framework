package com.xiongx9527.fw.plus.ai.askdata.agent;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AskDataAgentGraphContractTest {

	@Test
	void 多问题请求必须保留子问题标识依赖聚合与取消超时语义() {
		AskDataMultiQuestionRequest request = new AskDataMultiQuestionRequest(new AskDataProjectContext("project", "operator"),
				"request", Arrays.asList(new AskDataSubQuestion("q1", "first", Collections.<String>emptyList()),
						new AskDataSubQuestion("q2", "second", Collections.singletonList("q1"))), 1000L);

		assertEquals("q1", request.getQuestions().get(0).getId());
		assertEquals(Collections.singletonList("q1"), request.getQuestions().get(1).getDependsOn());
		request.cancel();
		assertTrue(request.isCancelled());
		assertEquals("second-answer", new AskDataMultiQuestionResult(Arrays.asList(
				new AskDataSubQuestionResult("q1", "first-answer"),
				new AskDataSubQuestionResult("q2", "second-answer"))).getAnswer("q2"));
	}

	@Test
	void 图执行器在无图依赖时必须按声明顺序回退执行() {
		AskDataAgentGraph graph = new AskDataAgentGraph("graph", "v1", Arrays.asList(
				new AskDataAgentNode("first", Collections.<String>emptyList()),
				new AskDataAgentNode("second", Collections.<String>emptyList())));
		List<String> executed = new AskDataAgentGraphExecutor().execute(graph, new AskDataAgentNodeExecutor() {
			@Override public String execute(AskDataAgentNode node) { return node.getId() + "-result"; }
		});

		assertEquals(Arrays.asList("first-result", "second-result"), executed);
		assertEquals(AskDataAgentGraphState.COMPLETED, AskDataAgentGraphStateMapper.fromResults(executed).getState());
	}
}
