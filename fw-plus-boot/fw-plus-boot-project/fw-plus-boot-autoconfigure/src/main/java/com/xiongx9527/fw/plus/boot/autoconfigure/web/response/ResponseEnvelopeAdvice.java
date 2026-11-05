package com.xiongx9527.fw.plus.boot.autoconfigure.web.response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.servlet.http.HttpServletResponse;

/**
 * 统一响应信封兜底切面：把 Controller 的原始返回值包装成 {@code {code,data,message}}。
 *
 * <p>不包装的情形（逐条显式排除，避免破坏既有契约）：</p>
 * <ul>
 *   <li>返回 {@code null} 或 {@link String}（String 走 StringHttpMessageConverter，套 Map 会 ClassCastException）；</li>
 *   <li>非 JSON 响应（{@code text/event-stream} 流式、文件下载、二进制）；</li>
 *   <li>已带 {@code Content-Disposition: attachment} 的响应；</li>
 *   <li>已经是信封结构（含 code 与 data 键）的响应，避免二次包装。</li>
 * </ul>
 *
 * <p>业务侧已显式返回 {@code JsonResult} 的路径会被识别为「已包装」而原样透传。</p>
 */
@RestControllerAdvice
public class ResponseEnvelopeAdvice implements ResponseBodyAdvice<Object> {

    private static final Logger log = LoggerFactory.getLogger(ResponseEnvelopeAdvice.class);

    private final ResponseProperties properties;

    private final ResponseEnvelopeFactory factory;

    public ResponseEnvelopeAdvice(ResponseProperties properties, ResponseEnvelopeFactory factory) {
        this.properties = properties;
        this.factory = factory;
    }

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        if (!properties.isEnabled()) {
            return false;
        }
        if (!MappingJackson2HttpMessageConverter.class.isAssignableFrom(converterType)) {
            return false;
        }
        return inBasePackages(returnType);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> converterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body == null || body instanceof String || body instanceof byte[]) {
            return body;
        }
        if (selectedContentType != null && !isJson(selectedContentType)) {
            return body;
        }
        if (isAttachment(response)) {
            return body;
        }
        if (factory.isWrapped(body)) {
            return body;
        }
        log.debug("wrap response envelope for {}", returnType.getContainingClass().getName());
        return factory.success(body);
    }

    private boolean inBasePackages(MethodParameter returnType) {
        String[] packages = properties.getBasePackages();
        if (packages == null || packages.length == 0) {
            return true;
        }
        Class<?> declaring = returnType.getContainingClass();
        if (declaring == null) {
            return true;
        }
        String name = declaring.getName();
        for (String prefix : packages) {
            if (StringUtils.hasText(prefix) && name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isJson(MediaType mediaType) {
        return MediaType.APPLICATION_JSON.includes(mediaType)
                || (mediaType.getSubtype() != null && mediaType.getSubtype().endsWith("+json"));
    }

    private static boolean isAttachment(ServerHttpResponse response) {
        if (response instanceof ServletServerHttpResponse) {
            HttpServletResponse servletResponse = ((ServletServerHttpResponse) response).getServletResponse();
            String disposition = servletResponse == null ? null : servletResponse.getHeader("Content-Disposition");
            return disposition != null && disposition.toLowerCase().contains("attachment");
        }
        return false;
    }
}
