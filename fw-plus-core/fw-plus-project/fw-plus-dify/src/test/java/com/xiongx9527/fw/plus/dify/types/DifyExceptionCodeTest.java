package com.xiongx9527.fw.plus.dify.types;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.xiongx9527.fw.plus.dify.dataset.types.external.KnowledgeException;

/**
 * 锁定 Dify 相关异常的固定状态码（SERVER/THIRD_PARTY/001-003）。
 */
class DifyExceptionCodeTest {

    @Test
    void difyExceptionCarriesThirdPartyCode() {
        assertEquals(50_008_001, DifyException.CODE);
        assertEquals(50_008_001, new DifyException().getCode());
        assertEquals(50_008_001,
                new DifyException("{\"status\":400,\"code\":\"invalid_param\",\"message\":\"bad request\"}", 422)
                        .getCode());
    }

    @Test
    void difyClientExceptionCarriesThirdPartyCode() {
        assertEquals(50_008_002, DifyClientException.CODE);
        assertEquals(50_008_002, new DifyClientException().getCode());
        assertEquals(50_008_002, new DifyClientException("timeout").getCode());
        assertEquals(50_008_002, new DifyClientException("timeout", new RuntimeException("io")).getCode());
    }

    @Test
    void knowledgeExceptionCarriesThirdPartyCodeAndKeepsFields() {
        assertEquals(50_008_003, KnowledgeException.CODE);
        assertEquals(50_008_003, new KnowledgeException().getCode());

        KnowledgeException withFields = new KnowledgeException(1001, "invalid authorization");
        assertEquals(50_008_003, withFields.getCode());
        assertEquals(Integer.valueOf(1001), withFields.getError_code());
        assertEquals("invalid authorization", withFields.getError_msg());
    }
}
