package com.xiongx9527.fw.plus.ai.base.skill;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 技能描述：一份可复用的「任务说明书」。
 *
 * <p>与工具的区别：工具是一个函数，技能是一次任务的模板——指令正文给模型，
 * 元数据给程序（工具白名单、权限点、是否需要人工确认）。</p>
 *
 * <p>三个字段是关键约束：{@link #getToolWhitelist()} 限定技能能调哪些工具（最小权限），
 * {@link #getPermissions()} 接鉴权链，{@link #isRequiresConfirm()} 决定是否走人工确认。</p>
 */
public final class SkillDescriptor {

    private final String id;
    private final String name;
    private final String version;
    private final String description;
    private final String instructions;
    private final Set<String> toolWhitelist;
    private final Set<String> permissions;
    private final boolean requiresConfirm;
    private final SkillSource source;
    private final String transport;

    private SkillDescriptor(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.version = builder.version;
        this.description = builder.description;
        this.instructions = builder.instructions;
        this.toolWhitelist = Collections.unmodifiableSet(new LinkedHashSet<String>(builder.toolWhitelist));
        this.permissions = Collections.unmodifiableSet(new LinkedHashSet<String>(builder.permissions));
        this.requiresConfirm = builder.requiresConfirm;
        this.source = builder.source;
        this.transport = builder.transport;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getDescription() { return description; }
    public String getInstructions() { return instructions; }
    public Set<String> getToolWhitelist() { return toolWhitelist; }
    public Set<String> getPermissions() { return permissions; }
    public boolean isRequiresConfirm() { return requiresConfirm; }
    public SkillSource getSource() { return source; }
    public String getTransport() { return transport; }

    public String ref() {
        return version == null ? id : id + "@" + version;
    }

    public boolean allowsTool(String toolName) {
        if (toolWhitelist.isEmpty()) {
            return true;
        }
        return toolName != null && toolWhitelist.contains(toolName);
    }

    public boolean hasPermission(String permission) {
        if (permissions.isEmpty()) {
            return true;
        }
        return permission != null && permissions.contains(permission);
    }

    @Override
    public String toString() {
        return "SkillDescriptor{id=" + id + ", version=" + version + ", source=" + source + '}';
    }

    public static final class Builder {

        private final Set<String> toolWhitelist = new LinkedHashSet<String>();
        private final Set<String> permissions = new LinkedHashSet<String>();
        private String id;
        private String name;
        private String version;
        private String description;
        private String instructions;
        private boolean requiresConfirm;
        private SkillSource source = SkillSource.LOCAL;
        private String transport = "in-process";

        private Builder() { }

        public Builder id(String value) { this.id = value; return this; }
        public Builder name(String value) { this.name = value; return this; }
        public Builder version(String value) { this.version = value; return this; }
        public Builder description(String value) { this.description = value; return this; }
        public Builder instructions(String value) { this.instructions = value; return this; }
        public Builder toolWhitelist(Set<String> value) { this.toolWhitelist.clear(); if (value != null) this.toolWhitelist.addAll(value); return this; }
        public Builder addTool(String value) { if (value != null && !value.isEmpty()) this.toolWhitelist.add(value); return this; }
        public Builder permissions(Set<String> value) { this.permissions.clear(); if (value != null) this.permissions.addAll(value); return this; }
        public Builder addPermission(String value) { if (value != null && !value.isEmpty()) this.permissions.add(value); return this; }
        public Builder requiresConfirm(boolean value) { this.requiresConfirm = value; return this; }
        public Builder source(SkillSource value) { this.source = value == null ? SkillSource.LOCAL : value; return this; }
        public Builder transport(String value) { this.transport = value; return this; }

        public SkillDescriptor build() {
            if (id == null || id.isEmpty()) {
                throw new IllegalArgumentException("skill id is required");
            }
            return new SkillDescriptor(this);
        }
    }
}

