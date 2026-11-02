package com.xiongx9527.fw.plus.ai.skill;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 技能（Skill）：一份可被 Agent 动态装载的能力包。
 *
 * <p><b>作用</b>：把一个 {@code SKILL.md}（含 front-matter 元数据 + 正文指令）连同其附带资源
 * （脚本、模板、参考文档等）打包成一个可注册、可筛选、可按需注入提示词、可暴露为工具的单元。
 * 与 {@link com.xiongx9527.fw.plus.ai.tool.ToolSpecification} 的区别：工具是「一次调用」的原子能力，
 * 技能是「一段指令 + 一组资源」的组合能力，通常通过工具（load-skill）被按需拉取。</p>
 *
 * <p><b>出处</b>：B 类（AgentScope 特殊抽象）。镜像 {@code io.agentscope.core.skill.AgentSkill}
 * （agentscope-core 2.0.4），语义遵循 Anthropic Agent Skills 的 {@code SKILL.md} 约定；
 * langchain4j 1.23.0 与 Spring AI 均无对应类型。</p>
 *
 * <p><b>实现约定</b>：</p>
 * <ul>
 *   <li>不可变；所有 {@code Map}/{@code Set} 入参在构造时做防御性拷贝并以只读视图暴露。</li>
 *   <li>{@link #name()} 是技能的唯一业务标识（对应目录名），{@link #skillId()} 默认等于 name，
 *       适配器如需「同名不同来源」的多版本共存，可用 {@link Builder#skillId(String)} 覆写。</li>
 *   <li>{@link #skillContent()} 为 SKILL.md 去掉 front-matter 后的正文，是注入提示词的主体。</li>
 *   <li>{@link #resources()} 为「相对路径 → 文本内容」，二进制资源由适配器自行以 base64 或外部引用表达。</li>
 * </ul>
 *
 * @author xiongxiao
 */
public class AgentSkill {

    private final String name;
    private final String description;
    private final Map<String, Object> metadata;
    private final String skillContent;
    private final String source;
    private final Map<String, String> resources;
    private final String skillId;
    private final Path originDir;

    protected AgentSkill(Builder builder) {
        if (builder.name == null || builder.name.trim().isEmpty()) {
            throw new IllegalArgumentException("skill name must not be blank");
        }
        this.name = builder.name;
        this.description = builder.description;
        this.metadata = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(builder.metadata));
        this.skillContent = builder.skillContent;
        this.source = builder.source;
        this.resources = Collections.unmodifiableMap(new LinkedHashMap<String, String>(builder.resources));
        this.skillId = builder.skillId == null ? builder.name : builder.skillId;
        this.originDir = builder.originDir;
    }

    /**
     * 技能名，对应 SKILL.md 所在目录名，同时是注册到 {@link SkillRegistry} 的业务标识。
     *
     * @return 非空的技能名
     */
    public String name() { return name; }

    /**
     * 技能的一句话描述，来自 front-matter 的 {@code description}，用于让模型判断「何时该装载这个技能」。
     *
     * @return 描述，可能为 {@code null}
     */
    public String description() { return description; }

    /**
     * front-matter 元数据（如 {@code name}/{@code description}/{@code version}/{@code allowed-tools}）。
     *
     * @return 只读元数据视图，永不为 {@code null}
     */
    public Map<String, Object> metadata() { return metadata; }

    /**
     * 按 key 读取元数据。
     *
     * @param key 元数据键
     * @return 对应值，不存在时为 {@code null}
     */
    public Object metadataValue(String key) { return metadata.get(key); }

    /**
     * SKILL.md 的正文（已剥离 front-matter），装载后作为指令注入到系统提示词。
     *
     * @return 技能正文，可能为 {@code null}
     */
    public String skillContent() { return skillContent; }

    /**
     * 技能来源标识，用于区分内置（classpath）、文件系统、运行时上传等不同出处。
     *
     * @return 来源标识，可能为 {@code null}
     */
    public String source() { return source; }

    /**
     * 技能附带资源：相对路径 → 文本内容。
     *
     * @return 只读资源视图，永不为 {@code null}
     */
    public Map<String, String> resources() { return resources; }

    /**
     * 按相对路径读取资源内容。
     *
     * @param path 资源相对路径
     * @return 资源文本，不存在时为 {@code null}
     */
    public String resource(String path) { return resources.get(path); }

    /**
     * 全部资源路径。
     *
     * @return 只读路径集合，永不为 {@code null}
     */
    public Set<String> resourcePaths() { return resources.keySet(); }

    /**
     * 技能注册 ID，默认等于 {@link #name()}；用于同一技能多来源/多版本共存。
     *
     * @return 非空的技能 ID
     */
    public String skillId() { return skillId; }

    /**
     * 技能原始目录（若技能来自文件系统），供适配器定位/回写资源。
     *
     * @return 原始目录，非文件系统来源时为空
     */
    public Optional<Path> originDir() { return Optional.ofNullable(originDir); }

    /**
     * 以当前实例为模板开启构建，便于局部覆盖。
     *
     * @return 已填充当前字段的构建器
     */
    public Builder toBuilder() {
        return new Builder()
                .name(name)
                .description(description)
                .metadata(metadata)
                .skillContent(skillContent)
                .source(source)
                .resources(resources)
                .skillId(skillId)
                .originDir(originDir);
    }

    /**
     * 创建构建器。
     *
     * @return 空构建器
     */
    public static Builder builder() { return new Builder(); }

    @Override
    public String toString() {
        return "AgentSkill{name='" + name + "', skillId='" + skillId + "', source='" + source
                + "', resources=" + resources.size() + '}';
    }

    /** {@link AgentSkill} 构建器；除 name 外均可选。 */
    public static class Builder {
        private String name;
        private String description;
        private Map<String, Object> metadata = new LinkedHashMap<String, Object>();
        private String skillContent;
        private String source;
        private Map<String, String> resources = new LinkedHashMap<String, String>();
        private String skillId;
        private Path originDir;

        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata == null ? new LinkedHashMap<String, Object>() : new LinkedHashMap<String, Object>(metadata);
            return this;
        }
        public Builder metadata(String key, Object value) { this.metadata.put(key, value); return this; }
        public Builder skillContent(String skillContent) { this.skillContent = skillContent; return this; }
        public Builder source(String source) { this.source = source; return this; }
        public Builder resources(Map<String, String> resources) {
            this.resources = resources == null ? new LinkedHashMap<String, String>() : new LinkedHashMap<String, String>(resources);
            return this;
        }
        public Builder resource(String path, String content) { this.resources.put(path, content); return this; }
        public Builder skillId(String skillId) { this.skillId = skillId; return this; }
        public Builder originDir(Path originDir) { this.originDir = originDir; return this; }
        public AgentSkill build() { return new AgentSkill(this); }
    }
}
