package com.xiongx9527.fw.plus.ai.base.event;

/**
 * 事件阶段：开始 / 结束。
 *
 * <p>检索、上下文注入、意图分类这类有“过程”的事件，统一用 {@code START} 与 {@code END}
 * 两个相位表达边界，便于上层做计时、埋点、UI 状态切换。</p>
 */
public enum Phase {

    /** 动作开始（如检索发起、上下文即将注入）。 */
    START,

    /** 动作结束（如检索命中、上下文已注入）。 */
    END
}
