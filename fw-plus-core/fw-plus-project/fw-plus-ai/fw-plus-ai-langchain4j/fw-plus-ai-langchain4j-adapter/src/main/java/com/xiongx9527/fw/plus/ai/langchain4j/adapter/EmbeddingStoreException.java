package com.xiongx9527.fw.plus.ai.langchain4j.adapter;

public class EmbeddingStoreException extends RuntimeException {

    public EmbeddingStoreException(String message) {
        super(message);
    }

    public EmbeddingStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
