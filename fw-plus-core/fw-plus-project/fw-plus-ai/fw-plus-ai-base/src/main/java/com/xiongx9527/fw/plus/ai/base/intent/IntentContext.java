package com.xiongx9527.fw.plus.ai.base.intent;

import java.util.Collections;
import java.util.Map;

/**
 * 规则引擎类分类器的求值上下文：一条待分类的文本 + 附带的结构化属性。
 *
 * <p>规则（{@link com.xiongx9527.fw.plus.ai.support.intent.IntentRule}）以声明式的方式
 * 基于本上下文判断是否命中某个意图，从而把“规则引擎 / 规范”接入意图分类。</p>
 */
public final class IntentContext {

    private final String text;

    private final Map<String, Object> attributes;

    public IntentContext(String text, Map<String, Object> attributes) {
        this.text = text;
        this.attributes = attributes == null ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(attributes);
    }

    /** 待分类文本（可能为 {@code null}）。 */
    public String getText() {
        return text;
    }

    /** 小写化的文本，便于规则做大小写不敏感匹配。 */
    public String getTextLower() {
        return text == null ? "" : text.toLowerCase();
    }

    /** 全部结构化属性（只读视图）。 */
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /** 取属性；不存在返回 {@code null}。 */
    public Object get(String key) {
        return attributes.get(key);
    }

    /** 属性是否存在且非空。 */
    public boolean has(String key) {
        Object v = attributes.get(key);
        return v != null && !(v instanceof String && ((String) v).isEmpty());
    }

    /** 属性等于期望值（字符串做大小写不敏感比较）。 */
    public boolean attrEquals(String key, String expected) {
        Object v = attributes.get(key);
        if (v == null || expected == null) {
            return false;
        }
        return String.valueOf(v).equalsIgnoreCase(expected);
    }
}
