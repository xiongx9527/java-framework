package com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow.dify;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public final class TinyflowDifyExternalRequestBodyFilter extends OncePerRequestFilter {

    private final TinyflowDifyExternalProperties properties;
    private final TinyflowDifyExternalAuditCoordinator audit;

    public TinyflowDifyExternalRequestBodyFilter(TinyflowDifyExternalProperties properties,
            TinyflowDifyExternalAuditCoordinator audit) {
        if (properties == null || audit == null) {
            throw new IllegalArgumentException("Tinyflow Dify external filter dependencies must not be null");
        }
        this.properties = properties;
        this.audit = audit;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equalsIgnoreCase(request.getMethod())
                && (request.getContextPath() + properties.getBasePath() + "/retrieval")
                .equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            jakarta.servlet.FilterChain filterChain) throws ServletException, IOException {
        long maxBytes = properties.getMaxRequestBodyBytes();
        if (request.getContentLengthLong() > maxBytes) {
            reject(response);
            return;
        }

        try {
            filterChain.doFilter(new LimitedBodyRequest(request, maxBytes), response);
        } catch (RequestBodyLimitException error) {
            reject(response);
        }
    }

    private void reject(HttpServletResponse response) throws IOException {
        RuntimeException error = new RequestBodyLimitAuditException();
        audit.failure(null, null, error);
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"error_code\":1003,\"error_msg\":\"Invalid request\"}");
    }

    private static final class LimitedBodyRequest extends HttpServletRequestWrapper {
        private final long maxBytes;
        private ServletInputStream inputStream;

        private LimitedBodyRequest(HttpServletRequest request, long maxBytes) {
            super(request);
            this.maxBytes = maxBytes;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (inputStream == null) {
                inputStream = new LimitedBodyInputStream(super.getInputStream(), maxBytes);
            }
            return inputStream;
        }

        @Override
        public BufferedReader getReader() throws IOException {
            String encoding = getCharacterEncoding();
            Charset charset = encoding == null ? StandardCharsets.ISO_8859_1 : Charset.forName(encoding);
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }
    }

    private static final class LimitedBodyInputStream extends ServletInputStream {
        private final ServletInputStream delegate;
        private final long maxBytes;
        private long count;

        private LimitedBodyInputStream(ServletInputStream delegate, long maxBytes) {
            this.delegate = delegate;
            this.maxBytes = maxBytes;
        }

        @Override
        public int read() throws IOException {
            int value = delegate.read();
            if (value < 0) return -1;
            countBytes(1L);
            return value;
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            if (length == 0) return 0;
            long remaining = maxBytes - count;
            if (remaining <= 0L) {
                probeForOverflow();
                return -1;
            }
            int allowed = (int) Math.min((long) length, remaining);
            int read = delegate.read(bytes, offset, allowed);
            if (read < 0) return -1;
            countBytes(read);
            if (count == maxBytes) {
                probeForOverflow();
            }
            return read;
        }

        private void countBytes(long bytes) throws IOException {
            count += bytes;
            if (count > maxBytes) {
                throw new RequestBodyLimitException();
            }
        }

        private void probeForOverflow() throws IOException {
            int extra = delegate.read();
            if (extra >= 0) {
                countBytes(1L);
            }
        }

        @Override
        public boolean isFinished() {
            return delegate.isFinished();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            delegate.setReadListener(readListener);
        }
    }

    static final class RequestBodyLimitException extends IOException {
        private RequestBodyLimitException() {
            super("Tinyflow Dify external request body exceeds configured limit");
        }
    }

    private static final class RequestBodyLimitAuditException extends RuntimeException {
        private RequestBodyLimitAuditException() {
            super("Tinyflow Dify external request body exceeds configured limit");
        }
    }
}
