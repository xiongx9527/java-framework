package com.xiongx9527.fw.plus.ai.support.skill;

import com.xiongx9527.fw.plus.ai.base.skill.SkillBox;

/**
 * 技能提示词渲染：把可见技能列表拼成可注入 prompt 的清单。
 *
 * <p>技能数量大时只取前 {@code maxSkills} 个（调用方应先按相关性排序），避免描述吃满上下文、
 * 也降低模型选错率——这是 skill 能力的「组合 / 收口」部分，放在 support。</p>
 */
public final class SkillPromptRenderer {

    private final SkillBox skillBox;

    private final int maxSkills;

    public SkillPromptRenderer(SkillBox skillBox, int maxSkills) {
        this.skillBox = skillBox;
        this.maxSkills = maxSkills <= 0 ? 20 : maxSkills;
    }

    public SkillPromptRenderer(SkillBox skillBox) {
        this(skillBox, 20);
    }

    /** 重新加载仓库并渲染当前可见技能的清单。 */
    public String renderAndReload() {
        skillBox.reload();
        return skillBox.describeForPrompt(maxSkills);
    }

    /** 用已加载的技能渲染（不重新扫描仓库）。 */
    public String render() {
        return skillBox.describeForPrompt(maxSkills);
    }
}
