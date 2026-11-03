package com.xiongx9527.fw.plus.ai.base.tool;

/**
 * 工具调用环的停止原因。
 */
public enum ToolStopReason {

    /** 模型不再请求工具，返回最终答案。 */
    FINAL,

    /** 达到最大步数仍未停止。 */
    MAX_STEPS,

    /** 调用过程中出错（上游失败 / 工具全部失败并让模型放弃）。 */
    ERROR,

    /** 没有进展（连续多步无新工具结果），避免死循环。 */
    NO_PROGRESS
}
