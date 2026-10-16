package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory;

/**
 * AI 持久化消息在持久历史中的插入位置。
 * <p>
 * 持久历史的基础结构为：{@code [System] + [历史 User（剥离 RAG）...] + [持久化的 AI（按策略+位置插入）]}
 * 通过此枚举控制持久化 AI 在该结构中的插入点，以实现「关键放前尾、不重要放中间」。
 * </p>
 */
public enum AiPersistencePosition {

    /**
     * 置于最前（HEAD）。
     * <p>插入到所有消息之前（优先级高于 {@link #AFTER_SYSTEM}）。</p>
     */
    HEAD,

    /**
     * 紧跟 SystemMessage 之后。
     * <p>适合全局性、关键性约束、核心结论、长期不变的指令。</p>
     */
    AFTER_SYSTEM,

    /**
     * 插入到「第一个历史 UserMessage」之前。
     * <p>位于 System 与历史对话之间，适合需要先于历史上下文生效的关键结论。</p>
     */
    BEFORE_FIRST_USER,

    /**
     * 插入到历史 UserMessage 块的中间。
     * <p>将持久化消息均匀插入到历史 User 列表的中部，用于降低注意力权重、不太关键但仍需保留的内容。</p>
     */
    MIDDLE,

    /**
     * 插入到「倒数第二条历史 UserMessage」之后，「最后一条历史 UserMessage」之前。
     * <p>用于放置次要结论，但又比中间更靠近当前轮次。</p>
     */
    BEFORE_LAST_USER,

    /**
     * 插入到「最后一条历史 UserMessage」之后。
     * <p><b>默认位置</b>。靠近下一轮 UserMessage，适合本轮的最终结论、决策，时效性较高。</p>
     */
    AFTER_LAST_USER,

    /**
     * 置于持久历史最末尾（TAIL）。
     * <p>最靠近即将到来的新一轮 UserMessage，时效性最高。适合关键结论、最终回答。</p>
     */
    TAIL
}
