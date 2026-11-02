package com.xiongx9527.fw.plus.ai.skill;

import java.util.Set;

/**
 * 技能注册表：进程内「技能 ID → 技能」的索引。
 *
 * <p><b>作用</b>：为 {@link SkillBox} / 提示词渲染器提供按 ID 的快速查找，是技能装载与卸载的唯一入口。
 * 它只做索引，不负责持久化与解析——那是
 * {@link com.xiongx9527.fw.plus.ai.skill.repository.AgentSkillRepository} 与 {@link SkillParser} 的职责。</p>
 *
 * <p><b>出处</b>：B 类（AgentScope 特殊抽象）。镜像 {@code io.agentscope.core.skill.SkillRegistry}
 * （agentscope-core 2.0.4）；该类型在 AgentScope 中为包私有类，此处提升为公开接口，便于各适配器提供实现。</p>
 *
 * <p><b>实现约定</b>：</p>
 * <ul>
 *   <li>实现必须线程安全：技能可能在运行时被并发注册/卸载。</li>
 *   <li>{@link #registerSkill(String, AgentSkill)} 为覆盖语义（同 ID 后写覆盖先写），不抛重复异常。</li>
 *   <li>查询类方法对不存在的 ID 返回 {@code null} 或空集合，不抛异常。</li>
 * </ul>
 *
 * @author xiongxiao
 */
public interface SkillRegistry {

    /**
     * 注册（或覆盖）一个技能。
     *
     * @param skillId 技能 ID，通常取 {@link AgentSkill#skillId()}
     * @param skill   技能实例
     */
    void registerSkill(String skillId, AgentSkill skill);

    /**
     * 按 ID 查找技能。
     *
     * @param skillId 技能 ID
     * @return 技能实例；不存在时返回 {@code null}
     */
    AgentSkill getSkill(String skillId);

    /**
     * 列出当前已注册的全部技能 ID。
     *
     * @return 只读 ID 集合，永不为 {@code null}
     */
    Set<String> getSkillIds();

    /**
     * 判断技能是否已注册。
     *
     * @param skillId 技能 ID
     * @return 已注册返回 {@code true}
     */
    boolean exists(String skillId);

    /**
     * 卸载技能；不存在时静默返回。
     *
     * @param skillId 技能 ID
     */
    void removeSkill(String skillId);
}
