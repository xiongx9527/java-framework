package com.xiongx9527.fw.plus.ai.support.memory;

/**
 * 持久化 AI 消息在「持久历史」中的插入位置。
 *
 * <p>持久历史的基础结构为：[System] + [历史 User（剥离 RAG）...] + [策略选中的 AI（按位置插入）]。
 * 通过该枚举把持久化 AI 放到不同插入点，从而实现「关键放前尾、不重要放中间」。</p>
 */
public enum AiPersistencePosition {

    /** 置于最前（HEAD）。 */
    HEAD,

    /** 紧跟 SystemMessage 之后，适合全局性、关键性约束、核心结论。 */
    AFTER_SYSTEM,

    /** 插入到第一个历史 UserMessage 之前。 */
    BEFORE_FIRST_USER,

    /** 插入到历史 UserMessage 块的中间。 */
    MIDDLE,

    /** 插入到倒数第二条历史 UserMessage 之后、最后一条之前。 */
    BEFORE_LAST_USER,

    /** 插入到最后一条历史 UserMessage 之后（默认位置）。 */
    AFTER_LAST_USER,

    /** 置于持久历史最末尾（TAIL），最靠近下一轮，时效性最高。 */
    TAIL
}
