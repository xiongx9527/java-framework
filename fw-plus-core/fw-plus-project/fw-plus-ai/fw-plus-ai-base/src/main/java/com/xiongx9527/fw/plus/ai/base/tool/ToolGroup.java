package com.xiongx9527.fw.plus.ai.base.tool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 工具分组：把一批工具打成一个可见性单元。
 *
 * <p>模型能看到的工具 = 所有「已激活分组」的工具并集；未激活分组里的工具对模型等于不存在，
 * 比「调用后再拒绝」更安全，也更省 token。</p>
 */
public class ToolGroup {

    private final String name;

    private final String description;

    private final ToolGroupScope scope;

    private final List<ToolSpecification> tools;

    public ToolGroup(String name, String description, ToolGroupScope scope, List<ToolSpecification> tools) {
        this.name = name;
        this.description = description;
        this.scope = scope == null ? ToolGroupScope.GLOBAL : scope;
        this.tools = Collections.unmodifiableList(tools == null
                ? new ArrayList<ToolSpecification>() : new ArrayList<ToolSpecification>(tools));
    }

    public static ToolGroup of(String name, ToolSpecification... tools) {
        List<ToolSpecification> list = new ArrayList<ToolSpecification>();
        if (tools != null) {
            Collections.addAll(list, tools);
        }
        return new ToolGroup(name, null, ToolGroupScope.GLOBAL, list);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ToolGroupScope getScope() {
        return scope;
    }

    public List<ToolSpecification> getTools() {
        return tools;
    }

    /** 组内工具名，用于与技能的白名单求交。 */
    public List<String> toolNames() {
        List<String> names = new ArrayList<String>(tools.size());
        for (ToolSpecification tool : tools) {
            names.add(tool.getName());
        }
        return names;
    }
}
