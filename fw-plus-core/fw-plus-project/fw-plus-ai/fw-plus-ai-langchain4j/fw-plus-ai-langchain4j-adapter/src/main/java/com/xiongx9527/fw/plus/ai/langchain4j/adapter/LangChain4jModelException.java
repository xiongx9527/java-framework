package com.xiongx9527.fw.plus.ai.langchain4j.adapter;

public class LangChain4jModelException extends RuntimeException {

    public LangChain4jModelException(String message) {
        super(message);
    }

    public LangChain4jModelException(String message, Throwable cause) {
        super(message, cause);
    }
}
