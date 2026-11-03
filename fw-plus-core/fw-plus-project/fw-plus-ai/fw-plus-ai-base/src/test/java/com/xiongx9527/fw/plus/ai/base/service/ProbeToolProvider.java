package com.xiongx9527.fw.plus.ai.base.service;

import com.xiongx9527.fw.plus.ai.base.tool.ToolExecutionRequest;
import com.xiongx9527.fw.plus.ai.base.tool.ToolProvider;
import com.xiongx9527.fw.plus.ai.base.tool.ToolSpecification;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 测试用：暴露一个具名工具。 */
final class DefaultToolProviderProbe implements ToolProvider {
    private final String name;
    DefaultToolProviderProbe(String name) { this.name = name; }
    @Override public List<ToolSpecification> specifications() {
        Map<String, Object> p = new LinkedHashMap<String, Object>();
        p.put("type", "object");
        return Collections.singletonList(ToolSpecification.builder().name(name).description(name).parameters(p).build());
    }
    @Override public String execute(ToolExecutionRequest request) { return "ran " + name; }
}
