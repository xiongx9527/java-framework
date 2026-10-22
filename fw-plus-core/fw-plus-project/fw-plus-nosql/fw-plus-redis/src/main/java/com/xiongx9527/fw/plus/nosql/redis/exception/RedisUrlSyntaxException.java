package com.xiongx9527.fw.plus.nosql.redis.exception;

import com.xiongx9527.fw.plus.common.exception.BizException;

/**
 * Exception thrown when a Redis URL is malformed or invalid.
 *
 * @author Scott Frederick
 */
public class RedisUrlSyntaxException extends BizException {

    /** 状态码 50016001（SERVER/CONFIG/001 Redis URL 配置非法） */
    public static final int CODE = 50_016_001;

    /**
     * 构造一个RedisURL语法异常实例
     * @param url 导致异常的Redis URL字符串
     */
    public RedisUrlSyntaxException(String url) {
        super(CODE, buildMessage(url));
    }

    /**
     * 构造一个RedisURL语法异常实例
     * @param url 导致异常的Redis URL字符串
     * @param cause 异常的根本原因
     */
    public RedisUrlSyntaxException(String url, Throwable cause) {
        super(CODE, buildMessage(url), cause);
    }

    /**
     * 构建异常消息字符串
     * @param url Redis URL字符串
     * @return 格式化后的异常消息
     */
    private static String buildMessage(String url) {
        return "Invalid Redis URL '" + url + "'";
    }

}
