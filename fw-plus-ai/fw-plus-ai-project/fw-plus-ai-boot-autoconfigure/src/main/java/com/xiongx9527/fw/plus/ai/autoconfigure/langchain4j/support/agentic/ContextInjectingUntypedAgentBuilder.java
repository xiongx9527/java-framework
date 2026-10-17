package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic;

import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.agent.UntypedAgentBuilder;
import dev.langchain4j.agentic.scope.DefaultAgenticScope;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.service.AiServiceContext;
import dev.langchain4j.service.AiServices;

import java.util.function.BiFunction;

/**
 * untyped 单 agent 构建器（上游 {@link UntypedAgentBuilder} 子类）：在
 * {@code build(scope, context, aiServices)} 扩展钩子里安装 fw 会话上下文变换器
 * （见 {@link ContextRequestTransformerHost}），语义同 {@link ContextInjectingAgentBuilder}。
 */
final class ContextInjectingUntypedAgentBuilder extends UntypedAgentBuilder
        implements ContextRequestTransformerHost {

    private BiFunction<ChatRequest, Object, ChatRequest> contextRequestTransformer;

    @Override
    public BiFunction<ChatRequest, Object, ChatRequest> contextRequestTransformer() {
        return contextRequestTransformer;
    }

    @Override
    public void contextRequestTransformer(BiFunction<ChatRequest, Object, ChatRequest> transformer) {
        this.contextRequestTransformer = transformer;
    }

    @Override
    protected void build(DefaultAgenticScope agenticScope, AiServiceContext context, AiServices<UntypedAgent> aiServices) {
        super.build(agenticScope, context, aiServices);
        ContextRequestTransformerHost.install(this, context, aiServices);
    }
}
