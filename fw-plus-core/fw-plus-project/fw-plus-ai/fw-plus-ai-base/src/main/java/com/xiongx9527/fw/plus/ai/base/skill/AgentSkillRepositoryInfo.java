package com.xiongx9527.fw.plus.ai.base.skill;

/**
 * 技能仓库元信息：用于多仓库合并时的优先级与来源标记。
 */
public final class AgentSkillRepositoryInfo {

    private final String name;

    private final SkillSource source;

    private final int priority;

    public AgentSkillRepositoryInfo(String name, SkillSource source, int priority) {
        this.name = name;
        this.source = source == null ? SkillSource.LOCAL : source;
        this.priority = priority;
    }

    public String getName() {
        return name;
    }

    public SkillSource getSource() {
        return source;
    }

    /** 数值越大越优先；同名技能由高优先级仓库覆盖。 */
    public int getPriority() {
        return priority;
    }
}
