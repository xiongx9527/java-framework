package com.xiongx9527.fw.plus.ai.support.retrieval;

/**
 * 索引模式（对齐 Dify）：
 *
 * <ul>
 *   <li><b>HIGH_QUALITY</b>：向量 + 倒排索引，支持语义 / 全文 / 混合检索与重排；</li>
 *   <li><b>ECONOMICAL</b>：只有全文索引，不支持语义与混合检索，也不做重排。</li>
 * </ul>
 *
 * <p>索引模式决定「能做什么检索」，检索模式决定「这次怎么做」——两者在
 * {@link RetrievalStrategies} 里组合成具体的检索链。</p>
 */
public enum IndexMode {

    HIGH_QUALITY,

    ECONOMICAL;

    /** 该索引模式是否支持某种检索方式。 */
    public boolean supports(RetrievalMode mode) {
        if (this == HIGH_QUALITY) {
            return true;
        }
        return mode == RetrievalMode.FULL_TEXT;
    }

    /** 该索引模式是否支持重排（经济索引没有向量，重排无意义）。 */
    public boolean supportsRerank() {
        return this == HIGH_QUALITY;
    }

    /** 该索引模式的默认检索方式。 */
    public RetrievalMode defaultMode() {
        return this == HIGH_QUALITY ? RetrievalMode.HYBRID : RetrievalMode.FULL_TEXT;
    }
}
