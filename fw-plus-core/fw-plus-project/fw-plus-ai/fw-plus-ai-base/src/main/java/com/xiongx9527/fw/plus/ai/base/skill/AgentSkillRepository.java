package com.xiongx9527.fw.plus.ai.base.skill;

import java.util.List;

/**
 * 技能仓库：一个技能来源（内置 classpath、外部目录、远程目录）。
 *
 * <p>加载顺序由 {@link AgentSkillRepositoryInfo#getPriority()} 决定，外部目录优先于内置，
 * 客户自定义技能不必改包。仓库不可用时返回空列表，不抛异常、不阻塞主链。</p>
 */
public interface AgentSkillRepository {

    AgentSkillRepositoryInfo info();

    /** 列出仓库内的全部技能；不可用时返回空列表。 */
    List<SkillDescriptor> loadAll();
}
