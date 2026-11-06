package com.xiongx9527.fw.plus.ai.askdata.answer;

/**
 * 自然语言答案合成能力（§6.38 反问/答案环节）。把“SQL + 结果集”转成对用户问题的可读回答，
 * 是“问数”产品最关键的一环：管线不再止于返回原始结果集。
 */
public interface AnswerCapability {
    AnswerResult answer(AnswerInput input);
}
