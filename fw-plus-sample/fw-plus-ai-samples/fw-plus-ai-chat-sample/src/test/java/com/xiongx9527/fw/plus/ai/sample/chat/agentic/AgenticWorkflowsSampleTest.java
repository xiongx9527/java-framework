package com.xiongx9527.fw.plus.ai.sample.chat.agentic;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.AgenticPipelineRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.AgenticToolProviders;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 样例工程内 agentic 门面的无模型回归：parallelMapper / humanInTheLoop / 注册中心 / agent-as-tool。
 *
 * <p>只覆盖「不需要大模型」的模式（与 fw-plus-ai-boot-autoconfigure 中 AgenticPatternsTest
 * 的断言形态一致），保证样例测试可在无 Ollama 的离线环境执行；依赖大模型的
 * supervisor / planner / 声明式系统见 main 代码与端点（需要本机 Ollama）。</p>
 */
class AgenticWorkflowsSampleTest {

	/** parallelMapper 端到端：{@code [a,b,c] → [A,B,C]}（非 AI 映射 agent，无模型）。 */
	@Test
	void parallelMapperShouldMapEachItemAndWriteBackList() {
		UntypedAgent mapper = AgenticWorkflowsDemo.parallelMapperDemo();

		Object result = mapper.invoke(Map.of("items", List.of("a", "b", "c")));

		assertEquals(List.of("A", "B", "C"), result);
	}

	/** humanInTheLoop 作为 sequence 的 subAgent 挂入（框架自动拆包），返回人工响应。 */
	@Test
	void humanInTheLoopShouldProvideResponseAndBeMountable() {
		UntypedAgent pipeline = AgenticWorkflowsDemo.hitlMountedDemo();

		Object result = pipeline.invoke(Map.of("request", "是否需要继续？"));

		assertEquals("人工响应", result);
	}

	/** 注册中心：register → keys / get / contains 全链路。 */
	@Test
	void pipelineRegistryShouldRegisterAndResolveByKey() {
		AgenticPipelineRegistry registry = new AgenticPipelineRegistry();
		UntypedAgent pipeline = AgenticWorkflowsDemo.parallelMapperDemo();

		registry.register("item-map", pipeline);

		assertTrue(registry.contains("item-map"));
		assertTrue(registry.keys().contains("item-map"));
		assertNotNull(registry.get("item-map"));
		assertEquals(1, registry.keys().size());
	}

	/** agent-as-tool：按注册中心 key 包装为 ToolProvider（未注册时抛 IllegalArgumentException）。 */
	@Test
	void toolProviderByKeyShouldWrapRegisteredPipeline() {
		AgenticPipelineRegistry registry = new AgenticPipelineRegistry();
		registry.register("item-map", AgenticWorkflowsDemo.parallelMapperDemo());

		ToolProvider provider = AgenticToolProviders.agenticToolProviderByKey(
				registry, "item-map", "map_items",
				"对给定列表逐项并行映射，返回映射后的列表", "results", "items");

		assertNotNull(provider);
		ToolProviderResult result = provider.provideTools(new ToolProviderRequest("m1", UserMessage.from("映射测试")));
		assertNotNull(result.aiServiceTools());
		assertEquals(1, result.aiServiceTools().size());
	}

	/** conditional 离线形态（非 AI 分支 agent，无模型）：按 mode 路由到大小写映射。 */
	@Test
	void conditionalOfflineShouldRouteToSelectedBranch() {
		assertEquals("ABC", AgenticWorkflowsDemo.runConditionalOfflineDemo("upper", "abc"));
		assertEquals("abc", AgenticWorkflowsDemo.runConditionalOfflineDemo("lower", "ABC"));
	}

	/** graph（langgraph4j StateGraph）端到端：upper → shout，节点均为纯函数。 */
	@Test
	void graphShouldExecuteNodesInOrder() {
		assertEquals("HELLO!", AgenticWorkflowsDemo.runGraphDemo());
	}

	/** observability：给非 AI 编排挂 AgentListener，监听到一次 agent 调用。 */
	@Test
	void observabilityShouldCountAgentInvocations() {
		String result = AgenticWorkflowsDemo.runObservabilityDemo();

		assertEquals("OBSERVE / agent 调用次数=1", result);
	}

	/** agent-as-tool 端到端：并行映射编排经 ToolProvider 执行，返回映射结果。 */
	@Test
	void toolProviderShouldExecuteWrappedPipeline() {
		String output = AgenticWorkflowsDemo.runToolProviderExecutionDemo();

		assertTrue(output.contains("X"), output);
		assertTrue(output.contains("Y"), output);
	}
}