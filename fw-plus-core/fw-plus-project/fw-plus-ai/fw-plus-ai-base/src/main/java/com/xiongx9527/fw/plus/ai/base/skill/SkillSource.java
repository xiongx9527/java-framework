package com.xiongx9527.fw.plus.ai.base.skill;

/**
 * 技能来源。
 */
public enum SkillSource {

    /** 随包发布，扫描 classpath / 外部目录得到。 */
    LOCAL,

    /** 远程技能目录，按 id@version 拉取。 */
    REMOTE
}
