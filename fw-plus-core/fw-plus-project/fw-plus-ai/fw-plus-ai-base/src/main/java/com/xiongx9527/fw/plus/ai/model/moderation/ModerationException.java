package com.xiongx9527.fw.plus.ai.model.moderation;

/** 内容审核拦截异常，镜像 langchain4j 1.23.0 的 {@code dev.langchain4j.service.ModerationException}。 */
public class ModerationException extends RuntimeException {
    public ModerationException(String message) { super(message); }
}
