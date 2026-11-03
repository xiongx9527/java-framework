package com.xiongx9527.fw.plus.ai.base.intent;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 与意图绑定的组件：{@code intents} 为空表示「全局 / 默认」（任何意图都激活），
 * 否则仅当分类结果命中其中之一才激活。
 *
 * <p>用于意图路由——同一支 AI 服务下，不同意图接入不同的工具、检索或技能。</p>
 */
public final class IntentScoped<T> {

    private final T value;

    private final Set<String> intents;

    public IntentScoped(T value, Set<String> intents) {
        this.value = value;
        this.intents = intents == null ? Collections.<String>emptySet()
                : Collections.unmodifiableSet(new LinkedHashSet<String>(intents));
    }

    public T get() {
        return value;
    }

    public Set<String> getIntents() {
        return intents;
    }

    /** 是否对该意图生效（空集恒为真）。 */
    public boolean appliesTo(String intentRef) {
        if (intents.isEmpty()) {
            return true;
        }
        return intentRef != null && intents.contains(intentRef);
    }

    /** 便捷构造：全局组件。 */
    public static <T> IntentScoped<T> global(T value) {
        return new IntentScoped<T>(value, null);
    }

    /** 便捷构造：绑定指定意图。 */
    public static <T> IntentScoped<T> on(T value, String... intents) {
        Set<String> set = new LinkedHashSet<String>();
        if (intents != null) {
            Collections.addAll(set, intents);
        }
        return new IntentScoped<T>(value, set);
    }
}
