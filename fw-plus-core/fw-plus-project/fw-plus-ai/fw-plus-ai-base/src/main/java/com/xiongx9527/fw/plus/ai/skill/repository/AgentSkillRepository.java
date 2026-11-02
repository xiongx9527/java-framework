package com.xiongx9527.fw.plus.ai.skill.repository;

import com.xiongx9527.fw.plus.ai.skill.AgentSkill;

import java.util.List;

/**
 * 技能仓库：技能的外部来源（classpath、文件系统、运行时上下文等）的统一读写端口。
 *
 * <p><b>作用</b>：{@code SkillRegistry} 是进程内索引，仓库才是「技能从哪来、能不能写回」的抽象。
 * 典型实现有三类：打包在 jar 里的内置技能（只读）、部署目录下的技能文件夹（可读写）、
 * 运行时上传/挂载的技能（会话级）。装载流程通常是「仓库列出全部技能 → 解析 → 注册进 SkillRegistry」。</p>
 *
 * <p><b>出处</b>：B 类（AgentScope 特殊抽象）。镜像
 * {@code io.agentscope.core.skill.repository.AgentSkillRepository}（agentscope-core 2.0.4），
 * 对应实现类为 {@code ClasspathSkillRepository}/{@code FileSystemSkillRepository}/
 * {@code RuntimeContextSkillRepository}。</p>
 *
 * <p><b>实现约定</b>：</p>
 * <ul>
 *   <li>只读仓库对 {@link #save(List, boolean)} / {@link #delete(String)} 应返回 {@code false}，
 *       而不是抛异常；{@link #isWriteable()} 必须与之一致。</li>
 *   <li>{@link #save(List, boolean)} 的 {@code overwrite} 为 {@code false} 时遇到同名技能应整批不写并返回 {@code false}。</li>
 *   <li>{@link #getAllSkills()} 允许较慢（涉及磁盘扫描），调用方应自行缓存。</li>
 *   <li>{@link #close()} 只释放本仓库持有的资源，不影响已注册到 {@code SkillRegistry} 的技能实例。</li>
 * </ul>
 *
 * @author xiongxiao
 */
public interface AgentSkillRepository extends AutoCloseable {

    /**
     * 按名称取单个技能。
     *
     * @param name 技能名
     * @return 技能实例；不存在时返回 {@code null}
     */
    AgentSkill getSkill(String name);

    /**
     * 列出仓库内全部技能名。
     *
     * @return 技能名列表，永不为 {@code null}
     */
    List<String> getAllSkillNames();

    /**
     * 读取仓库内全部技能。
     *
     * @return 技能列表，永不为 {@code null}
     */
    List<AgentSkill> getAllSkills();

    /**
     * 批量写入技能。
     *
     * @param skills    待写入技能
     * @param overwrite 是否覆盖同名技能
     * @return 全部写入成功返回 {@code true}
     */
    boolean save(List<AgentSkill> skills, boolean overwrite);

    /**
     * 删除技能。
     *
     * @param name 技能名
     * @return 删除成功返回 {@code true}
     */
    boolean delete(String name);

    /**
     * 判断技能是否存在。
     *
     * @param name 技能名
     * @return 存在返回 {@code true}
     */
    boolean skillExists(String name);

    /**
     * 仓库自描述信息（类型、位置、是否可写），用于运维排查与健康检查。
     *
     * @return 仓库信息，永不为 {@code null}
     */
    AgentSkillRepositoryInfo getRepositoryInfo();

    /**
     * 仓库来源标识，写入 {@link AgentSkill#source()}。
     *
     * @return 来源标识
     */
    String getSource();

    /**
     * 切换可写标记（仅影响后续写操作是否被接受，不改变底层介质权限）。
     *
     * @param writeable 是否允许写
     */
    void setWriteable(boolean writeable);

    /**
     * 当前是否可写。
     *
     * @return 可写返回 {@code true}
     */
    boolean isWriteable();

    /**
     * 释放资源；默认空实现。
     */
    @Override
    default void close() {
    }
}
