package com.xiongx9527.fw.plus.ai.base.trace;

/**
 * 节点状态。
 */
public enum SpanStatus {

    /** 未决（还没结束）。 */
    UNSET,

    /** 成功。 */
    OK,

    /** 失败。 */
    ERROR
}
