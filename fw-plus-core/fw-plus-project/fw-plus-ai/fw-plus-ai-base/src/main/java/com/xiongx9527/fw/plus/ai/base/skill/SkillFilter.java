package com.xiongx9527.fw.plus.ai.base.skill;

/**
 * 技能过滤器：按来源 / 权限 / 标签筛选可见技能。
 */
public interface SkillFilter {

    /** 全通过。 */
    SkillFilter ALL = new SkillFilter() {
        @Override
        public boolean accept(SkillDescriptor skill) {
            return true;
        }
    };

    boolean accept(SkillDescriptor skill);
}
