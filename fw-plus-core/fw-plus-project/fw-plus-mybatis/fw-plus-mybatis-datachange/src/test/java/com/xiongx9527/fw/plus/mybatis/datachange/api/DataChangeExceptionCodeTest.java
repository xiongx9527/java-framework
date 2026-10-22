package com.xiongx9527.fw.plus.mybatis.datachange.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.xiongx9527.fw.plus.mybatis.datachange.dialect.SqlAnalysisException;

/**
 * 锁定数据变更异常的固定状态码（SERVER/CDC_STREAM/001-002）。
 */
class DataChangeExceptionCodeTest {

    @Test
    void dataChangeExceptionCarriesCdcStreamCode() {
        assertEquals(50_019_001, DataChangeException.CODE);

        DataChangeException exception = new DataChangeException(DataChangeErrorCode.CONFIGURATION, "后置处理失败");
        assertEquals(50_019_001, exception.getCode());
        assertEquals(DataChangeErrorCode.CONFIGURATION, exception.getErrorCode());

        IllegalStateException cause = new IllegalStateException("boom");
        DataChangeException withCause = new DataChangeException(DataChangeErrorCode.CONFIGURATION, "后置处理失败", cause);
        assertEquals(50_019_001, withCause.getCode());
        assertSame(cause, withCause.getCause());
    }

    @Test
    void sqlAnalysisExceptionCarriesItsOwnCode() {
        assertEquals(50_019_002, SqlAnalysisException.CODE);

        SqlAnalysisException exception = new SqlAnalysisException("不支持的语句");
        assertEquals(50_019_002, exception.getCode());
        assertEquals(DataChangeErrorCode.UNSUPPORTED_SQL, exception.getErrorCode());

        IllegalStateException cause = new IllegalStateException("boom");
        SqlAnalysisException withCause = new SqlAnalysisException("不支持的语句", cause);
        assertEquals(50_019_002, withCause.getCode());
        assertSame(cause, withCause.getCause());
    }
}
