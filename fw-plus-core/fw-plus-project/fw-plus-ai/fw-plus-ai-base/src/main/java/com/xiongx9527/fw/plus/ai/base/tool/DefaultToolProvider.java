package com.xiongx9527.fw.plus.ai.base.tool;

import java.util.List;

/**
 * 把「一个对象上的 {@code @Tool} 方法」同时当作规格来源与执行器。
 */
public class DefaultToolProvider implements ToolProvider {

    private final List<ToolSpecification> specifications;

    private final ToolExecutor executor;

    public DefaultToolProvider(Object target, ToolArgumentCodec codec) {
        this.specifications = ToolSpecifications.from(target);
        this.executor = new DefaultToolExecutor(target, codec);
    }

    @Override
    public List<ToolSpecification> specifications() {
        return specifications;
    }

    @Override
    public String execute(ToolExecutionRequest request) {
        return executor.execute(request);
    }
}
