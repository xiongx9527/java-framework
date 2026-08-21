package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.filter;

import org.springframework.core.Ordered;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class StaticResourceFilter extends StaticResourceBaseFilter implements Filter, Ordered {

    private static final Map<String, String> MIME_TYPES = new HashMap<>();
    static {
        MIME_TYPES.put("js", "text/javascript");
        MIME_TYPES.put("css", "text/css");
        MIME_TYPES.put("html", "text/html");
        MIME_TYPES.put("xml", "application/xml");
        MIME_TYPES.put("ttf", "font/ttf");
        MIME_TYPES.put("woff", "font/woff");
        MIME_TYPES.put("woff2", "font/woff2");
        MIME_TYPES.put("eot", "application/vnd.ms-fontobject");
        MIME_TYPES.put("otf", "font/otf");
    }

    @Override
    void writeScript(Object response, String suffix, String script) {
        HttpServletResponse res = (HttpServletResponse) response;
        String contentType = MIME_TYPES.getOrDefault(suffix, "text/plain");
        res.setContentType(contentType + ";charset=UTF-8");
        res.setHeader("Cache-Control", "max-age=3600, public");
        try {
            res.getWriter().write(script);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest)) {
            throw new IllegalArgumentException("Only HTTP requests are supported");
        }

        if (writeScript(((HttpServletRequest) request).getRequestURI(), response)) {
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }
}