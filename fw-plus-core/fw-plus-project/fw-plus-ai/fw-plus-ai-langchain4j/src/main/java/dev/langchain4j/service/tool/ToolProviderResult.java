package dev.langchain4j.service.tool;

import dev.langchain4j.agent.tool.ReturnBehavior;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.IllegalConfigurationException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ToolProviderResult {
    private final List<AiServiceTool> tools;

    public ToolProviderResult(Builder builder) { tools = builder.buildFinalToolList(); }
    public ToolProviderResult(List<AiServiceTool> tools) { this(builder().addAll(tools)); }
    public ToolProviderResult(Map<ToolSpecification, ToolExecutor> tools) { this(builder().addAll(tools)); }

    public List<AiServiceTool> aiServiceTools() { return tools; }
    @Deprecated public ToolSpecification toolSpecificationByName(String name) {
        for (AiServiceTool tool : tools) if (tool.name().equals(name)) return tool.toolSpecification();
        return null;
    }
    @Deprecated public ToolExecutor toolExecutorByName(String name) {
        for (AiServiceTool tool : tools) if (tool.name().equals(name)) return tool.toolExecutor();
        return null;
    }
    @Deprecated public Map<ToolSpecification, ToolExecutor> tools() {
        Map<ToolSpecification, ToolExecutor> result = new LinkedHashMap<ToolSpecification, ToolExecutor>();
        for (AiServiceTool tool : tools) result.put(tool.toolSpecification(), tool.toolExecutor());
        return result;
    }
    @Deprecated public Set<String> immediateReturnToolNames() {
        Set<String> names = new HashSet<String>();
        for (AiServiceTool tool : tools) {
            if (tool.returnBehavior() == ReturnBehavior.IMMEDIATE) names.add(tool.name());
        }
        return names;
    }
    public Builder toBuilder() { return builder().addAll(tools); }
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final List<AiServiceTool> tools = new ArrayList<AiServiceTool>();
        private final Set<String> immediateReturnToolNames = new HashSet<String>();
        public Builder add(AiServiceTool tool) { tools.add(tool); return this; }
        public Builder add(ToolSpecification specification, ToolExecutor executor) {
            return add(specification, executor, ReturnBehavior.TO_LLM);
        }
        public Builder add(ToolSpecification specification, ToolExecutor executor, ReturnBehavior behavior) {
            return add(AiServiceTool.builder().toolSpecification(specification)
                    .toolExecutor(executor).returnBehavior(behavior).build());
        }
        public Builder addAll(Collection<AiServiceTool> values) {
            if (values != null) tools.addAll(values);
            return this;
        }
        public Builder addAll(Map<ToolSpecification, ToolExecutor> values) {
            if (values != null) {
                for (Map.Entry<ToolSpecification, ToolExecutor> entry : values.entrySet()) {
                    add(entry.getKey(), entry.getValue());
                }
            }
            return this;
        }
        @Deprecated public Builder immediateReturnToolNames(Set<String> values) {
            if (values != null) immediateReturnToolNames.addAll(values);
            return this;
        }
        public ToolProviderResult build() { return new ToolProviderResult(this); }

        private List<AiServiceTool> buildFinalToolList() {
            List<AiServiceTool> result = new ArrayList<AiServiceTool>(tools);
            Map<String, Integer> indexes = new HashMap<String, Integer>();
            for (int i = 0; i < result.size(); i++) {
                String name = result.get(i).name();
                if (indexes.put(name, Integer.valueOf(i)) != null) {
                    throw new IllegalConfigurationException("Duplicated definition for tool: " + name);
                }
            }
            for (String name : immediateReturnToolNames) {
                Integer index = indexes.get(name);
                if (index != null) {
                    AiServiceTool existing = result.get(index.intValue());
                    if (existing.returnBehavior() != ReturnBehavior.IMMEDIATE) {
                        result.set(index.intValue(), existing.toBuilder()
                                .returnBehavior(ReturnBehavior.IMMEDIATE).build());
                    }
                }
            }
            return java.util.Collections.unmodifiableList(result);
        }
    }
}
