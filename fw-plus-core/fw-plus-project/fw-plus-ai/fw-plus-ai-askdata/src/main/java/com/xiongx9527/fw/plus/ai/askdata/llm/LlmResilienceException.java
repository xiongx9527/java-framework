package com.xiongx9527.fw.plus.ai.askdata.llm;

import com.xiongx9527.fw.plus.common.exception.BizException;

/**
 * LLM 韧性层异常（§6.26 / D.4）：熔断器打开拒绝请求、或重试耗尽后仍失败。
 * 继承 {@link BizException} 以便统一状态码与 i18n。
 *
 * <p>状态码 50_006_801：SERVER(50) + 限流/熔断(6) + 801（本模块内置段，避开框架 001-099）。</p>
 */
public class LlmResilienceException extends BizException {
    public static final int CODE = 50_006_801;

    public LlmResilienceException(String message) {
        super(CODE, message);
    }

    public LlmResilienceException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}
