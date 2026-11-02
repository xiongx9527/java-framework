package com.xiongx9527.fw.plus.common.debounce;

import com.xiongx9527.fw.plus.common.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DebounceBlockedExceptionTest {

    @Test
    void extendsBizExceptionWithIdempotencyCode() {
        DebounceBlockedException e = new DebounceBlockedException();
        assertTrue(e instanceof BizException);
        assertEquals(40_025_001, DebounceBlockedException.CODE);
        assertEquals(DebounceBlockedException.CODE, e.getCode());
        assertNull(e.getDetailOverride());
        assertEquals(-1L, e.getWindowMs());
    }

    @Test
    void keepsCustomDetailAndWindow() {
        DebounceBlockedException e = new DebounceBlockedException("请勿重复提交", 1500L);
        assertEquals("请勿重复提交", e.getDetailOverride());
        assertEquals(1500L, e.getWindowMs());
        assertEquals(DebounceBlockedException.CODE, e.getCode());
    }
}
