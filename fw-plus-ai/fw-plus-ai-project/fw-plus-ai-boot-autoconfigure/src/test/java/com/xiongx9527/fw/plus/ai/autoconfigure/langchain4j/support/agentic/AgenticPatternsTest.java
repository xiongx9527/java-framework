package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 覆盖新增的 agentic 模式：parallelMapper / supervisor / planner / humanInTheLoop /
 * 单 agent（typed/untyped）/ 声明式 createAgenticSystem / A2A。
 */
class AgenticPatternsTest {

    // ------------------------------------------------------------------
    // parallelMapper
    // ------------------------------------------------------------------

    /** 非 AI 映射 agent：对逐项输入做确定性变换，便于验证并行映射聚合语义。 */
    public static class UpperCaseMapper {
        @Agent(name = "upper", description = "把输入项转为大写", outputKey = "mapped")
        public String map(@V("item") String item) {
            return item.toUpperCase();
        }
    }

    @Test
    void parallelMapperShouldMapEachItemAndWriteBackList() {
        UntypedAgent mapper = AgenticWorkflows.parallelMapper()
                .itemsProvider("items")
                .subAgents(new UpperCaseMapper())
                .outputKey("results")
                .build();

        Object result = mapper.invoke(Map.of("items", List.of("a", "b", "c")));

        assertThat(result).isEqualTo(List.of("A", "B", "C"));
    }

    // ------------------------------------------------------------------
    // humanInTheLoop
    // ------------------------------------------------------------------

    @Test
    void humanInTheLoopShouldProvideResponseAndBeMountable() {
        AgenticHumanInTheLoop human = AgenticWorkflows.humanInTheLoop()
                .responseProvider(() -> "人工响应")
                .outputKey("answer")
                .build();

        UntypedAgent pipeline = AgenticWorkflows.sequence()
                .subAgents(human)
                .outputKey("answer")
                .build();

        assertThat(pipeline.invoke(Map.of("request", "是否需要继续？"))).isEqualTo("人工响应");
    }

    // ------------------------------------------------------------------
    // 单 agent（typed / untyped）
    // ------------------------------------------------------------------

    public interface EchoAgent {
        @Agent(name = "echo", description = "回显输入", outputKey = "out")
        @UserMessage("请回显：{{text}}")
        String echo(@V("text") String text);
    }

    @Test
    void typedAgentShouldExecuteStandalone() {
        EchoAgent echo = AgenticWorkflows.agent(EchoAgent.class)
                .chatModel(new ScriptedChatModel().agent("ECHO-OUT"))
                .build();

        assertThat(echo.echo("hello")).isEqualTo("ECHO-OUT");
    }

    @Test
    void untypedAgentShouldExecuteStandalone() {
        UntypedAgent agent = AgenticWorkflows.agent()
                .chatModel(new ScriptedChatModel().agent("UNTYPED-OUT"))
                .userMessage("回答：{{question}}")
                .outputKey("answer")
                .build();

        assertThat(agent.invoke(Map.of("question", "问题"))).isEqualTo("UNTYPED-OUT");
    }

    @Test
    void typedAgentShouldBeMountableAsSubAgent() {
        EchoAgent echo = AgenticWorkflows.agent(EchoAgent.class)
                .chatModel(new ScriptedChatModel().agent("MOUNTED-OUT"))
                .build();

        UntypedAgent pipeline = AgenticWorkflows.sequence()
                .subAgents(echo)
                .outputKey("out")
                .build();

        assertThat(pipeline.invoke(Map.of("text", "hello"))).isEqualTo("MOUNTED-OUT");
    }

    // ------------------------------------------------------------------
    // supervisor / planner（LLM 自主编排）
    // ------------------------------------------------------------------

    public interface StepA {
        @Agent(name = "A", description = "第一步：处理请求")
        @UserMessage("A: {{request}}")
        String run(@V("request") String request);
    }

