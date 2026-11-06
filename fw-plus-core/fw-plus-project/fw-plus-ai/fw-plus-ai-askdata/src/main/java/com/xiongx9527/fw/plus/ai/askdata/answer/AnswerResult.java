package com.xiongx9527.fw.plus.ai.askdata.answer;

import lombok.Builder;
import lombok.Getter;

/**
 * 自然语言答案合成的输出。
 */
@Getter
@Builder
public class AnswerResult {

    /** 面向用户的自然语言回答（结论 / 趋势描述）。 */
    private final String text;

    /** 0~1 置信度；生成失败或结果异常时降低。 */
    @lombok.Builder.Default
    private final double confidence = 0.0d;

    /** 是否成功合成（false 时调用方应回退为直接展示 SQL/结果）。 */
    @lombok.Builder.Default
    private final boolean valid = false;
}
