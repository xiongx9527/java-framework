package com.xiongx9527.fw.plus.ai.askdata.clarify;

/**
 * 低置信/歧义时的澄清反问能力（§6.32 反问）。当 SQL 生成无效或置信度过低时，
 * 不硬执行错误 SQL，而是向用户提出一个澄清问题以收敛意图。
 */
public interface ClarificationCapability {
    ClarificationResult clarify(ClarificationInput input);
}
