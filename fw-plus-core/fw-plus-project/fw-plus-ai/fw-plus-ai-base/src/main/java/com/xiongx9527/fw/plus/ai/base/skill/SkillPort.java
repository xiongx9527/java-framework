package com.xiongx9527.fw.plus.ai.base.skill;

import java.util.List;

/**
 * 远程技能目录端口：列出可用技能、按 id 拉取技能内容（SKILL.md 文本）。
 */
public interface SkillPort {

    List<SkillDescriptor> list();

    /** 拉取技能内容；不存在返回 {@code null}。 */
    String load(String skillId);
}
