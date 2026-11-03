package com.xiongx9527.fw.plus.ai.base.trace;

/**
 * 链路节点的类型：决定它在观测平台里如何归类与着色。
 */
public enum SpanKind {

    /** 大语言补全 / 流式。 */
    LLM,

    /** 文本向量化。 */
    EMBEDDING,

    /** 重排。 */
    RERANK,

    /** 检索（向量 / 全文 / 混合）。 */
    RETRIEVAL,

    /** 工具调用。 */
    TOOL,

    /** 记忆读写。 */
    MEMORY,

    /** 审核。 */
    MODERATION,

    /** 编排：链 / 工作流 / 智能体步骤。 */
    CHAIN,

    /** 其它通用节点。 */
    INTERNAL
}
