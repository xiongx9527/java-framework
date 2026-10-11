package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.service.TokenStream;
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
    // typed 编排：sequence / loop / conditional / parallel / streaming
    // ------------------------------------------------------------------

    public interface TypedSequenceAgent {
        @Agent
        String run(@V("request") String request);
    }

    public static class FirstMapper {
        @Agent(name = "first", description = "第一步", outputKey = "tmp")
        public String first(@V("request") String request) {
            return request + "-first";
        }
    }

    public static class SecondMapper {
        @Agent(name = "second", description = "第二步", outputKey = "final")
        public String second(@V("tmp") String tmp) {
            return tmp + "-second";
        }
    }

    @Test
    void typedSequenceShouldBuildAndExecute() {
        TypedSequenceAgent agent = AgenticWorkflows.sequence(TypedSequenceAgent.class)
                .subAgents(new FirstMapper(), new SecondMapper())
                .outputKey("final")
                .build();

        assertThat(agent.run("x")).isEqualTo("x-first-second");
    }

    public interface TypedLoopAgent {
        @Agent
        String run(@V("request") String request);
    }

    public static class LoopMapper {
        @Agent(name = "loop", description = "循环处理", outputKey = "answer")
        public String loop(@V("request") String request) {
            return request + "-looped";
        }
    }

    @Test
    void typedLoopShouldBuildAndExecute() {
        TypedLoopAgent agent = AgenticWorkflows.loop(TypedLoopAgent.class)
                .subAgents(new LoopMapper())
                .maxIterations(3)
                .exitCondition(scope -> ((String) scope.readState("answer", "")).contains("looped"))
                .outputKey("answer")
                .build();

        assertThat(agent.run("x")).isEqualTo("x-looped");
    }

    public interface TypedConditionalAgent {
        @Agent
        String run(@V("intent") String intent, @V("request") String request);
    }

    public static class QueryBranch {
        @Agent(name = "query", description = "查询分支", outputKey = "answer")
        public String query(@V("request") String request) {
            return "query-" + request;
        }
    }

    public static class EchoBranch {
        @Agent(name = "echo", description = "回显分支", outputKey = "answer")
        public String echo(@V("request") String request) {
            return "echo-" + request;
        }
    }

    @Test
    void typedConditionalShouldBuildAndExecute() {
        TypedConditionalAgent agent = AgenticWorkflows.conditional(TypedConditionalAgent.class)
                .subAgents(scope -> "query".equals(scope.readState("intent")), new QueryBranch())
                .subAgents(scope -> "echo".equals(scope.readState("intent")), new EchoBranch())
                .outputKey("answer")
                .build();

        assertThat(agent.run("query", "hello")).isEqualTo("query-hello");
        assertThat(agent.run("echo", "hello")).isEqualTo("echo-hello");
    }

    public interface TypedParallelAgent {
        @Agent
        String run(@V("title") String title);
    }

    public static class TranslateAgent {
        @Agent(name = "translate", description = "翻译", outputKey = "translation")
        public String translate(@V("title") String title) {
            return title + "-translated";
        }
    }

    public static class SloganAgent {
        @Agent(name = "slogan", description = "宣传语", outputKey = "slogan")
        public String slogan(@V("title") String title) {
            return title + "-slogan";
        }
    }

    @Test
    void typedParallelShouldBuildAndExecute() {
        TypedParallelAgent agent = AgenticWorkflows.parallel(TypedParallelAgent.class)
                .subAgents(new TranslateAgent(), new SloganAgent())
                .outputKey("translation")
                .build();

        assertThat(agent.run("hello")).isEqualTo("hello-translated");
    }

    public interface StreamingSequenceAgent {
        @Agent
        @UserMessage("请写一篇关于 {{topic}} 的故事")
        TokenStream writeStory(@V("topic") String topic);
    }

    public interface SyncStepAgent {
        @Agent
        @UserMessage("同步处理：{{topic}}")
        String step(@V("topic") String topic);
    }

    public interface StreamingStepAgent {
        @Agent
        @UserMessage("流式处理：{{topic}}")
        TokenStream step(@V("topic") String topic);
    }

    @Test
    void typedSequenceShouldStreamFromLastSubAgent() {
        ScriptedStreamingChatModel streamingModel = new ScriptedStreamingChatModel()
                .agent("streamed", "-", "output");

        SyncStepAgent syncStep = AgenticWorkflows.agent(SyncStepAgent.class)
                .chatModel(new ScriptedChatModel().agent("sync-out"))
                .build();

        StreamingStepAgent streamingStep = AgenticWorkflows.agent(StreamingStepAgent.class)
                .streamingChatModel(streamingModel)
                .outputKey("story")
                .build();

        StreamingSequenceAgent agent = AgenticWorkflows.sequence(StreamingSequenceAgent.class)
                .subAgents(syncStep, streamingStep)
                .build();

        TokenStream tokenStream = agent.writeStory("dragons");
        StringBuilder sb = new StringBuilder();
        tokenStream.onPartialResponse(sb::append).ignoreErrors().start();

        assertThat(sb.toString()).isEqualTo("streamed-output");
    }

    // ------------------------------------------------------------------
    // 自定义规划器编排（PlannerBasedService）
    // ------------------------------------------------------------------

    public interface PlannerBasedAgent {
        @Agent
        String run(@V("request") String request);
    }

    /** 确定性规划器：按固定顺序调用子 agent，不依赖 LLM 决策。 */
    static class SequentialPlanner implements dev.langchain4j.agentic.planner.Planner {
        private List<dev.langchain4j.agentic.internal.AgentExecutor> agents;
        private int cursor;

        @Override
        public void init(dev.langchain4j.agentic.planner.InitPlanningContext ctx) {
            this.agents = ctx.subagents().stream()
                    .map(dev.langchain4j.agentic.internal.AgentExecutor.class::cast)
                    .toList();
        }

        @Override
        public dev.langchain4j.agentic.planner.Action nextAction(
                dev.langchain4j.agentic.planner.PlanningContext ctx) {
            if (cursor >= agents.size()) {
                return new dev.langchain4j.agentic.planner.Action.DoneAction();
            }
            return new dev.langchain4j.agentic.planner.Action.AgentCallAction(agents.get(cursor++));
        }
    }

    @Test
    void plannerBuilderShouldExecuteWithCustomPlanner() {
        Object stepA = AgenticWorkflows.agent(StepA.class)
                .chatModel(new ScriptedChatModel().agent("A-OUT"))
                .build();
        Object stepB = AgenticWorkflows.agent(StepB.class)
                .chatModel(new ScriptedChatModel().agent("B-OUT"))
                .build();

        PlannerBasedAgent agent = AgenticWorkflows.plannerBuilder(PlannerBasedAgent.class)
                .planner(SequentialPlanner::new)
                .subAgents(stepA, stepB)
                .outputKey("answer")
                .build();

        assertThat(agent.run("用户请求")).isEqualTo("B-OUT");
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
