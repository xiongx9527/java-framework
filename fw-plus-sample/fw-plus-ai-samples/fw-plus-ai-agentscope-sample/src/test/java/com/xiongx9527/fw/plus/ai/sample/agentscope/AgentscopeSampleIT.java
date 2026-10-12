package com.xiongx9527.fw.plus.ai.sample.agentscope;

import com.xiongx9527.fw.plus.ai.sample.agentscope.tool.SampleTools;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.Memory;
import io.agentscope.core.model.Model;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.tool.Toolkit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AgentScope 集成样例 IT。
 *
 * <p>默认只验证 Spring 上下文装配（不访问任何外部服务）：
 *
 * <ul>
 *   <li>{@code AgentscopeAutoConfiguration} 生效：{@link Memory} / {@link Toolkit} / {@link ReActAgent}
 *       三个 bean 均已注册；</li>
 *   <li>样例提供的 {@link Model}（OllamaChatModel）被 ReActAgent 依赖注入；</li>
 *   <li>样例工具已注册进 {@link Toolkit}。</li>
 * </ul>
 *
 * <p>实时语音对话测试（调远端 Ollama，见 {@link #liveCall()})默认跳过，仅当启动参数带
 * {@code -Dagentscope.it.live=true} 时执行（需要局域网内可访问的 Ollama，且按
 * application.yaml 配置了 base-url 与 model-name）。
 */
@SpringBootTest(classes = AgentscopeSampleApplication.class)
public class AgentscopeSampleIT {

    @Autowired
    private ReActAgent agent;

    @Autowired
    private Memory memory;

    @Autowired
    private Toolkit toolkit;

    @Autowired
    private Model model;

    @Autowired
    private SampleTools sampleTools;

    @Test
    @DisplayName("AgentscopeAutoConfiguration 装配出 Memory / Toolkit / ReActAgent")
    public void autoConfigurationWiresAgentScopeBeans() {
        assertThat(agent).isNotNull();
        assertThat(agent.getName()).isEqualTo("agentscope-sample");
        assertThat(memory).isNotNull();
        assertThat(toolkit).isNotNull();
    }

    @Test
    @DisplayName("样例 Model bean（OllamaChatModel）已提供并被 ReActAgent 使用")
    public void modelBeanIsProvided() {
        assertThat(model).isNotNull();
        assertThat(model.getClass().getName())
                .startsWith("io.agentscope.extensions.model.ollama.OllamaChatModel");
    }

    @Test
    @DisplayName("样例工具 get_magic_number 已注册进 Toolkit")
    public void sampleToolIsRegistered() {
        assertThat(toolkit.getTool("get_magic_number")).isNotNull();
    }

    @Test
    @DisplayName("实时调用：ReActAgent 自主调用 get_magic_number 工具并给出答案")
    @EnabledIfSystemProperty(named = "agentscope.it.live", matches = "true")
    public void liveCall() {
        int before = sampleTools.getCallCount();

        Msg reply = agent
                .call(List.of(new UserMessage(
                        "What is the magic number? You MUST use the get_magic_number tool.")))
                .block(Duration.ofSeconds(180));

        assertThat(reply).isNotNull();
        int toolCalls = sampleTools.getCallCount() - before;
        assertThat(toolCalls).isPositive();

        String text = reply.getContent().stream()
                .filter(TextBlock.class::isInstance)
                .map(block -> ((TextBlock) block).getText())
                .collect(Collectors.joining("\n"));
        assertThat(text).contains("9527");
    }
}