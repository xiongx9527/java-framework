package com.xiongx9527.fw.plus.ai.support.skill;

import com.xiongx9527.fw.plus.ai.skill.AgentSkill;

import java.util.List;
import java.util.Map;

/**
 * 远程技能目录端口：从技能市场 / 注册中心按需拉取并调用技能。
 *
 * <p><b>作用</b>：本地技能（classpath / 目录下的 SKILL.md）由 base 的
 * {@link com.xiongx9527.fw.plus.ai.skill.AgentSkillRepository} 负责；本端口面向<b>远程</b>场景：
 * 列出可用技能、按 ID 拉取描述、锁定版本、远程执行。版本锁定很重要——同一会话里技能不能中途变行为。</p>
 *
 * <p><b>出处</b>：C 类（自行抽象）。对应 {@code docs/ai-layer} 增强清单 P2-b「Skill（本地 / 远程）」的 S2，
 * 与 {@code ai-port-layer.md} §6.34.4。AgentScope 只有本地仓库，无远程目录端口。</p>
 *
 * <p><b>实现约定</b>：</p>
 * <ul>
 *   <li>{@link #list(String)} 允许返回缓存结果，但缓存需带版本号。</li>
 *   <li>{@link #describe(String)} 返回的 {@link AgentSkill} 必须填充 {@code source} 与 {@code skillId}。</li>
 *   <li>{@link #invoke(String, Map)} 在远程执行，超时与重试由实现决定；失败应抛
 *       {@link com.xiongx9527.fw.plus.ai.exception.IllegalConfigurationException} 或返回结构化错误文本。</li>
 *   <li>{@link #lockedVersion(String)} 用于会话内版本固定，返回 {@code null} 表示不锁版本。</li>
 * </ul>
 *
 * @author xiongxiao
 */
public interface SkillPort {

    /**
     * 列出可用技能。
     *
     * @param namespace 命名空间 / 租户，{@code null} 表示公共目录
     * @return 技能 ID 列表，永不为 {@code null}
     */
    List<String> list(String namespace);

    /**
     * 拉取技能描述。
     *
     * @param skillId 技能 ID
     * @return 技能，永不为 {@code null}
     */
    AgentSkill describe(String skillId);

    /**
     * 远程调用技能。
     *
     * @param skillId 技能 ID
     * @param args    调用参数
     * @return 调用结果文本
     */
    String invoke(String skillId, Map<String, Object> args);

    /**
     * 查询技能被锁定的版本。
     *
     * @param skillId 技能 ID
     * @return 版本号；不锁定版本时返回 {@code null}
     */
    String lockedVersion(String skillId);
}
