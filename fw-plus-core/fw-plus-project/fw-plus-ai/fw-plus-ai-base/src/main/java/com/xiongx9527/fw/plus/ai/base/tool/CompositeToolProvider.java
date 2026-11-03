package com.xiongx9527.fw.plus.ai.base.tool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 把多个 {@link ToolProvider} 合并成一个：规格并集，执行时按工具名路由到对应 provider。
 *
 * <p>这样 AiServices 等可以一次性注入多个工具来源（内置 + MCP + 技能），不用业务自己拼。</p>
 */
public final class CompositeToolProvider implements ToolProvider {

    private final List<ToolProvider> providers;

    public CompositeToolProvider(List<ToolProvider> providers) {
        if (providers == null || providers.isEmpty()) {
            throw new IllegalArgumentException("at least one tool provider is required");
        }
        this.providers = Collections.unmodifiableList(new ArrayList<ToolProvider>(providers));
    }

    public static ToolProvider of(ToolProvider... providers) {
        List<ToolProvider> list = new ArrayList<ToolProvider>();
        if (providers != null) {
            Collections.addAll(list, providers);
        }
        return new CompositeToolProvider(list);
    }

    @Override
    public List<ToolSpecification> specifications() {
        List<ToolSpecification> all = new ArrayList<ToolSpecification>();
        for (ToolProvider provider : providers) {
            List<ToolSpecification> specs = provider.specifications();
            if (specs != null) {
                all.addAll(specs);
            }
        }
        return all;
    }

    @Override
    public String execute(ToolExecutionRequest request) {
        if (request == null || request.getName() == null) {
            throw new ToolExecutionException("tool name is required");
        }
        for (ToolProvider provider : providers) {
            if (owns(provider, request.getName())) {
                return provider.execute(request);
            }
        }
        throw new ToolExecutionException("unknown tool: " + request.getName());
    }

    private boolean owns(ToolProvider provider, String name) {
        List<ToolSpecification> specs = provider.specifications();
        if (specs == null) {
            return false;
        }
        for (ToolSpecification spec : specs) {
            if (spec.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }
}
