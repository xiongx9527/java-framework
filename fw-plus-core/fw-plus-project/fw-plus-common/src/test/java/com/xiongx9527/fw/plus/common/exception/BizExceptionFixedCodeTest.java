package com.xiongx9527.fw.plus.common.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.xiongx9527.fw.plus.common.uid.exception.UidGenerateException;

/**
 * 锁定框架内置异常的固定状态码（分配规则：框架占 specific 001-099，业务从 100 起）。
 */
class BizExceptionFixedCodeTest {

    @Test
    void baseRuntimeExceptionCarriesInfrastructureCode() {
        assertEquals(50_010_001, BaseRuntimeException.CODE);
        assertEquals(50_010_001, new BaseRuntimeException().getCode());
        assertEquals(50_010_001, new BaseRuntimeException("boom").getCode());
        assertEquals(50_010_001, new BaseRuntimeException("value=%s", "42").getCode());
        assertEquals("value=42", new BaseRuntimeException("value=%s", "42").getMessage());
    }

    @Test
    void uidGenerateExceptionCarriesInfrastructureCode() {
        assertEquals(50_010_002, UidGenerateException.CODE);
        assertEquals(50_010_002, new UidGenerateException("boom").getCode());
        assertEquals(50_010_002, new UidGenerateException().getCode());
    }

    @Test
    void fixedCodeConstructorKeepsMessageAndCause() {
        IllegalStateException cause = new IllegalStateException("cause");
        BizException messageOnly = new BizException(40_001_001, "参数缺失");
        assertEquals(40_001_001, messageOnly.getCode());
        assertEquals("参数缺失", messageOnly.getMessage());
        assertNull(messageOnly.getData());

        BizException withCause = new BizException(50_009_001, "连接超时", cause);
        assertEquals(50_009_001, withCause.getCode());
        assertEquals("连接超时", withCause.getMessage());
        assertSame(cause, withCause.getCause());
    }

    @Test
    void fixedCodeCauseOnlyConstructorMatchesPlainConstructorSemantics() {
        IllegalStateException cause = new IllegalStateException("cause");
        BizException fixed = new BizException(50_010_003, cause);
        BizException plain = new BizException(cause);
        assertEquals(50_010_003, fixed.getCode());
        assertEquals(plain.getMessage(), fixed.getMessage());
        assertSame(cause, fixed.getCause());
    }

    @Test
    void fixedCodeConstructorRejectsNonEightDigitCode() {
        assertThrows(IllegalStateException.class, () -> new BizException(1234567, "too short"));
        assertThrows(IllegalStateException.class, () -> new BizException(100_000_000, "too long"));
    }
}
