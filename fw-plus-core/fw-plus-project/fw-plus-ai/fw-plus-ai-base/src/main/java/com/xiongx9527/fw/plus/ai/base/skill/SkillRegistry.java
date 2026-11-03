package com.xiongx9527.fw.plus.ai.base.skill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 技能注册表：按 {@code id@version} 锁定版本，不自动跟随 latest。
 *
 * <p>同名技能默认「先注册者生效」，重复注册需显式 {@link #register(SkillDescriptor, boolean)}
 * 覆盖，避免远程升版后行为悄悄变化。</p>
 */
public final class SkillRegistry {

    private final Map<String, SkillDescriptor> active = new LinkedHashMap<String, SkillDescriptor>();

    private final Map<String, SkillDescriptor> byRef = new LinkedHashMap<String, SkillDescriptor>();

    /** 注册技能；已存在同 id 时保留先注册的那个。 */
    public void register(SkillDescriptor skill) {
        register(skill, false);
    }

    /**
     * 注册技能。
     *
     * @param override 为 {@code true} 时覆盖同 id 的已有版本
     */
    public void register(SkillDescriptor skill, boolean override) {
        if (skill == null || skill.getId() == null) {
            return;
        }
        byRef.put(skill.ref(), skill);
        if (override || !active.containsKey(skill.getId())) {
            active.put(skill.getId(), skill);
        }
    }

    /** 按 {@code id} 或 {@code id@version} 查找；找不到返回 {@code null}。 */
    public SkillDescriptor find(String ref) {
        if (ref == null) {
            return null;
        }
        if (ref.indexOf('@') > 0) {
            return byRef.get(ref);
        }
        return active.get(ref);
    }

    public boolean contains(String ref) {
        return find(ref) != null;
    }

    /** 当前生效的技能（每个 id 一个版本）。 */
    public List<SkillDescriptor> list() {
        return Collections.unmodifiableList(new ArrayList<SkillDescriptor>(active.values()));
    }

    public int size() {
        return active.size();
    }

    public void clear() {
        active.clear();
        byRef.clear();
    }
}
