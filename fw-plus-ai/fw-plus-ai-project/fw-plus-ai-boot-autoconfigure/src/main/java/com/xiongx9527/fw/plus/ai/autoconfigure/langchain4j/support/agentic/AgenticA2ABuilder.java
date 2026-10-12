package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.internal.A2AClientBuilder;
import dev.langchain4j.agentic.observability.AgentListener;

import java.util.function.Consumer;

/**
 * A2A（Agent-to-Agent）远程客户端构建器（untyped，隔离层）：指定服务端地址构建远程 agent
 * 客户端，产物为 {@link UntypedAgent} 句柄，可作为 subAgent 或工具参与编排。
 *
 * <p>为避免门面构造阶段触达网络，本类在 {@link #build()} 时才创建底层客户端并连接服务端；
 * 因此只构建不 {@code build()} 时不会发起网络请求。</p>
 */
public final class AgenticA2ABuilder {

    private final String url;
    private String[] inputKeys;
    private String outputKey;
    private boolean async;
    private AgentListener listener;
    private Consumer<?> clientCustomizer;
    private String tenant;

    AgenticA2ABuilder(String url) {
        this.url = url;
    }

    public AgenticA2ABuilder inputKeys(String... inputKeys) {
        this.inputKeys = inputKeys;
        return this;
    }

    public AgenticA2ABuilder outputKey(String outputKey) {
        this.outputKey = outputKey;
        return this;
    }

    public AgenticA2ABuilder async(boolean async) {
        this.async = async;
        return this;
    }

    public AgenticA2ABuilder listener(AgentListener listener) {
        this.listener = listener;
        return this;
    }

    public AgenticA2ABuilder clientCustomizer(Consumer<?> clientCustomizer) {
        this.clientCustomizer = clientCustomizer;
        return this;
    }

    public AgenticA2ABuilder tenant(String tenant) {
        this.tenant = tenant;
        return this;
    }

    /**
     * 构建远程 agent 句柄（此处会创建底层 A2A 客户端并连接服务端）。
     *
     * @return 远程 agent 的 {@link UntypedAgent} 句柄
     */
    public UntypedAgent build() {
        A2AClientBuilder<UntypedAgent> builder = AgenticServices.a2aBuilder(url);
        if (inputKeys != null) {
            builder.inputKeys(inputKeys);
        }
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
