package com.xiongx9527.fw.plus.ai.support.moderation;

/**
 * 审核作用点：在请求前审输入、在返回后审输出。
 *
 * <p>流式通道无法「先审后发」输出（增量已发出去），所以流式只审输入；非流式两端都审。</p>
 */
public enum ModerationMode {

    /** 只审输入（消息文本）。 */
    INPUT,

    /** 只审输出（模型正文）。 */
    OUTPUT,

    /** 两端都审（非流式推荐）。 */
    BOTH,

    /** 不审。 */
    NONE
}
