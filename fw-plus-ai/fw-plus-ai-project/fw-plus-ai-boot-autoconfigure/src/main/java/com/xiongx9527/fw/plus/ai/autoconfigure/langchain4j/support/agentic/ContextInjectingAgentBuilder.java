package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic;

import dev.langchain4j.agentic.agent.AgentBuilder;
import dev.langchain4j.agentic.scope.DefaultAgenticScope;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.service.AiServiceContext;
import dev.langchain4j.service.AiServices;

import java.util.function.BiFunction;

/**
 * typed 单 agent 构建器（上游 {@link AgentBuilder} 子类）：在 {@code build(scope, context, aiServices)}
 * 扩展钩子里安装 fw 会话上下文变换器（见 {@link ContextRequestTransformerHost}），使
 * {@link AgenticBuilder} 的 {@code contextProvider} / {@code contextMaintenance} 在「独立执行」与
 * 「挂入编排当 sub-agent」两种形态下都生效。
 *
 * @param <T> 调用方声明的 {@code @Agent} 接口类型
 */
final class ContextInjectingAgentBuilder<T> extends AgentBuilder<T, ContextInjectingAgentBuilder<T>>
        implements ContextRequestTransformerHost {

    private BiFunction<ChatRequest, Object, ChatRequest> contextRequestTransformer;

    ContextInjectingAgentBuilder(Class<T> agentServiceClass) {
        super(agentServiceClass);
    }

    @Override
    public BiFunction<ChatRequest, Object, ChatRequest> contextRequestTransformer() {
        return contextRequestTransformer;
    }

    @Override
    public void contextRequestTransformer(BiFunction<ChatRequest, Object, ChatRequest> transformer) {
        this.contextRequestTransformer = transformer;
    }

    @Override
    protected void build(DefaultAgenticScope agenticScope, AiServiceContext context, AiServices<T> aiServices) {
        super.build(agenticScope, context, aiServices);
        ContextRequestTransformerHost.install(this, context, aiServices);
    }
}
