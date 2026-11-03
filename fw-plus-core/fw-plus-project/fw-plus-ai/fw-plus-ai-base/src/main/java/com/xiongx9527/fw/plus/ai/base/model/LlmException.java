package com.xiongx9527.fw.plus.ai.base.model;

import com.xiongx9527.fw.plus.common.exception.BizException;

/**
 * 大语言调用失败（模型未配置、上游错误、网络异常等）。
 */
public class LlmException extends BizException {

    private static final long serialVersionUID = 1L;

    /** 状态码：服务端 + 第三方服务 + 001。 */
    public static final int CODE = 50_008_001;

    public LlmException(String message) {
        super(CODE, message);
    }

    public LlmException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}
