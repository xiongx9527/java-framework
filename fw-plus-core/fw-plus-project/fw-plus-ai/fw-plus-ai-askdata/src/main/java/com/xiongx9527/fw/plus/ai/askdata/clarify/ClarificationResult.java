package com.xiongx9527.fw.plus.ai.askdata.clarify;

import lombok.Builder;
import lombok.Getter;

/**
 * 澄清/反问的输出。
 */
@Getter
@Builder
public class ClarificationResult {

    /** 面向用户的澄清问题（如“你指的是订单表还是支付表？”）。 */
    private final String text;

    /** 是否成功生成（false 时调用方回退为直接报错/返回 SQL）。 */
    @lombok.Builder.Default
    private final boolean valid = false;
}
