package com.xiongx9527.fw.plus.mybatis.datascope.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/**
 * 锁定行级数据权限异常的固定状态码（SERVER/AUTHORIZATION/001-002）。
 */
class RowPermissionExceptionCodeTest {

    @Test
    void configExceptionCarriesAuthorizationCode() {
        assertEquals(50_003_001, RowPermissionConfigException.CODE);
        assertEquals(50_003_001, new RowPermissionConfigException("配置损坏").getCode());

        IllegalStateException cause = new IllegalStateException("boom");
        RowPermissionConfigException withCause = new RowPermissionConfigException("配置损坏", cause);
        assertEquals(50_003_001, withCause.getCode());
        assertSame(cause, withCause.getCause());
    }

    @Test
    void handleExceptionCarriesAuthorizationCode() {
        assertEquals(50_003_002, RowPermissionHandleException.CODE);
        assertEquals(50_003_002, new RowPermissionHandleException("过滤失败").getCode());

        IllegalStateException cause = new IllegalStateException("boom");
        RowPermissionHandleException withCause = new RowPermissionHandleException("过滤失败", cause);
        assertEquals(50_003_002, withCause.getCode());
        assertSame(cause, withCause.getCause());
    }
}
