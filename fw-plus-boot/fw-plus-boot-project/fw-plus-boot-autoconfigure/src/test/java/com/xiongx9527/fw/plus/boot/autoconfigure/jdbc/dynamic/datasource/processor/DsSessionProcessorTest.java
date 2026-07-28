package com.xiongx9527.fw.plus.boot.autoconfigure.jdbc.dynamic.datasource.processor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;

class DsSessionProcessorTest {

    private final DsSessionProcessor processor = new DsSessionProcessor();

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
        assertNull(processor.doDetermineDatasource(null, "#session.ds"));
    }

    @Test
    void determineDatasourceDoesNotCreateSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertNull(processor.doDetermineDatasource(null, "#session.ds"));
        assertNull(request.getSession(false));
    }

    @Test
    void determineDatasourceReturnsNullForMissingSessionAttribute() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertNull(processor.doDetermineDatasource(null, "#session.ds"));
    }

    @Test
    void determineDatasourceReadsSessionAttributeValue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true).setAttribute("ds", "tenant_b");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertEquals("tenant_b", processor.doDetermineDatasource(null, "#session.ds"));
    }
}
