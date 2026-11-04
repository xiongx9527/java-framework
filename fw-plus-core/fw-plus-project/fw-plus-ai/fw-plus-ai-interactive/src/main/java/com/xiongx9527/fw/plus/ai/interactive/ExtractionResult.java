package com.xiongx9527.fw.plus.ai.interactive;

/**
 * 从用户自然语言里抽取某道题答案的结果。
 *
 * <p>{@code certain = false} 表示抽取不确定（例如选项模糊匹配到多个 / 没匹配到 /
 * 正则不命中），此时调用方应继续反问，而不是强行填充。</p>
 */
public final class ExtractionResult {

    private final Object value;
    private final boolean certain;

    private ExtractionResult(Object value, boolean certain) {
        this.value = value;
        this.certain = certain;
    }

    public static ExtractionResult certain(Object value) {
        return new ExtractionResult(value, true);
    }

    public static ExtractionResult uncertain() {
        return new ExtractionResult(null, false);
    }

    public Object getValue() {
        return value;
    }

    public boolean isCertain() {
        return certain;
    }
}
