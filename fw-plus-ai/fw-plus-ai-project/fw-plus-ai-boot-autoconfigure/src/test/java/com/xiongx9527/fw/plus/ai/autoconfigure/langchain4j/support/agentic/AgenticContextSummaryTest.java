package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import dev.langchain4j.agentic.supervisor.SupervisorContextStrategy;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * agentic 原生摘要 / 上下文注入的隔离层封装：
 * <ul>
 *   <li>{@code context(...)}：从共享 {@code AgenticScope} 读取上下文包装进子 agent 的 prompt；</li>
 *   <li>{@code summarizedContext(...)}：把指定名称 agent 的对话先摘要再注入；</li>
 *   <li>supervisor 的 {@code SUMMARIZATION} 上下文策略端到端。</li>
 * </ul>
 *
 * <p>注入只在 agent 作为「agentic 系统」一员运行时生效（框架以共享 scope 重建 agent 并加装
 * chatRequestTransformer），独立执行的单 agent 不生效。</p>
 */
class AgenticContextSummaryTest {

    /** supervisor 用例的 typed 子 agent。 */
    public interface SupervisorStep {
        @Agent(name = "A", description = "第一步：处理请求")
        @UserMessage("A: {{request}}")
        String run(@V("request") String request);
    }

    @Test
    void contextProviderShouldInjectScopeContextIntoSubAgentPrompt() {
        ScriptedChatModel model = new ScriptedChatModel().agent("A-OUT", "B-OUT");

        UntypedAgent stepA = AgenticBuilder.agent()
                .chatModel(model)
                .name("A")
                .userMessage("A: {{request}}")
                .inputKey(String.class, "request")
                .outputKey("first")
                .build();

        UntypedAgent stepB = AgenticBuilder.agent()
                .chatModel(model)
                .name("B")
                .userMessage("B: {{request}}")
                .inputKey(String.class, "request")
                .outputKey("second")
                .context(scope -> "共享上下文=" + scope.readState("first"))
                .build();

        AgenticWorkflows.sequence()
                .subAgents(stepA, stepB)
                .outputKey("second")
                .build()
                .invoke(Map.of("request", "用户请求"));

        assertThat(model.requests())
                .as("B 的请求应被注入来自共享 scope 的上下文")
                .anySatisfy(request -> {
                    assertThat(request).contains("B: 用户请求");
                    assertThat(request).contains("共享上下文=A-OUT");
                });
    }

    @Test
    void facadeAgentBuilderShouldSupportContextInjection() {
        ScriptedChatModel model = new ScriptedChatModel().agent("A-OUT", "B-OUT");

        UntypedAgent stepA = AgenticWorkflows.agentBuilder()
                .chatModel(model)
                .name("A")
                .userMessage("A: {{request}}")
                .inputKey(String.class, "request")
                .outputKey("first")
                .build();

        UntypedAgent stepB = AgenticWorkflows.agentBuilder()
                .chatModel(model)
                .name("B")
                .userMessage("B: {{request}}")
                .inputKey(String.class, "request")
                .outputKey("second")
                .context(scope -> "门面上下文=" + scope.readState("first"))
                .build();

        AgenticWorkflows.sequence()
                .subAgents(stepA, stepB)
                .outputKey("second")
                .build()
                .invoke(Map.of("request", "用户请求"));

        assertThat(model.requests())
                .as("门面主体（AgenticWorkflows.agentBuilder()）的 context 应生效")
                .anySatisfy(request -> {
                    assertThat(request).contains("B: 用户请求");
                    assertThat(request).contains("门面上下文=A-OUT");
                });
    }

    @Test
    void summarizedContextShouldInjectSummaryOfNamedAgent() {
        ScriptedChatModel model = new ScriptedChatModel()
                .agent("A-OUT", "B-OUT")
                .summarizer("{\"summary\":\"摘要-OK\"}");

        UntypedAgent stepA = AgenticBuilder.agent()
                .chatModel(model)
                .name("A")
                .userMessage("A: {{request}}")
                .inputKey(String.class, "request")
                .outputKey("first")
                .build();

        UntypedAgent stepB = AgenticBuilder.agent()
                .chatModel(model)
                .name("B")
                .userMessage("B: {{request}}")
                .inputKey(String.class, "request")
                .outputKey("second")
                .summarizedContext("A")
                .build();

        AgenticWorkflows.sequence()
                .subAgents(stepA, stepB)
                .outputKey("second")
                .build()
                .invoke(Map.of("request", "用户请求"));

        assertThat(model.requests())
                .as("summary 策略应先调用模型摘要 A 的对话")
                .anySatisfy(request -> assertThat(request).contains("Create a short summary"));
        assertThat(model.requests())
                .as("B 的请求应被注入 A 的对话摘要")
                .anySatisfy(request -> {
                    assertThat(request).contains("B: 用户请求");
                    assertThat(request).contains("摘要-OK");
                });
    }

    @Test
    void supervisorSummarizationStrategyShouldSummarizeContextIntoPlannerTurn() {
        ScriptedChatModel model = new ScriptedChatModel()
                .planner(
                        "{\"agentName\":\"A\",\"arguments\":{\"request\":\"r1\"}}",
                        "{\"agentName\":\"done\",\"arguments\":{\"response\":\"汇总\"}}")
                .agent("A-OUT")
                .summarizer("{\"summary\":\"摘要-OK\"}");

        SupervisorStep stepA = AgenticWorkflows.agent(SupervisorStep.class)
                .chatModel(model)
                .build();

        AgenticSupervisor supervisor = AgenticWorkflows.supervisor()
                .chatModel(model)
                .subAgents(stepA)
                .contextGenerationStrategy(SupervisorContextStrategy.SUMMARIZATION)
                .maxAgentsInvocations(4)
                .outputKey("answer")
                .build();

        ResultWithAgenticScope<String> result = supervisor.invokeWithAgenticScope("用户请求");

        assertThat(result.result()).isEqualTo("A-OUT");
        assertThat(model.requests())
                .as("SUMMARIZATION 策略应调用模型摘要子 agent 的对话")
                .anySatisfy(request -> assertThat(request).contains("Create a short summary"));
        assertThat(model.requests())
                .as("后续 planner 轮次应被注入摘要上下文")
                .anySatisfy(request -> {
                    assertThat(request).contains("planner expert");
                    assertThat(request).contains("摘要-OK");
                });
    }
}
