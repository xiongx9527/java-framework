package com.xiongx9527.fw.plus.ai.support.retrieval;

/**
 * 检索方式（对齐 Dify）：语义 / 全文 / 混合。
 */
public enum RetrievalMode {

    /** 向量召回。 */
    SEMANTIC,

    /** 词法（倒排）召回。 */
    FULL_TEXT,

    /** 向量 + 词法，按权重融合。 */
    HYBRID
}
