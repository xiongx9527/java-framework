package com.xiongx9527.fw.plus.nosql.redis.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/**
 * 锁定 RedisUrlSyntaxException 的固定状态码（SERVER/CONFIG/001）。
 */
class RedisUrlSyntaxExceptionCodeTest {

    @Test
    void carriesConfigCode() {
        assertEquals(50_016_001, RedisUrlSyntaxException.CODE);
    }

    @Test
    void messageAndCauseBehaviorIsPreserved() {
        RedisUrlSyntaxException exception = new RedisUrlSyntaxException("redis://");
        assertEquals(50_016_001, exception.getCode());
        assertEquals("Invalid Redis URL 'redis://'", exception.getMessage());

        IllegalStateException cause = new IllegalStateException("boom");
        RedisUrlSyntaxException withCause = new RedisUrlSyntaxException("redis://", cause);
        assertEquals(50_016_001, withCause.getCode());
        assertEquals("Invalid Redis URL 'redis://'", withCause.getMessage());
        assertSame(cause, withCause.getCause());
    }
}
