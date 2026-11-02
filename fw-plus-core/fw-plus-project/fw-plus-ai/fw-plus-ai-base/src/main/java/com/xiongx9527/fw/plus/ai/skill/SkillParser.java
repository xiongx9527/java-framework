package com.xiongx9527.fw.plus.ai.skill;

import java.util.Map;

/**
 * SKILL.md 解析器：在「Markdown 文本」与 {@link AgentSkill} 之间做双向转换。
 *
 * <p><b>作用</b>：技能的事实标准是一份带 YAML front-matter 的 Markdown。解析器负责把
 * {@code ---\nname: ...\ndescription: ...\n---\n正文} 拆成元数据 + 正文，反向则拼回可落盘的文本，
 * 使「从磁盘/classpath/ZIP 装载」与「编辑后回写」共用同一套格式约定。</p>
 *
 * <p><b>出处</b>：B 类（AgentScope 特殊抽象）。对应 {@code io.agentscope.core.skill.util.MarkdownSkillParser}
 * （agentscope-core 2.0.4，静态工具类 {@code parse}/{@code generate}）。此处收为接口：
 * 解析涉及 YAML 方言与编码选择，不同适配器可替换实现（例如复用 Spring 的 SnakeYAML、或保持零依赖的轻量解析）。</p>
 *
 * <p><b>实现约定</b>：</p>
 * <ul>
 *   <li>{@link #parse(String, String)} 遇到缺失 front-matter 时，应把全文当作正文、元数据为空，而不是抛异常。</li>
 *   <li>解析失败（格式非法）抛 {@link com.xiongx9527.fw.plus.ai.exception.IllegalConfigurationException}。</li>
 *   <li>实现必须无状态、线程安全。</li>
 * </ul>
 *
 * @author xiongxiao
 */
public interface SkillParser {

    /**
     * 解析 SKILL.md 文本。
     *
     * @param markdown SKILL.md 全文
     * @param source   来源标识，写入 {@link AgentSkill#source()}
     * @return 解析出的技能
     */
    AgentSkill parse(String markdown, String source);

    /**
     * 生成 SKILL.md 文本。
     *
     * @param metadata front-matter 元数据
     * @param body     Markdown 正文
     * @return 可直接落盘的 SKILL.md 文本
     */
    String generate(Map<String, Object> metadata, String body);
}
