package com.xiongx9527.fw.plus.ai.base.guard;

import com.xiongx9527.fw.plus.common.exception.BizException;

/** 护栏拒绝时抛出，携带可直接展示的原因。 */
public class GuardrailException extends BizException {

    private static final long serialVersionUID = 1L;

    public static final int CODE = 50_010_003;

    public GuardrailException(String message) {
        super(CODE, message);
    }

    public GuardrailException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}
