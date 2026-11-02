package com.xiongx9527.fw.plus.ai.skill;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 技能筛选器：决定「哪些技能可以出现在提示词里 / 可以被装载」。
 *
 * <p><b>作用</b>：Agent 往往同时持有大量技能，但每次对话只应暴露相关子集。筛选器把「全部 / 全部禁用 /
 * 白名单 / 黑名单」以及「在父筛选结果上叠加启用或禁用」这几种策略统一成一个可组合的判定对象。</p>
 *
 * <p><b>出处</b>：B 类（AgentScope 特殊抽象）。镜像 {@code io.agentscope.core.skill.SkillFilter}
 * （agentscope-core 2.0.4）的公开 API：{@code all/none/only/except/enable/disable/isAllowed/isOverlay/overlay}。</p>
 *
 * <p><b>语义</b>：</p>
 * <ul>
 *   <li>{@code all()} / {@code none()} / {@code only(..)} / {@code except(..)} 为「基准筛选器」，
 *       {@link #isOverlay()} 返回 {@code false}。</li>
 *   <li>{@code enable(..)} / {@code disable(..)} 为「叠加筛选器」，单独使用时以 {@code all()} 为父，
 *       {@link #isOverlay()} 返回 {@code true}；通过 {@link #overlay(SkillFilter)} 绑定到某个父筛选器后，
 *       在父结果上做并集（enable）或差集（disable）。</li>
 *   <li>实现不可变、线程安全；{@link #overlay(SkillFilter)} 返回新实例，不修改自身。</li>
 * </ul>
 *
 * @author xiongxiao
 */
public final class SkillFilter {

    private enum Mode { ALL, NONE, ONLY, EXCEPT, ENABLE, DISABLE }

    private final Mode mode;
    private final Set<String> names;
    private final SkillFilter parent;

    private SkillFilter(Mode mode, Set<String> names, SkillFilter parent) {
        this.mode = mode;
        this.names = names == null ? Collections.<String>emptySet()
                : Collections.unmodifiableSet(new LinkedHashSet<String>(names));
        this.parent = parent;
    }

    /**
     * 不筛选，全部技能可见。
     *
     * @return 恒真筛选器
     */
    public static SkillFilter all() { return new SkillFilter(Mode.ALL, null, null); }

    /**
     * 全部不可见。
     *
     * @return 恒假筛选器
     */
    public static SkillFilter none() { return new SkillFilter(Mode.NONE, null, null); }

    /**
     * 仅允许给定技能。
     *
     * @param names 技能名或技能 ID
     * @return 白名单筛选器
     */
    public static SkillFilter only(String... names) { return new SkillFilter(Mode.ONLY, toSet(names), null); }

    /**
     * 排除给定技能，其余全部允许。
     *
     * @param names 技能名或技能 ID
     * @return 黑名单筛选器
     */
    public static SkillFilter except(String... names) { return new SkillFilter(Mode.EXCEPT, toSet(names), null); }

    /**
     * 在父筛选结果上叠加启用：父结果并上给定技能。
     *
     * @param names 技能名或技能 ID
     * @return 叠加型筛选器（{@link #isOverlay()} 为 {@code true}）
     */
    public static SkillFilter enable(String... names) { return new SkillFilter(Mode.ENABLE, toSet(names), null); }

    /**
     * 在父筛选结果上叠加禁用：父结果去掉给定技能。
     *
     * @param names 技能名或技能 ID
     * @return 叠加型筛选器（{@link #isOverlay()} 为 {@code true}）
     */
    public static SkillFilter disable(String... names) { return new SkillFilter(Mode.DISABLE, toSet(names), null); }

    /**
     * 判断指定技能是否可见。
     *
     * @param skillName 技能名或技能 ID
     * @return 可见返回 {@code true}
     */
    public boolean isAllowed(String skillName) {
        switch (mode) {
            case ALL:
                return true;
            case NONE:
                return false;
            case ONLY:
                return names.contains(skillName);
            case EXCEPT:
                return !names.contains(skillName);
            case ENABLE:
                return baseAllowed(skillName) || names.contains(skillName);
            case DISABLE:
                return baseAllowed(skillName) && !names.contains(skillName);
            default:
                return true;
        }
    }

    /**
     * 是否为叠加型筛选器。
     *
     * @return {@code enable}/{@code disable} 返回 {@code true}
     */
    public boolean isOverlay() { return mode == Mode.ENABLE || mode == Mode.DISABLE; }

    /**
     * 绑定父筛选器，形成「父结果 + 叠加规则」的复合筛选器。
     *
     * <p>非叠加型筛选器直接返回自身（叠加无意义）。</p>
     *
     * @param parent 父筛选器，为 {@code null} 时等价于 {@code all()}
     * @return 复合筛选器
     */
    public SkillFilter overlay(SkillFilter parent) {
        if (!isOverlay()) {
            return this;
        }
        return new SkillFilter(mode, names, parent);
    }

    private boolean baseAllowed(String skillName) {
        return parent == null || parent.isAllowed(skillName);
    }

    private static Set<String> toSet(String... names) {
        return names == null ? Collections.<String>emptySet() : new LinkedHashSet<String>(Arrays.asList(names));
    }
}