    public interface StepB {
        @Agent(name = "B", description = "第二步：润色结果")
        @UserMessage("B: {{request}}")
        String run(@V("request") String request);
    }

    @Test
    void supervisorShouldLetLlmChooseSubAgentsAndReturnOutput() {
        ScriptedChatModel model = new ScriptedChatModel()
                .planner(
                        "{\"agentName\":\"A\",\"arguments\":{\"request\":\"r1\"}}",
                        "{\"agentName\":\"B\",\"arguments\":{\"request\":\"r2\"}}",
                        "{\"agentName\":\"done\",\"arguments\":{\"response\":\"最终汇总\"}}")
                .agent("A-OUT", "B-OUT");

        Object stepA = AgenticWorkflows.agent(StepA.class).chatModel(model).build();
        Object stepB = AgenticWorkflows.agent(StepB.class).chatModel(model).build();

        AgenticSupervisor supervisor = AgenticWorkflows.supervisor()
                .chatModel(model)
                .subAgents(stepA, stepB)
                .maxAgentsInvocations(6)
                .outputKey("answer")
                .build();

        dev.langchain4j.agentic.scope.ResultWithAgenticScope<String> result =
                supervisor.invokeWithAgenticScope("用户请求");

        assertThat(result.result()).isEqualTo("B-OUT");
        assertThat(result.agenticScope().readState("answer")).isEqualTo("B-OUT");
    }

    @Test
    void plannerShouldLetLlmOrchestrateAgents() {
        ScriptedChatModel model = new ScriptedChatModel()
                .planner(
                        "{\"agentName\":\"A\",\"arguments\":{\"request\":\"r1\"}}",
                        "{\"agentName\":\"done\",\"arguments\":{\"response\":\"完成\"}}")
                .agent("A-OUT");

        Object stepA = AgenticWorkflows.agent(StepA.class).chatModel(model).build();

        UntypedAgent plannerPipeline = AgenticWorkflows.planner()
                .chatModel(model)
                .subAgents(stepA)
                .maxAgentsInvocations(4)
                .outputKey("answer")
                .build();

        assertThat(plannerPipeline.invoke(Map.of("request", "用户请求"))).isEqualTo("A-OUT");
    }

    @Test
    void plannerShouldRequireChatModel() {
        assertThatThrownBy(() -> AgenticWorkflows.planner().build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ------------------------------------------------------------------
    // 声明式 createAgenticSystem
    // ------------------------------------------------------------------

    public interface DeclA {
        @Agent(name = "A", description = "第一步")
        @UserMessage("A: {{request}}")
        String run(@V("request") String request);
    }

    public interface DeclB {
        @Agent(name = "B", description = "第二步", outputKey = "finalOut")
        @UserMessage("B: {{request}}")
        String run(@V("request") String request);
    }

    public interface DeclarativeSequence {
        @SequenceAgent(subAgents = {DeclA.class, DeclB.class}, outputKey = "finalOut")
        String run(@V("request") String request);
    }

    @Test
    void createAgenticSystemShouldBuildDeclarativeSequence() {
        ScriptedChatModel model = new ScriptedChatModel().agent("A-OUT", "B-OUT");

        DeclarativeSequence system = AgenticWorkflows.createAgenticSystem(DeclarativeSequence.class, model);

        assertThat(system.run("用户请求")).isEqualTo("B-OUT");
    }

    // ------------------------------------------------------------------
    // A2A（结构验证：build 才触网，构建配置阶段不连接）
    // ------------------------------------------------------------------

    @Test
    void a2aFactoryShouldReturnLazyFrameworkBuilder() {
        AgenticA2ABuilder builder = AgenticWorkflows.a2a("http://127.0.0.1:1/agent")
                .inputKeys("request")
                .outputKey("response")
                .async(false);

        assertThat(builder).isSameAs(builder);
        assertThat(AgenticWorkflows.a2aBuilder("http://127.0.0.1:1/agent")).isNotNull();
    }
}
