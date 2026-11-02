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
}
