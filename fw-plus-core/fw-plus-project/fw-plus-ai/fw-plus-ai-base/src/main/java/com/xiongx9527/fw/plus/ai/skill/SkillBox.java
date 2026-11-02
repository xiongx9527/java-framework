package com.xiongx9527.fw.plus.ai.skill;

import java.util.Set;

/**
 * 技能盒：Agent 侧持有的一批技能，并负责把它们渲染成系统提示词片段。
 *
 * <p><b>作用</b>：{@link SkillRegistry} 管「存」，SkillBox 管「用」——决定暴露哪些技能给模型
 * （{@link #getSkillPrompt(SkillFilter)}）、以何种粒度暴露（仅元数据 or 含正文）、以及运行期动态增删。
 * 多 Agent 共享同一注册表时，各自持有独立 SkillBox 即可做到「同一批技能、不同可见范围」。</p>
 *
 * <p><b>出处</b>：B 类（AgentScope 特殊抽象）。镜像 {@code io.agentscope.core.skill.SkillBox}
 * （agentscope-core 2.0.4）。</p>
 *
 * <p><b>刻意未抽象的部分</b>：AgentScope 的 SkillBox 还有
 * {@code bindToolkit}/{@code copyForToolkit}/{@code registerSkillLoadTool}/{@code uploadSkillFiles}/
 * {@code setWorkDir}/{@code getCodeExecutionWorkDir} 等成员，它们直接依赖
 * {@code io.agentscope.core.tool.Toolkit} 与本地文件系统，属于 AgentScope 运行时细节，
 * 保留在 {@code fw-plus-ai-agentscope} 的实现类中，不进中立端口层。</p>
 *
 * <p><b>实现约定</b>：线程安全；{@link #getSkillPrompt()} 在无可见技能时返回空字符串而非 {@code null}。</p>
 *
 * @author xiongxiao
 */
public interface SkillBox {

    /**
     * 渲染全部可见技能的提示词片段（按当前默认筛选器）。
     *
     * @return 提示词片段，无可见技能时为空字符串
     */
    String getSkillPrompt();

    /**
     * 按指定筛选器渲染提示词片段。
     *
     * @param filter 筛选器，为 {@code null} 时等价于 {@link SkillFilter#all()}
     * @return 提示词片段，无可见技能时为空字符串
     */
    String getSkillPrompt(SkillFilter filter);

    /**
     * 是否暴露全部技能元数据（{@code true} 时把每个技能的 name/description 全量写入提示词，
     * 否则只暴露「按需装载」入口，由模型通过工具拉取）。
     *
     * @param exposeAllSkillMetadata 是否全量暴露
     */
    void setExposeAllSkillMetadata(boolean exposeAllSkillMetadata);

    /**
     * 向技能盒注册技能（同时写入其 {@link SkillRegistry}）。
     *
     * @param skill 技能实例
     */
    void registerSkill(AgentSkill skill);

    /**
     * 列出技能盒内全部技能 ID。
     *
     * @return 只读 ID 集合，永不为 {@code null}
     */
    Set<String> getAllSkillIds();

    /**
     * 按 ID 取技能。
     *
     * @param skillId 技能 ID
     * @return 技能实例；不存在时返回 {@code null}
     */
    AgentSkill getSkill(String skillId);

    /**
     * 卸载技能。
     *
     * @param skillId 技能 ID
     */
    void removeSkill(String skillId);

    /**
     * 判断技能是否存在。
     *
     * @param skillId 技能 ID
     * @return 存在返回 {@code true}
     */
    boolean exists(String skillId);
}
