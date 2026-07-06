package com.xiongx9527.fw.plus.boot.autoconfigure.jdbc.dynamic.datasource.processor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;

class DsHeaderProcessorTest {

    private final DsHeaderProcessor processor = new DsHeaderProcessor();

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void matchesReturnsFalseForNullKey() {
        assertFalse(processor.matches(null));
    }

    @Test
    void determineDatasourceReturnsNullOutsideWebRequest() {
        assertNull(processor.doDetermineDatasource(null, "#header.ds"));
    }

    @Test
    void determineDatasourceReturnsNullForEmptyHeaderName() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        assertNull(processor.doDetermineDatasource(null, "#header"));
    }

    @Test
    void determineDatasourceReadsHeaderValue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("ds", "tenant_a");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertEquals("tenant_a", processor.doDetermineDatasource(null, "#header.ds"));
    }
}
