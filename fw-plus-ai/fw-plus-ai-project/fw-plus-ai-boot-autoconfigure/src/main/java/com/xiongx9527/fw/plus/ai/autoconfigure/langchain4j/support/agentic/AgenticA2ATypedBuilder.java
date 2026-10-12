package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.internal.A2AClientBuilder;
import dev.langchain4j.agentic.observability.AgentListener;

import java.util.function.Consumer;

/**
 * A2A（Agent-to-Agent）远程客户端构建器（typed，隔离层）：传入带 {@code @Agent} 注解的接口，
 * 产物为该接口的可执行远程代理，可作为 subAgent 参与编排。
 *
 * <p>为避免门面构造阶段触达网络，本类在 {@link #build()} 时才创建底层客户端并连接服务端。</p>
 *
 * @param <T> 调用方声明的远程 agent 接口类型
 */
public final class AgenticA2ATypedBuilder<T> {

    private final String url;
    private final Class<T> agentServiceClass;
    private String outputKey;
    private boolean async;
    private AgentListener listener;
    private Consumer<?> clientCustomizer;
    private String tenant;

    AgenticA2ATypedBuilder(String url, Class<T> agentServiceClass) {
        this.url = url;
        this.agentServiceClass = agentServiceClass;
    }

    public AgenticA2ATypedBuilder<T> outputKey(String outputKey) {
        this.outputKey = outputKey;
        return this;
    }

    public AgenticA2ATypedBuilder<T> async(boolean async) {
        this.async = async;
        return this;
    }

    public AgenticA2ATypedBuilder<T> listener(AgentListener listener) {
        this.listener = listener;
        return this;
    }

    public AgenticA2ATypedBuilder<T> clientCustomizer(Consumer<?> clientCustomizer) {
        this.clientCustomizer = clientCustomizer;
        return this;
    }

    public AgenticA2ATypedBuilder<T> tenant(String tenant) {
        this.tenant = tenant;
        return this;
    }

    /**
     * 构建远程 agent 代理（此处会创建底层 A2A 客户端并连接服务端）。
     *
     * @return 远程 agent 接口代理
     */
    public T build() {
        A2AClientBuilder<T> builder = AgenticServices.a2aBuilder(url, agentServiceClass);
        if (outputKey != null) {
            builder.outputKey(outputKey);
        }
        builder.async(async);
        if (listener != null) {
            builder.listener(listener);
        }
        if (clientCustomizer != null) {
            builder.clientCustomizer(clientCustomizer);
        }
        if (tenant != null) {
            builder.tenant(tenant);
        }
        return builder.build();
    }
}
