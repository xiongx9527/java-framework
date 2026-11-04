package com.xiongx9527.fw.plus.ai.interactive;

/**
 * 反问 / 确认的触发原因（对应 {@code clarify} 帧的 {@code reason} 字段）。
 *
 * <p>判定必须由代码做，不由模型判断（同一场景今天问、明天也要问，文案走 i18n）。</p>
 */
public enum ClarifyReason {

    /** 必填项未填或置信度低于阈值。 */
    MISSING,
    /** 候选 &gt; 1 且无法排序，需要用户选择。 */
    AMBIGUOUS,
    /** 请求超出能力 / 权限 / 配额。 */
    OUT_OF_RANGE,
    /** 写操作或高成本操作前的二次确认。 */
    CONFIRM
}
