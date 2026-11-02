package com.xiongx9527.fw.plus.ai.tool;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 工具组：一组可整体启用/禁用的工具集合。
 *
 * <p><b>作用</b>：工具数量一多，逐个开关不现实。工具组给出「批量激活」的粒度——
 * Agent 初始化时只激活相关组，模型看到的工具清单因此收敛；技能装载本质上也是「激活某个工具组」。</p>
 *
 * <p><b>出处</b>：B 类（AgentScope 特殊抽象）。镜像 {@code io.agentscope.core.tool.ToolGroup}
 * （agentscope-core 2.0.4）。langchain4j 1.23.0 只有扁平的
 * {@code ToolSpecification} 列表，无分组概念。</p>
 *
 * <p><b>实现约定</b>：{@link #active} 可变（运行期激活/停用），其余字段不可变；
 * 工具名集合的增删须线程安全。</p>
 *
 * @author xiongxiao
 */
public class ToolGroup {

    private final String name;
    private final String description;
    private volatile boolean active;
    private final ToolGroupScope scope;
    private final Set<String> tools;

    /**
     * 构造工具组；仅供子类与 {@link Builder} 使用。
     *
     * @param name        组名
     * @param description 组描述
     * @param active      初始是否激活
     * @param scope       作用域
     * @param tools       工具名集合
     */
    protected ToolGroup(String name, String description, boolean active, ToolGroupScope scope, Set<String> tools) {
        this.name = name;
        this.description = description;
        this.active = active;
        this.scope = scope == null ? ToolGroupScope.EXTERNAL : scope;
        this.tools = Collections.synchronizedSet(new LinkedHashSet<String>(tools == null ? Collections.<String>emptySet() : tools));
    }

    /**
     * 组名。
     *
     * @return 组名
     */
    public String getName() { return name; }

    /**
     * 组描述，供提示词说明该组用途。
     *
     * @return 描述
     */
    public String getDescription() { return description; }

    /**
     * 是否已激活。
     *
     * @return 激活返回 {@code true}
     */
    public boolean isActive() { return active; }

    /**
     * 激活/停用该组。
     *
     * @param active 是否激活
     */
    public void setActive(boolean active) { this.active = active; }

    /**
     * 作用域。
     *
     * @return 作用域，永不为 {@code null}
     */
    public ToolGroupScope getScope() { return scope; }

    /**
     * 组内工具名快照。
     *
     * @return 只读工具名集合
     */
    public Set<String> getTools() { return Collections.unmodifiableSet(new LinkedHashSet<String>(tools)); }

    /**
     * 加入一个工具。
     *
     * @param toolName 工具名
     */
    public void addTool(String toolName) { tools.add(toolName); }

    /**
     * 移除一个工具。
     *
     * @param toolName 工具名
     */
    public void removeTool(String toolName) { tools.remove(toolName); }

    /**
     * 判断组内是否包含该工具。
     *
     * @param toolName 工具名
     * @return 包含返回 {@code true}
     */
    public boolean containsTool(String toolName) { return tools.contains(toolName); }

    /**
     * 浅拷贝（工具集合独立，激活状态一并复制）。
     *
     * @return 新实例
     */
    public ToolGroup copy() {
        return new ToolGroup(name, description, active, scope, getTools());
    }

    /**
     * 创建构建器。
     *
     * @return 构建器
     */
    public static Builder builder() { return new Builder(); }

    /** {@link ToolGroup} 构建器。 */
    public static class Builder {
        protected String name;
        protected String description;
        protected boolean active;
        protected ToolGroupScope scope = ToolGroupScope.EXTERNAL;
        protected Set<String> tools = new LinkedHashSet<String>();

        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder active(boolean active) { this.active = active; return this; }
        public Builder scope(ToolGroupScope scope) { this.scope = scope; return this; }
        public Builder tools(Set<String> tools) {
            this.tools = tools == null ? new LinkedHashSet<String>() : new LinkedHashSet<String>(tools);
            return this;
        }
        public Builder tool(String toolName) { this.tools.add(toolName); return this; }
        public ToolGroup build() { return new ToolGroup(name, description, active, scope, tools); }
    }
}
