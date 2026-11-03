package com.xiongx9527.fw.plus.ai.base.skill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 技能容器：合并多个仓库、按优先级去重、按过滤器收窄，并提供 prompt 注入视图。
 *
 * <p>合并规则：仓库优先级高的覆盖低的；同优先级时先加载者生效（外部目录排在内置之前）。
 * 仓库不可用时该仓库贡献空列表，主链不报错。</p>
 */
public final class SkillBox {

    private final List<AgentSkillRepository> repositories = new ArrayList<AgentSkillRepository>();

    private final SkillRegistry registry = new SkillRegistry();

    private SkillFilter filter = SkillFilter.ALL;

    /** 添加仓库（内部按优先级降序加载）。 */
    public SkillBox addRepository(AgentSkillRepository repository) {
        if (repository != null) {
            repositories.add(repository);
        }
        return this;
    }

    public SkillBox filter(SkillFilter value) {
        this.filter = value == null ? SkillFilter.ALL : value;
        return this;
    }

    public SkillRegistry registry() {
        return registry;
    }

    /** 重新加载全部仓库；先清空再按优先级合并。 */
    public SkillBox reload() {
        registry.clear();
        List<AgentSkillRepository> ordered = new ArrayList<AgentSkillRepository>(repositories);
        Collections.sort(ordered, new Comparator<AgentSkillRepository>() {
            @Override
            public int compare(AgentSkillRepository left, AgentSkillRepository right) {
                return Integer.compare(priority(right), priority(left));
            }
        });
        for (AgentSkillRepository repository : ordered) {
            List<SkillDescriptor> skills;
            try {
                skills = repository.loadAll();
            } catch (RuntimeException ignored) {
                continue;
            }
            if (skills == null) {
                continue;
            }
            for (SkillDescriptor skill : skills) {
                if (skill != null && filter.accept(skill)) {
                    registry.register(skill, true);
                }
            }
        }
        return this;
    }

    public List<SkillDescriptor> list() {
        return registry.list();
    }

    public SkillDescriptor get(String ref) {
        return registry.find(ref);
    }

    /**
     * 生成注入 prompt 的技能清单。
     *
     * <p>技能数量超过上限时只取前 {@code maxSkills} 个（调用方应先按相关性排序），
     * 避免描述把上下文吃满、也降低模型选错的概率。</p>
     *
     * @param maxSkills 最多注入几个；{@code <= 0} 表示不限制
     */
    public String describeForPrompt(int maxSkills) {
        List<SkillDescriptor> skills = registry.list();
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (SkillDescriptor skill : skills) {
            if (maxSkills > 0 && count >= maxSkills) {
                break;
            }
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append("- ").append(skill.getId());
            if (skill.getDescription() != null) {
                sb.append(": ").append(skill.getDescription());
            }
            count++;
        }
        return sb.toString();
    }

    private static int priority(AgentSkillRepository repository) {
        AgentSkillRepositoryInfo info = repository.info();
        return info == null ? 0 : info.getPriority();
    }
}
