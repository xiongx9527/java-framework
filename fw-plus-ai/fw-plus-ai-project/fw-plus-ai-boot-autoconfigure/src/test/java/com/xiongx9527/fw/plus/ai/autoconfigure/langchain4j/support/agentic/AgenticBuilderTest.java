package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.observability.AgentRequest;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link AgenticBuilder}（单 agent 构建器，对标 {@code AssistantBuilder}）：
 * typed/untyped 两条路径的构建与执行、name 透传、untyped 专属方法的 typed 守卫、流式返回 self。
 */
class AgenticBuilderTest {

    public interface EchoAgent {
        @Agent(name = "echo", description = "回显输入", outputKey = "out")
        @UserMessage("请回显：{{text}}")
        String echo(@V("text") String text);
    }

    @Test
    void typedAgentShouldBuildAndExecute() {
        EchoAgent echo = AgenticBuilder.agent(EchoAgent.class)
                .chatModel(new ScriptedChatModel().agent("ECHO-OUT"))
                .build();

        assertThat(echo.echo("hello")).isEqualTo("ECHO-OUT");
    }

    @Test
    void untypedAgentShouldBuildAndExecuteWithInputKeyAndOutputKey() {
        UntypedAgent agent = AgenticBuilder.agent()
                .chatModel(new ScriptedChatModel().agent("UNTYPED-OUT"))
                .userMessage("回答：{{question}}")
                .inputKey(String.class, "question")
                .outputKey("answer")
                .build();

        assertThat(agent.invoke(Map.of("question", "问题"))).isEqualTo("UNTYPED-OUT");
    }

    @Test
    void nameShouldBeForwardedToAgent() {
        List<String> names = new ArrayList<>();
        AgentListener listener = new AgentListener() {
            @Override
            public void beforeAgentInvocation(AgentRequest agentRequest) {
                names.add(agentRequest.agentName());
            }
        };

        UntypedAgent agent = AgenticBuilder.agent()
                .chatModel(new ScriptedChatModel().agent("X"))
                .userMessage("回答：{{q}}")
                .inputKey(String.class, "q")
                .name("my-agent")
                .description("我的 agent")
                .outputKey("out")
                .listener(listener)
                .build();

        agent.invoke(Map.of("q", "x"));

        assertThat(names).contains("my-agent");
    }

    @Test
    void typedBuilderShouldRejectUntypedOnlyMethods() {
        assertThatThrownBy(() -> AgenticBuilder.agent(EchoAgent.class).returnType(String.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("untyped");
    }

    @Test
    void fluentSettersShouldReturnSameInstance() {
        AgenticBuilder<UntypedAgent> builder = AgenticBuilder.agent();

        assertThat(builder.chatModel(new ScriptedChatModel().agent("X"))).isSameAs(builder);
        assertThat(builder.name("n")).isSameAs(builder);
        assertThat(builder.outputKey("o")).isSameAs(builder);
        assertThat(builder.async(true)).isSameAs(builder);
    }
}
