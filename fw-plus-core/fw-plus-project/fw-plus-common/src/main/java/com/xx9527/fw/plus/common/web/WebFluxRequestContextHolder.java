package com.xx9527.fw.plus.common.web;

import cn.hutool.core.util.ObjectUtil;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class WebFluxRequestContextHolder {

    private static final ThreadLocal<ServerWebExchange> serverWebExchangeHolder =
            new NamedThreadLocal<>("ServerWebExchange");

    private WebFluxRequestContextHolder(){

    }

    public static void set(ServerWebExchange exchange) {
        serverWebExchangeHolder.set(exchange);
    }

    public static ServerWebExchange get() {
        return serverWebExchangeHolder.get();
    }


    public static void clear() {
        serverWebExchangeHolder.remove();
    }


    public static WebFluxRequest getRequest() {
        ServerWebExchange serverWebExchange = get();
        if (serverWebExchange != null) {
            return new WebFluxRequest(serverWebExchange);
        }
        return null;
    }

    public static WebFluxResponse getResponse() {
        ServerWebExchange serverWebExchange = get();
        if (serverWebExchange != null) {
            return new WebFluxResponse(serverWebExchange);
        }
        return null;
    }

    public static class WebFluxSession implements WebSession {
        private ServerWebExchange exchange;

        private WebFluxSession(){}

        public WebFluxSession(ServerWebExchange exchange) {
            this.exchange = exchange;
        }
        @Override
        public Object getSession() {
            if (this.exchange != null) {
                this.exchange.getSession();
            }
            return null;
        }

        @Override
        public Object getAttribute(String name) {
            if (this.exchange == null) {
                return null;
            }
            return this.exchange.getSession().subscribe(session -> session.getAttributes().get(name));
        }

        @Override
        public void setAttribute(String key, Object value) {
            if (this.exchange != null) {
                this.exchange.getSession().subscribe(session -> session.getAttributes().put(key, value));
            }
        }
    }


    public  static class WebFluxRequest implements WebRequest {
        private ServerWebExchange exchange;

        private WebFluxRequest(){}

        public WebFluxRequest(ServerWebExchange exchange) {
            this.exchange = exchange;
        }

        @Override
        public Object getRequest() {
            return null==this.exchange?null:this.exchange.getRequest();
        }

        @Override
        public WebSession getSession() {
            if (this.exchange != null) {
                return new WebFluxSession(this.exchange);
            }
            return null;
        }

        @Override
        public String getHeader(String name) {
            if (com.xx9527.fw.plus.common.util.StringUtils.isEmpty(name)) {
                return null;
            }
            if (this.exchange != null) {
                return this.exchange.getRequest().getHeaders().getFirst(name);
            }
            return null;
        }

        @Override
        public String getRemoteAddr() {
            if (this.exchange != null) {
                return this.exchange.getRequest().getRemoteAddress() != null ?
                       this.exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : null;
            }
            return null;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            if (this.exchange != null) {
                return new ByteArrayInputStream(this.exchange.getRequest().getBody()
                        .collectList()
                        .block()
                        .stream()
                        .map(dataBuffer -> {
                            byte[] bytes = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(bytes);
                            DataBufferUtils.release(dataBuffer);
                            return bytes;
                        })
                        .reduce(new byte[0], (a, b) -> {
                            byte[] result = new byte[a.length + b.length];
                            System.arraycopy(a, 0, result, 0, a.length);
                            System.arraycopy(b, 0, result, a.length, b.length);
                            return result;
                        }));
            }
            return null;
        }

        @Override
        public int getContentLength() {
            if (this.exchange != null) {
                // 从请求头中获取 Content-Length
                long contentLength = this.exchange.getRequest().getHeaders().getContentLength();
                if (contentLength > 0) {
                    return (int) contentLength;
                }
                // 如果请求头中没有 Content-Length，则通过读取请求体来计算长度
                List<DataBuffer> dataBuffers = this.exchange.getRequest().getBody().collectList().block();
                if (dataBuffers != null) {
                    return dataBuffers.stream()
                            .mapToInt(DataBuffer::readableByteCount)
                            .sum();
                }
            }
            return 0;
        }

        @Override
        public String getContentType() {
            if (this.exchange != null) {
                return this.exchange.getRequest().getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
            }
            return null;
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            if (this.exchange != null) {
                return Collections.enumeration(new ArrayList<>(this.exchange.getRequest().getHeaders().keySet()));
            }
            return null;
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (this.exchange != null) {
                return Collections.enumeration(this.exchange.getRequest().getHeaders().get(name));
            }
            return null;
        }

        @Override
        public void setAttribute(String key, Object value) {
            if (this.exchange != null) {
                this.exchange.getSession().subscribe(session -> session.getAttributes().put(key, value));
            }
        }

        @Override
        public String getRequestURI() {
            if (this.exchange != null) {
                return this.exchange.getRequest().getURI().getPath();
            }
            return null;
        }

        @Override
        public String getMethod() {
            if (this.exchange != null) {
                return this.exchange.getRequest().getMethod().toString();
            }
            return null;
        }

        @Override
        public String[] getParameterValues(String name) {
            if (this.exchange != null) {
                return new String[]{this.exchange.getRequest().getQueryParams().getFirst(name)};
            }
            return null;
        }

        @Override
        public Object getAttribute(String name) {
            if (this.exchange != null) {
                return this.exchange.getSession().subscribe(session -> session.getAttributes().get(name));
            }
            return null;
        }

        @Override
        public HttpInputMessage getHttpInputMessage() {
            if (this.exchange != null) {
                return new HttpInputMessage() {
                    @Override
                    public InputStream getBody() throws IOException {
                        return getInputStream();
                    }

                    @Override
                    public HttpHeaders getHeaders() {
                        return WebFluxRequest.this.exchange.getRequest().getHeaders();
                    }
                };
            }
            return null;
        }

        @Override
        public WebCookie[] getCookies() {
            if (this.exchange != null) {
                MultiValueMap<String, HttpCookie> cookies = this.exchange.getRequest().getCookies();
                return cookies == null ? new WebFluxCookie[0] : cookies.values().stream().map(e-> new WebFluxCookie((HttpCookie)e) ).toArray((x$0) -> {
                    return new WebFluxCookie[x$0];
                });

            }
            return null;
        }

        @Override
        public WebRequestContextHolder.WebRequestDispatcher getRequestDispatcher(String path) {
            if (this.exchange != null) {
                // 通过 ServerWebExchange 获取请求分发器
                return new WebFluxRequestDispatcher(path);
            }
            return null;
        }


    }

    public  static class WebFluxCookie implements WebCookie {
        private final HttpCookie cookie;

        public WebFluxCookie(HttpCookie cookie) {
            this.cookie = cookie;
        }

        @Override
        public Object getCookie() {
            return this.cookie;
        }
    }

    public  static class WebFluxResponse implements WebResponse {
        private ServerWebExchange exchange;

        private WebFluxResponse(){}

        public WebFluxResponse(ServerWebExchange exchange) {
            this.exchange = exchange;
        }

        @Override
        public Object getResponse() {
            return null==this.exchange?null:this.exchange.getResponse();
        }

        @Override
        public void setStatus(int sc) {
            if (this.exchange != null) {
                this.exchange.getResponse().setStatusCode(HttpStatus.valueOf(sc));
            }
        }

        @Override
        public void setContentType(String contentType) {
            if (this.exchange != null) {
                this.exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE, contentType);
            }
        }

        @Override
        public void setCharacterEncoding(String charset) {
            if (this.exchange != null) {
                if (StringUtils.isEmpty(charset)) {
                    return;
                }
                String contentType = this.exchange.getResponse().getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
                if (StringUtils.isEmpty(contentType) || !contentType.contains("charset")) {
                    if (StringUtils.isNotEmpty(contentType)) {
                        contentType = contentType + ";charset=" + charset;
                    } else {
                        contentType = "charset=" + charset;
                    }
                    this.exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE, contentType);
                }
            }
        }

        @Override
        public void setDateHeader(String name, long date) {
            if (this.exchange != null) {
                this.exchange.getResponse().getHeaders().setDate(name, date);
            }
        }

        @Override
        public void addHeader(String name, String value) {
            if (this.exchange != null) {
                this.exchange.getResponse().getHeaders().add(name, value);
            }
        }

        @Override
        public Object writeWith(String result) throws IOException {
            if (this.exchange != null) {
                return this.exchange.getResponse().writeWith(Mono.just(this.exchange.getResponse()
                        .bufferFactory().wrap(result.getBytes())));
            }
            return Mono.empty();
        }

        @Override
        public void setHeader(String name, String value) {
            if (this.exchange != null) {
                this.exchange.getResponse().getHeaders().set(name, value);
            }
        }
    }

    public static class WebFluxRequestDispatcher implements WebRequestContextHolder.WebRequestDispatcher {

        private String path;

        private WebFluxRequestDispatcher(){}

        public WebFluxRequestDispatcher(String path) {
            this.path = path;
        }

        @Override
        public void forward(WebRequest request, WebResponse response) throws Throwable {
            ServerResponse.ok().render(this.path, new HashMap<>());
        }

        @Override
        public void forward(WebRequest request, WebResponse response,Map<String, ?> model) throws Throwable {
            ServerResponse.ok().render(this.path, MapUtils.emptyIfNull(model));

        }

        @Override
        public void include(WebRequest request, WebResponse response) throws Throwable {
            ServerResponse.ok().render(this.path, new HashMap<>());
        }

        @Override
        public void include(WebRequest request, WebResponse response,Map<String, ?> model) throws Throwable {
            ServerResponse.ok().render(this.path, MapUtils.emptyIfNull(model));
        }
    }

}
