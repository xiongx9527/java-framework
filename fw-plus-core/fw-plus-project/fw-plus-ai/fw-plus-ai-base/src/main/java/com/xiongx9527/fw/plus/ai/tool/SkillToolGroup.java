package com.xiongx9527.fw.plus.ai.tool;

import java.util.Set;

/**
 * 技能工具组：与某个技能绑定的工具组，技能被装载时整组激活。
 *
 * <p><b>作用</b>：技能正文告诉模型「怎么用」，技能工具组提供「用什么」。二者绑定后，
 * 装载技能即可一次性把该技能专属的工具（脚本执行、资源读取等）挂到当前会话，
 * 卸载技能时再整组停用，避免工具清单无限膨胀。</p>
 *
 * <p><b>出处</b>：B 类（AgentScope 特殊抽象）。镜像 {@code io.agentscope.core.tool.SkillToolGroup}
 * （agentscope-core 2.0.4），父类为 {@link ToolGroup}。</p>
 *
 * <p><b>实现约定</b>：{@link #getActivateOnSkill()} 必须非空；{@link #copy()} 需保留该绑定关系。</p>
 *
 * @author xiongxiao
 */
public class SkillToolGroup extends ToolGroup {

    private final String activateOnSkill;

    /**
     * 构造技能工具组；仅供 {@link SkillBuilder} 使用。
     *
     * @param name            组名
     * @param description     组描述
     * @param active          初始是否激活
     * @param tools           工具名集合
     * @param activateOnSkill 绑定的技能 ID
     */
    protected SkillToolGroup(String name, String description, boolean active, Set<String> tools, String activateOnSkill) {
        super(name, description, active, ToolGroupScope.META, tools);
        this.activateOnSkill = activateOnSkill;
    }

    /**
     * 绑定的技能 ID：该技能装载时本组自动激活。
     *
     * @return 技能 ID
     */
    public String getActivateOnSkill() { return activateOnSkill; }

    /**
     * 拷贝，保留技能绑定关系。
     *
     * @return 新实例
     */
    @Override
    public ToolGroup copy() {
        return new SkillToolGroup(getName(), getDescription(), isActive(), getTools(), activateOnSkill);
    }

    /**
     * 创建技能工具组构建器。
     *
     * @return 构建器
     */
    public static SkillBuilder skillBuilder() { return new SkillBuilder(); }

    /** {@link SkillToolGroup} 构建器。 */
    public static class SkillBuilder extends Builder {
        private String activateOnSkill;

        /**
         * 绑定技能 ID。
         *
         * @param activateOnSkill 技能 ID
         * @return 本构建器
         */
        public SkillBuilder activateOnSkill(String activateOnSkill) {
            this.activateOnSkill = activateOnSkill;
            return this;
        }

        @Override
        public SkillBuilder name(String name) { super.name(name); return this; }

        @Override
        public SkillBuilder description(String description) { super.description(description); return this; }

        @Override
        public SkillBuilder active(boolean active) { super.active(active); return this; }

        @Override
        public SkillBuilder tools(Set<String> tools) { super.tools(tools); return this; }

        @Override
        public SkillBuilder tool(String toolName) { super.tool(toolName); return this; }

        @Override
        public SkillToolGroup build() {
            return new SkillToolGroup(name, description, active, tools, activateOnSkill);
        }
    }
}
