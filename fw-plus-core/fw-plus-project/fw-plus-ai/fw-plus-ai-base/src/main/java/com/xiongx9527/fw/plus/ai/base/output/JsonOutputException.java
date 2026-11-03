package com.xiongx9527.fw.plus.ai.base.output;

import com.xiongx9527.fw.plus.common.exception.BizException;

/**
 * 稳定 JSON 输出失败：模型在多次尝试后仍无法产出可解析的 JSON。
 */
public class JsonOutputException extends BizException {

    private static final long serialVersionUID = 1L;

    /** 状态码：服务端 + 大模型 + 004。 */
    public static final int CODE = 50_008_004;

    public JsonOutputException(String message) {
        super(CODE, message);
    }

    public JsonOutputException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}
