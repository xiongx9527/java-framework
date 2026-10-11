package com.xiongx9527.fw.plus.ai.sample.agentscope.config;

import io.agentscope.core.model.Model;
import io.agentscope.extensions.model.ollama.OllamaChatModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 提供 {@link Model} bean（Ollama）。
 *
 * <p>{@code AgentscopeAutoConfiguration} 中的 {@code ReActAgent} 标注了
 * {@code @ConditionalOnBean(Model.class)}：只要容器里存在任意 {@code Model} bean，就会自动装配
 * ReActAgent。本类即填充该缺口，把 {@link SampleOllamaProperties} 转换为 {@code OllamaChatModel}。
 *
 * <p>声明为 {@code @Bean} 后，使用方无需引入 AgentScope 官方的
 * {@code agentscope-ollama-spring-boot-starter} 即可让 ReActAgent 工作。
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SampleOllamaProperties.class)
public class AgentscopeModelConfiguration {

    @Bean
    public Model ollamaChatModel(SampleOllamaProperties properties) {
        return OllamaChatModel.builder()
                .modelName(properties.getModelName())
                .baseUrl(properties.getBaseUrl())
                .build();
    }
}
