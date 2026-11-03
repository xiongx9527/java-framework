package com.xiongx9527.fw.plus.ai.base.intent;

/**
 * 一个候选意图的定义。
 */
public final class Intent {

    private final String ref;

    private final String label;

    private final String description;

    public Intent(String ref, String label, String description) {
        this.ref = ref;
        this.label = label;
        this.description = description;
    }

    public String getRef() {
        return ref;
    }

    public String getLabel() {
        return label;
    }

    /** 用于向量 / 正则匹配的样本或说明。 */
    public String getDescription() {
        return description;
    }

    /** 便捷构造：只有引用与说明。 */
    public static Intent of(String ref, String description) {
        return new Intent(ref, ref, description);
    }
}
