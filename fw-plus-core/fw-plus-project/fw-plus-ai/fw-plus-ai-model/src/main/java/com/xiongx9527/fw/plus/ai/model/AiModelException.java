package com.xiongx9527.fw.plus.ai.model;

import com.xiongx9527.fw.plus.common.exception.BizException;

public class AiModelException extends BizException {

    public AiModelException(String message) {
        super(message);
    }

    public AiModelException(String message, Throwable cause) {
        super(message, cause);
    }
}
