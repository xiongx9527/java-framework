package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * agentic 侧会话记忆上下文（与助手侧同一套实现）：
 * <ul>
 *   <li>{@code contextProvider}：摘要 / 事实等来源注入模型请求——独立执行的单 agent 也生效；</li>
 *   <li>{@code contextMaintenance}：记忆写入（AI 回复落库）后触发维护；</li>
 *   <li>memoryId（{@code @MemoryId} / 默认值）驱动会话键与 userId（段[3]）解析；</li>
 *   <li>与 agentic 原生 scope 上下文（{@code context(...)}）组合成链、互不覆盖；</li>
 *   <li>typed / untyped 两条门面路径透传。</li>
 * </ul>
 *
 * <p>与 {@link AgenticContextSummaryTest} 互补：那个文件覆盖原生 scope 上下文，本文件覆盖
 * 基于会话记忆的 fw 上下文（跨会话）。</p>
 */
class AgenticContextMemoryTest {

    /** typed 会话用例：memoryId 由 {@code @MemoryId} 参数显式传入（5 段契约，段[3] 为用户）。 */
    public interface SessionAgent {
        @Agent(name = "session", description = "会话记忆用例", outputKey = "out")
        @UserMessage("{{text}}")
        String chat(@V("text") String text, @MemoryId String memoryId);
    }

    @Test
    void contextProviderShouldInjectAndMaintainOnStandaloneUntypedAgent() {
        ScriptedChatModel model = new ScriptedChatModel().agent("OUT");
        AgenticBuilderStorageTest.RecordingChatMemoryStore store =
                new AgenticBuilderStorageTest.RecordingChatMemoryStore();
        List<String> provided = Collections.synchronizedList(new ArrayList<>());
        List<String> maintained = Collections.synchronizedList(new ArrayList<>());

        UntypedAgent agent = AgenticBuilder.agent()
                .chatModel(model)
                .userMessage("{{q}}")
                .inputKey(String.class, "q")
                .memory(10)
                .memoryStore(store)
                .contextProvider(request -> {
                    provided.add(request.getLatestUserMessage());
                    return "会话记忆上下文=OK";
                })
                .contextMaintenance((request, messages) -> maintained.add(request.getLatestUserMessage()))
                .build();

        agent.invoke(Map.of("q", "你好"));

        assertThat(model.requests())
                .as("模型请求应包含注入的前置系统消息（独立执行的单 agent 也生效）")
                .anySatisfy(request -> assertThat(request).contains("会话记忆上下文=OK"));
        assertThat(provided)
                .as("来源应收到本轮用户原话（RAG 检索段剥离后）")
                .contains("你好");
        assertThat(maintained)
                .as("AI 回复落库后应触发上下文维护")
                .isNotEmpty();
    }

    @Test
    void memoryIdShouldDriveSessionKeyAndUserIdParsing() {
        ScriptedChatModel model = new ScriptedChatModel().agent("OUT", "OUT");
        AgenticBuilderStorageTest.RecordingChatMemoryStore store =
                new AgenticBuilderStorageTest.RecordingChatMemoryStore();
        List<String> seen = Collections.synchronizedList(new ArrayList<>());

        SessionAgent agent = AgenticBuilder.agent(SessionAgent.class)
                .chatModel(model)
                .memory(10)
                .memoryStore(store)
                .contextProvider(request -> {
                    seen.add(request.getSessionKey() + "|" + request.getUserId());
                    return "ctx";
                })
                .build();

        agent.chat("第一轮", "0:app:agent:user7:session1");
        agent.chat("第二轮", "0:app:agent:user7:session2");

        assertThat(seen)
                .as("会话键取 memoryId 原文，userId 取段[3]（跨会话维度）")
                .contains("0:app:agent:user7:session1|user7", "0:app:agent:user7:session2|user7");
        assertThat(store.messages)
                .as("两个会话各自落库（会话隔离）")
                .containsKeys("0:app:agent:user7:session1", "0:app:agent:user7:session2");
    }

    @Test
    void sessionContextShouldCoexistWithNativeScopeContext() {
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
                .contextProvider(request -> "会话记忆上下文=链式OK")
                .build();

        AgenticWorkflows.sequence()
                .subAgents(stepA, stepB)
                .outputKey("second")
                .build()
                .invoke(Map.of("request", "用户请求"));

        assertThat(model.requests())
                .as("子 agent 请求应同时包含 fw 会话上下文与原生 scope 上下文，互不覆盖")
                .anySatisfy(request -> {
                    assertThat(request).contains("B: 用户请求");
                    assertThat(request).contains("会话记忆上下文=链式OK");
                    assertThat(request).contains("共享上下文=A-OUT");
                });
    }

    @Test
    void facadeUntypedAgentBuilderShouldSupportSessionContext() {
        ScriptedChatModel model = new ScriptedChatModel().agent("OUT");

        UntypedAgent agent = AgenticWorkflows.agentBuilder()
                .chatModel(model)
                .userMessage("{{q}}")
                .inputKey(String.class, "q")
                .contextProvider(request -> "门面会话上下文=OK")
                .build();

        agent.invoke(Map.of("q", "hi"));

        assertThat(model.requests())
                .as("门面主体（AgenticWorkflows.agentBuilder()）的 contextProvider 应生效")
                .anySatisfy(request -> assertThat(request).contains("门面会话上下文=OK"));
    }

    @Test
    void facadeTypedAgentBuilderShouldSupportSessionContext() {
        ScriptedChatModel model = new ScriptedChatModel().agent("OUT");
        List<String> seen = Collections.synchronizedList(new ArrayList<>());

        SessionAgent agent = AgenticWorkflows.agent(SessionAgent.class)
                .chatModel(model)
                .memoryStore(new AgenticBuilderStorageTest.RecordingChatMemoryStore())
                .contextProvider(request -> {
                    seen.add(request.getSessionKey());
                    return "门面typed上下文=OK";
                })
                .build();

        agent.chat("你好", "0:app:agent:user7:session1");

        assertThat(seen).as("typed 门面的来源应收到会话键").contains("0:app:agent:user7:session1");
        assertThat(model.requests())
                .as("typed 门面的 contextProvider 应生效")
                .anySatisfy(request -> assertThat(request).contains("门面typed上下文=OK"));
    }

}
