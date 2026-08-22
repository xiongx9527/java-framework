package com.xiongx9527.fw.plus.boot.autoconfigure.jdbc.dynamic.datasource.processor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DsHeaderProcessorTest {

    private final DsHeaderProcessor processor = new DsHeaderProcessor();

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        DsRequestContextHolder.clear();
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

    @Test
    void determineDatasourceReadsReactiveHeaderSnapshot() {
        Map<String, List<String>> headers = Collections.singletonMap(
                "DS", Collections.singletonList("tenant_reactive"));
        DsRequestContext context = new DsRequestContext(headers, Collections.<String, Object>emptyMap());

        try (DsRequestContextHolder.Scope ignored = DsRequestContextHolder.open(context)) {
            assertEquals("tenant_reactive", processor.doDetermineDatasource(null, "#header.ds"));
        }
    }

    @Test
    void determineDatasourcePrefersServletHeaderOverReactiveSnapshot() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("ds", "tenant_servlet");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        DsRequestContext context = new DsRequestContext(
                Collections.singletonMap("DS", Collections.singletonList("tenant_reactive")),
                Collections.<String, Object>emptyMap());

        try (DsRequestContextHolder.Scope ignored = DsRequestContextHolder.open(context)) {
            assertEquals("tenant_servlet", processor.doDetermineDatasource(null, "#header.ds"));
        }
    }
}
