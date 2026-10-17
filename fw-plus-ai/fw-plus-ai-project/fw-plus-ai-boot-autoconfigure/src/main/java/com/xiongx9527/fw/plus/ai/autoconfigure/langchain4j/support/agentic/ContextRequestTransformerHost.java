package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic;

import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.service.AiServiceContext;
import dev.langchain4j.service.AiServices;

import java.util.function.BiFunction;

/**
 * 承载 fw 会话上下文请求变换器的构建器（typed / untyped 注入子类共用）：
 * 在 {@code AgentBuilder#build(scope, context, aiServices)} 扩展钩子里把变换器安装到
 * {@link AiServiceContext#chatRequestTransformer}。
 *
 * <p>组合链口径：上游可能已写入变换器（共享 scope 上下文 / 摘要，见 {@code Context.AgenticScopeContextGenerator}），
 * 本变换器<b>先执行</b>（看到的是用户原话，保证事实检索 / 摘要来源拿到干净输入），上游变换器后执行、
 * 语义不被覆盖。每次 {@code build(scope)} 都是全新 {@link AiServiceContext}，不会重复叠加。</p>
 */
interface ContextRequestTransformerHost {

    /** 已配置的会话上下文变换器；未配置为 {@code null}（不安装，保持零开销）。 */
    BiFunction<ChatRequest, Object, ChatRequest> contextRequestTransformer();

    void contextRequestTransformer(BiFunction<ChatRequest, Object, ChatRequest> transformer);

    /** 安装点：读 public 字段做组合，不覆盖上游写入的槽位语义。 */
    static void install(ContextRequestTransformerHost host, AiServiceContext context, AiServices<?> aiServices) {
        BiFunction<ChatRequest, Object, ChatRequest> transformer = host.contextRequestTransformer();
        if (transformer == null) {
            return;
        }
        BiFunction<ChatRequest, Object, ChatRequest> existing = context.chatRequestTransformer;
        BiFunction<ChatRequest, Object, ChatRequest> composed = existing == null
                ? transformer
                : (request, memoryId) -> existing.apply(transformer.apply(request, memoryId), memoryId);
        aiServices.chatRequestTransformer(composed);
    }
}
