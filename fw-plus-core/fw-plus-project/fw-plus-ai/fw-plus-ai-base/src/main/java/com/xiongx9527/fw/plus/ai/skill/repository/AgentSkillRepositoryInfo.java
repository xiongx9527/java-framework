package com.xiongx9527.fw.plus.ai.skill.repository;

/**
 * 技能仓库自描述信息。
 *
 * <p><b>作用</b>：给运维/健康检查一个可读的仓库画像——它是哪种介质、落在哪里、能否写回，
 * 避免排查「为什么技能没装载」时只能翻日志。</p>
 *
 * <p><b>出处</b>：B 类（AgentScope 特殊抽象）。镜像
 * {@code io.agentscope.core.skill.repository.AgentSkillRepositoryInfo}（agentscope-core 2.0.4）。</p>
 *
 * @author xiongxiao
 */
public class AgentSkillRepositoryInfo {

    private final String type;
    private final String location;
    private final boolean writable;

    /**
     * 构造仓库信息。
     *
     * @param type     仓库类型标识（如 {@code classpath} / {@code filesystem} / {@code runtime}）
     * @param location 仓库位置（jar 路径、目录路径或上下文键）
     * @param writable 是否可写
     */
    public AgentSkillRepositoryInfo(String type, String location, boolean writable) {
        this.type = type;
        this.location = location;
        this.writable = writable;
    }

    /**
     * 仓库类型标识。
     *
     * @return 类型标识
     */
    public String type() { return type; }

    /**
     * 仓库位置。
     *
     * @return 位置描述
     */
    public String location() { return location; }

    /**
     * 是否可写。
     *
     * @return 可写返回 {@code true}
     */
    public boolean writable() { return writable; }

    @Override
    public String toString() {
        return "AgentSkillRepositoryInfo{type='" + type + "', location='" + location + "', writable=" + writable + '}';
    }
}
