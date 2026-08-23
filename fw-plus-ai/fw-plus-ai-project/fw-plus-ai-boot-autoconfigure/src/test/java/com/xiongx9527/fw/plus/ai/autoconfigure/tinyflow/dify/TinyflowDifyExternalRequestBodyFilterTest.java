package com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow.dify;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TinyflowDifyExternalRequestBodyFilterTest {

    private TinyflowDifyExternalProperties properties;
    private List<RuntimeException> audits;
    private TinyflowDifyExternalRequestBodyFilter filter;

    @Before
    public void setUp() {
        properties = new TinyflowDifyExternalProperties();
        audits = new ArrayList<RuntimeException>();
        filter = new TinyflowDifyExternalRequestBodyFilter(properties,
                new TinyflowDifyExternalAuditCoordinator(event -> audits.add(new RuntimeException(event.getErrorType()))));
    }

    @Test
    public void shouldRejectKnownOversizedRetrievalBeforeFilterChain() throws Exception {
        properties.setMaxRequestBodyBytes(8);
        MockHttpServletRequest request = request("POST", "/external-knowledge/retrieval", "123456789");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicInteger calls = new AtomicInteger();

        filter.doFilter(request, response, (req, res) -> calls.incrementAndGet());

        assertEquals(400, response.getStatus());
        assertEquals(0, calls.get());
        assertTrue(response.getContentAsString().contains("\"error_code\":1003"));
        assertEquals(1, audits.size());
    }

    @Test
    public void shouldIgnoreNonTargetRequest() throws Exception {
        MockHttpServletRequest request = request("POST", "/other", repeat("x", 100));
        AtomicInteger calls = new AtomicInteger();

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> calls.incrementAndGet());

        assertEquals(1, calls.get());
        assertEquals(0, audits.size());
    }

    @Test
    public void shouldIgnoreNonPostRetrieval() throws Exception {
        properties.setMaxRequestBodyBytes(8);
        MockHttpServletRequest request = request("GET", "/external-knowledge/retrieval", "123456789");
        AtomicInteger calls = new AtomicInteger();

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> calls.incrementAndGet());

        assertEquals(1, calls.get());
        assertEquals(0, audits.size());
    }

    @Test
    public void shouldMatchRetrievalWithContextPath() throws Exception {
        properties.setMaxRequestBodyBytes(8);
        MockHttpServletRequest request = request("POST", "/app/external-knowledge/retrieval", "123456789");
        request.setContextPath("/app");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> fail("must reject before chain"));

        assertEquals(400, response.getStatus());
        assertEquals(1, audits.size());
    }

    @Test
    public void shouldStopUnknownLengthBodyAtLimitPlusOneByte() throws Exception {
        properties.setMaxRequestBodyBytes(8);
        HttpServletRequest request = unknownLength(request("POST", "/external-knowledge/retrieval", "123456789"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> drain(req.getInputStream()));

        assertEquals(400, response.getStatus());
        assertEquals(1, audits.size());
    }

    @Test
    public void shouldAllowBodyExactlyAtByteLimit() throws Exception {
        properties.setMaxRequestBodyBytes(8);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicInteger calls = new AtomicInteger();

        filter.doFilter(request("POST", "/external-knowledge/retrieval", "12345678"), response,
                (req, res) -> {
                    drain(req.getInputStream());
                    calls.incrementAndGet();
                });

        assertEquals(1, calls.get());
        assertEquals(0, audits.size());
        assertFalse(response.isCommitted());
    }

    @Test
    public void shouldCountUtf8BytesInsteadOfCharacters() throws Exception {
        properties.setMaxRequestBodyBytes(5);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("POST", "/external-knowledge/retrieval", "中文"), response,
                (req, res) -> drain(req.getInputStream()));

        assertEquals(400, response.getStatus());
        assertEquals(1, audits.size());
    }

    @Test
    public void shouldRejectOversizedBodyReadThroughReader() throws Exception {
        properties.setMaxRequestBodyBytes(8);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(unknownLength(request("POST", "/external-knowledge/retrieval", "123456789")), response,
                (req, res) -> {
                    BufferedReader reader = req.getReader();
                    while (reader.read() != -1) {
                        // consume through the MVC reader path
                    }
                });

        assertEquals(400, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"error_code\":1003"));
        assertEquals(1, audits.size());
    }

    @Test
    public void shouldAuditOnceWithoutRewritingCommittedResponse() throws Exception {
        properties.setMaxRequestBodyBytes(8);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(unknownLength(request("POST", "/external-knowledge/retrieval", "123456789")), response,
                (req, res) -> {
                    res.getWriter().write("already committed");
                    res.flushBuffer();
                    drain(req.getInputStream());
                });

        assertEquals(1, audits.size());
        assertEquals("already committed", response.getContentAsString());
    }

    private MockHttpServletRequest request(String method, String uri, String body) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }

    private HttpServletRequest unknownLength(MockHttpServletRequest request) {
        return new HttpServletRequestWrapper(request) {
            @Override
            public int getContentLength() {
                return -1;
            }

            @Override
            public long getContentLengthLong() {
                return -1L;
            }
        };
    }

    private int drain(ServletInputStream input) throws IOException {
        int total = 0;
        byte[] buffer = new byte[32];
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
        }
        return total;
    }

    private String repeat(String value, int count) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < count; i++) result.append(value);
        return result.toString();
    }
}
