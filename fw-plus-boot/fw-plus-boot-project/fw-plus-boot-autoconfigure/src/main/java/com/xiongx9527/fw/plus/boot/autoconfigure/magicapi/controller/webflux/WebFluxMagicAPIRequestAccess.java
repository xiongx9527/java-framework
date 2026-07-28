package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.controller.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.controller.MagicAPIRequestAccess;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.server.ServerWebExchange;
import org.ssssssss.magicapi.core.config.Constants;
import org.ssssssss.magicapi.core.context.MagicUser;
import org.ssssssss.magicapi.core.servlet.MagicCookie;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest;
import org.ssssssss.magicapi.core.servlet.MagicHttpSession;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.Principal;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * 为管理接口授权提供 WebFlux 请求视图，不参与动态 API 运行时请求处理。
 */
public class WebFluxMagicAPIRequestAccess implements MagicAPIRequestAccess, MagicHttpServletRequest {

    private final ServerWebExchange exchange;
    private final byte[] body;

    public WebFluxMagicAPIRequestAccess(ServerWebExchange exchange) {
        this(exchange, new byte[0]);
    }

    public WebFluxMagicAPIRequestAccess(ServerWebExchange exchange, byte[] body) {
        this.exchange = exchange;
        this.body = body == null ? new byte[0] : body;
    }

    @Override
    public MagicUser getMagicUser() {
        return exchange.getAttribute(Constants.ATTRIBUTE_MAGIC_USER);
    }

    @Override
    public MagicHttpServletRequest asServletRequest() {
        return this;
    }

    @Override
    public String getHeader(String name) {
        return exchange.getRequest().getHeaders().getFirst(name);
    }

	@Override
	public Enumeration<String> getHeaders(String name) {
		List<String> values = exchange.getRequest().getHeaders().get(name);
		return Collections.enumeration(values == null ? Collections.<String>emptyList() : values);
	}

    @Override
    public String getRequestURI() {
        return exchange.getRequest().getPath().pathWithinApplication().value();
    }

    @Override
    public String getMethod() {
        return exchange.getRequest().getMethod().name();
    }

    @Override
    public void setAttribute(String name, Object value) {
        exchange.getAttributes().put(name, value);
    }

    @Override
    public String[] getParameterValues(String name) {
        java.util.List<String> values = exchange.getRequest().getQueryParams().get(name);
        return values == null ? null : values.toArray(new String[values.size()]);
    }

    @Override
    public Object getAttribute(String name) {
        return exchange.getAttribute(name);
    }

	@Override
	public HttpInputMessage getHttpInputMessage() {
		return new HttpInputMessage() {
			@Override
			public InputStream getBody() throws IOException {
				return new ByteArrayInputStream(body);
			}

			@Override
			public HttpHeaders getHeaders() {
				return exchange.getRequest().getHeaders();
			}
		};
	}

    @Override
    public String getContentType() {
        MediaType contentType = exchange.getRequest().getHeaders().getContentType();
        return contentType == null ? null : contentType.toString();
    }

    @Override
    public MagicHttpSession getSession() {
        return new MagicHttpSession() {
            @Override
            public Object getAttribute(String key) {
                return exchange.getAttribute(key);
            }

            @Override
            public void setAttribute(String key, Object value) {
                exchange.getAttributes().put(key, value);
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T> T getSession() {
                return (T) exchange;
            }
        };
    }

    @Override
    public MagicCookie[] getCookies() {
        return exchange.getRequest().getCookies().values().stream()
                .flatMap(List::stream)
                .map(WebFluxMagicCookie::new)
                .toArray(MagicCookie[]::new);
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(body);
    }

    @Override
    public boolean isMultipart() {
        MediaType contentType = exchange.getRequest().getHeaders().getContentType();
        return contentType != null && MediaType.MULTIPART_FORM_DATA.isCompatibleWith(contentType);
    }

    @Override
    public String getRemoteAddr() {
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress == null) {
            return null;
        }
        return remoteAddress.getAddress() == null ? remoteAddress.getHostString()
                : remoteAddress.getAddress().getHostAddress();
    }

    @Override
    public MultipartRequest resolveMultipart() {
        throw new UnsupportedOperationException("MultipartRequest is not available in WebFlux");
    }

    @Override
    public Principal getUserPrincipal() {
        throw new UnsupportedOperationException("Synchronous Principal access is not available in WebFlux");
    }

    @Override
    public <T> T getRequest() {
        throw new UnsupportedOperationException("Servlet request access is not available in WebFlux");
    }

    private static final class WebFluxMagicCookie implements MagicCookie {

        private final HttpCookie cookie;

        private WebFluxMagicCookie(HttpCookie cookie) {
            this.cookie = cookie;
        }

        @Override
        public String getName() {
            return cookie.getName();
        }

        @Override
        public String getValue() {
            return cookie.getValue();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getCookie() {
            return (T) cookie;
        }
    }
}
