package com.xiongx9527.fw.plus.ai.askdata.chart;

import com.xiongx9527.fw.plus.common.exception.BizException;

/** 图表生成异常，继承 {@link BizException} 以便统一状态码与 i18n。 */
public class ChartGenerationException extends BizException {
    public ChartGenerationException(String message) {
        super(message);
    }

    public ChartGenerationException(String message, Throwable cause) {
        super(message, cause);
    }

    /** 按状态码国际化构造（消息模板见 i18n/messages，{0} 为原始 reason）。 */
    public ChartGenerationException(int code, Object... args) {
        super(code, args);
    }

    /** 按状态码 + 已解析文案 + 原始异常构造。 */
    public ChartGenerationException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
