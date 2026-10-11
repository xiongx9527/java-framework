/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xiongx9527.fw.plus.ai.autoconfigure.agentscope;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.memory.Memory;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import com.xiongx9527.fw.plus.ai.autoconfigure.agentscope.properties.AgentProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.agentscope.properties.AgentscopeProperties;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;

/**
 * Spring Boot auto-configuration that exposes default Memory, Toolkit and ReActAgent beans for
 * AgentScope.
 *
 * <p>Model beans are provided by provider-specific starters such as
 * {@code agentscope-dashscope-spring-boot-starter}, {@code agentscope-openai-spring-boot-starter},
 * {@code agentscope-gemini-spring-boot-starter}, {@code agentscope-anthropic-spring-boot-starter},
 * or by user-defined {@link Model} beans.
 *
 * <p>Basic configuration:
 *
 * <pre>{@code
 * agentscope:
 *   agent:
 *     enabled: true
 *     name: "Assistant"
 *     sys-prompt: "You are a helpful AI assistant."
 *     max-iters: 10
 * }</pre>
 *
 * <p>本类是 AgentScope Java 官方 {@code agentscope-spring-boot-starter} v2.0.4 自动配置源码的
 * 移植（Apache-2.0 许可）。官方 starter 面向 Spring Boot 4 编译，fw-plus 分发线为
 * Spring Boot 3.5.8，因此抄录其源码至 fw 命名空间而非直接引入官方 starter 构件；
 * 配置前缀与官方保持一致（{@code agentscope.agent.*} / {@code agentscope.model.*}）。
 *
 * <p>版本对齐（由 fw-plus-ai-boot-dependencies 钉住，避免被 Spring Boot 3.5.8 管理压低）：
 * reactor-core 3.8.2、jackson 2.21.1、opentelemetry 1.61.0。这些高于 Boot 3.5.8 BOM 的
 * 默认管理版本，引用前请确认与使用方其它依赖不冲突。
 */
@AutoConfiguration
@EnableConfigurationProperties(AgentscopeProperties.class)
@ConditionalOnClass(ReActAgent.class)
public class AgentscopeAutoConfiguration {

    /**
     * Default Memory implementation backed by InMemoryMemory.
     *
     * <p>
     * Memory is stateful and not thread-safe, so we expose it as a prototype-scoped
     * bean.
     * In multi-threaded / web environments, it is recommended to obtain instances
     * lazily via
     * {@code ObjectProvider<Memory>} or method injection.
     */
    @Bean
    @ConditionalOnProperty(prefix = "agentscope.agent", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public Memory agentscopeMemory() {
        return new InMemoryMemory();
    }

    /**
     * Default Toolkit implementation with an initially empty tool set.
     *
     * <p>
     * Toolkit holds mutable state and is not thread-safe, so it is also exposed as
     * a
     * prototype-scoped bean. In application code, prefer obtaining instances lazily
     * via
     * {@code ObjectProvider<Toolkit>} or method injection.
     */
    @Bean
    @ConditionalOnProperty(prefix = "agentscope.agent", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public Toolkit agentscopeToolkit() {
        return new Toolkit();
    }

    /**
     * Default ReActAgent that wires together the configured Model, Memory and
     * Toolkit beans using
     * {@link AgentProperties}.
     *
     * ReActAgent in 2.0 is thread-safe, so we just use a singleton instance.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(Model.class)
    @ConditionalOnProperty(prefix = "agentscope.agent", name = "enabled", havingValue = "true")
    public ReActAgent agentscopeReActAgent(
            Model model, Memory memory, Toolkit toolkit, AgentscopeProperties properties) {
        AgentProperties config = properties.getAgent();
        return ReActAgent.builder()
                .name(config.getName())
                .sysPrompt(config.getSysPrompt())
                .model(model)
                .toolkit(toolkit)
                .maxIters(config.getMaxIters())
                .build();
    }
}