package com.xiongx9527.fw.plus.ai.exception;

import com.xiongx9527.fw.plus.common.exception.BizException;

/**
 * AI 配置非法异常，镜像 langchain4j 1.23.0 的 {@code dev.langchain4j.exception.IllegalConfigurationException}，
 * 并集成框架统一 {@link BizException}（8 位状态码）。
 *
 * <p>状态码 {@code 50_016_001} = SERVER + CONFIG(16) + 001（框架内置异常占用各分类 001-099 段）。
 */
public class IllegalConfigurationException extends BizException {

    private static final long serialVersionUID = 1L;

    public static final int CODE = 50_016_001;

    public IllegalConfigurationException(String message) {
        super(CODE, message);
    }

    public static IllegalConfigurationException illegalConfiguration(String format, Object... args) {
        return new IllegalConfigurationException(String.format(format, args));
    }
}
