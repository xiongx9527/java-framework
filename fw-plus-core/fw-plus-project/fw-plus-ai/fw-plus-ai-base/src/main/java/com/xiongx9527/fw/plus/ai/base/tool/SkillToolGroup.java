package com.xiongx9527.fw.plus.ai.base.tool;

import com.xiongx9527.fw.plus.ai.base.skill.SkillDescriptor;
import java.util.ArrayList;
import java.util.List;

/**
 * 技能工具组：技能装载后激活的那组工具。
 *
 * <p>组的成员 = 技能声明的 {@code toolWhitelist} 与候选工具求交；白名单外的工具即便存在
 * 也不会出现在组里，因此模型看不到、也调不到。</p>
 */
public final class SkillToolGroup extends ToolGroup {

    private final String skillId;

    private SkillToolGroup(String skillId, String description, List<ToolSpecification> tools) {
        super(skillId, description, ToolGroupScope.SKILL, tools);
        this.skillId = skillId;
    }

    /**
     * 按技能白名单裁剪候选工具。
     *
     * @param skill     技能描述
     * @param available 当前可用的全部工具
     */
    public static SkillToolGroup of(SkillDescriptor skill, List<ToolSpecification> available) {
        List<ToolSpecification> selected = new ArrayList<ToolSpecification>();
        if (skill != null && available != null) {
            for (ToolSpecification tool : available) {
                if (skill.allowsTool(tool.getName())) {
                    selected.add(tool);
                }
            }
        }
        return new SkillToolGroup(skill == null ? null : skill.getId(),
                skill == null ? null : skill.getDescription(), selected);
    }

    public String getSkillId() {
        return skillId;
    }
}
