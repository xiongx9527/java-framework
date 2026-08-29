package com.xiongx9527.fw.plus.ai.langchain4j.adapter.model;

import com.xiongx9527.fw.plus.common.exception.BizException;

public class LangChain4jModelException extends BizException {

    public LangChain4jModelException(String message) {
        super(message);
    }

    public LangChain4jModelException(String message, Throwable cause) {
        super(message, cause);
    }
}
