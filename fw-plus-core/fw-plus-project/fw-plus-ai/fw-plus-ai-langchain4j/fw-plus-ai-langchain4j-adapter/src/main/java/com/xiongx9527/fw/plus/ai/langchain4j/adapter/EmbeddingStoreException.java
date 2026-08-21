package com.xiongx9527.fw.plus.ai.langchain4j.adapter;

import com.xiongx9527.fw.plus.common.exception.BizException;

public class EmbeddingStoreException extends BizException {

    public EmbeddingStoreException(String message) {
        super(message);
    }

    public EmbeddingStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
