package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic;

import dev.langchain4j.agentic.workflow.HumanInTheLoop;

/**
 * 框架自有句柄：包装 {@link HumanInTheLoop}，供编排挂载使用。
 */
public final class AgenticHumanInTheLoop {

    private final HumanInTheLoop delegate;

    private AgenticHumanInTheLoop(HumanInTheLoop delegate) {
        this.delegate = delegate;
    }

    static AgenticHumanInTheLoop of(HumanInTheLoop delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("human-in-the-loop delegate must not be null");
        }
        return new AgenticHumanInTheLoop(delegate);
    }

    HumanInTheLoop delegate() {
        return delegate;
    }
}
