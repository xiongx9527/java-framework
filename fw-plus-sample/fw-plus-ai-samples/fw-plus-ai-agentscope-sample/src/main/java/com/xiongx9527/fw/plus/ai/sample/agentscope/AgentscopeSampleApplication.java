package com.xiongx9527.fw.plus.ai.sample.agentscope;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AgentScope 2 集成样例启动类。
 *
 * <p>演示 fw-plus-ai 对 AgentScope Java 2.0.4 的条件装配：
 *
 * <ul>
 *   <li>{@code AgentscopeAutoConfiguration}（fw-plus-ai-boot-autoconfigure）在 classpath 存在
 *       {@code io.agentscope.core.ReActAgent} 且 {@code agentscope.agent.enabled=true} 时，
 *       自动装配 {@code Memory}（prototype）、{@code Toolkit}（prototype）与 {@code ReActAgent}（singleton）。</li>
 *   <li>本样例提供 {@code OllamaChatModel}（{@code Model} bean），ReActAgent 依赖它完成对话。</li>
 *   <li>自定义工具通过 {@code @io.agentscope.core.tool.Tool} 注解声明，注册进 {@code Toolkit} 后由
 *       ReActAgent 在推理中自主调用。</li>
 * </ul>
 */
@SpringBootApplication(scanBasePackages = "com.xiongx9527.fw.plus.ai.sample.agentscope")
public class AgentscopeSampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentscopeSampleApplication.class, args);
    }
}
