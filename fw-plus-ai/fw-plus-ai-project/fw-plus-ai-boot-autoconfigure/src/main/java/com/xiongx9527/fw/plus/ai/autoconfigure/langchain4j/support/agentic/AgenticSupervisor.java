package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic;

import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;

/**
 * 框架自有句柄：包装 {@link SupervisorAgent}，供业务与 magic 脚本使用。
 * <p>仅暴露最小必要方法 {@link #invoke(String)} 与 {@link #invokeWithAgenticScope(String)}。</p>
 */
public final class AgenticSupervisor {

    private final SupervisorAgent delegate;

    private AgenticSupervisor(SupervisorAgent delegate) {
        this.delegate = delegate;
    }

    static AgenticSupervisor of(SupervisorAgent delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("supervisor delegate must not be null");
        }
        return new AgenticSupervisor(delegate);
    }

    /**
     * 同步调用监督者。
     *
     * @param request 用户请求
     * @return 监督者最终响应
     */
    public String invoke(String request) {
        return delegate.invoke(request);
    }

    /**
     * 同步调用监督者并返回结果与作用域。
     *
     * @param request 用户请求
     * @return 结果与作用域
     */
    public ResultWithAgenticScope<String> invokeWithAgenticScope(String request) {
        return delegate.invokeWithAgenticScope(request);
    }

    SupervisorAgent delegate() {
        return delegate;
    }
}
