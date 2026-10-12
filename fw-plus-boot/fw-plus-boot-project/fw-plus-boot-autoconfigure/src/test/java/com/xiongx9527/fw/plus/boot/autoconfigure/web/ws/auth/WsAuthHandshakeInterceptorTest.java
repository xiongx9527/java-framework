package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.auth;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * webmvc 握手认证拦截器验收：认证失败时置 HTTP 401 并中止升级；认证通过才继续；
 * query 与请求头凭据合并后交给认证内核（请求头优先）。
 *
 * @author zzy
 */
class WsAuthHandshakeInterceptorTest {

    @Test
    void 认证失败时置401并中止升级() {
        WsAuthSupport support = mock(WsAuthSupport.class);
        when(support.isAuthenticated(any(), any())).thenReturn(false);
        WsAuthHandshakeInterceptor interceptor = new WsAuthHandshakeInterceptor(support);

        ServerHttpRequest request = request("/ws/api?clientId=c1", new HttpHeaders());
        ServerHttpResponse response = mock(ServerHttpResponse.class);

        boolean accepted = interceptor.beforeHandshake(request, response, mock(WebSocketHandler.class), new HashMap<>());

        assertThat(accepted).as("认证失败不升级").isFalse();
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void 认证通过时放行且不改状态码() {
        WsAuthSupport support = mock(WsAuthSupport.class);
        when(support.isAuthenticated(any(), any())).thenReturn(true);
        WsAuthHandshakeInterceptor interceptor = new WsAuthHandshakeInterceptor(support);

        ServerHttpRequest request = request("/ws/api?clientId=c1", new HttpHeaders());
        ServerHttpResponse response = mock(ServerHttpResponse.class);

        boolean accepted = interceptor.beforeHandshake(request, response, mock(WebSocketHandler.class), new HashMap<>());

        assertThat(accepted).isTrue();
        verify(response, never()).setStatusCode(any());
    }

    @Test
    void query与header凭据合并后交给内核且header优先() {
        WsAuthSupport support = mock(WsAuthSupport.class);
        when(support.isAuthenticated(any(), any())).thenReturn(true);
        WsAuthHandshakeInterceptor interceptor = new WsAuthHandshakeInterceptor(support);

        HttpHeaders headers = new HttpHeaders();
        headers.add("satoken", "from-header");
        ServerHttpRequest request = request("/ws/api?clientId=c1&satoken=from-query&apikey=qk", headers);

        interceptor.beforeHandshake(request, mock(ServerHttpResponse.class), mock(WebSocketHandler.class),
                new HashMap<>());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(support).isAuthenticated(eq("/ws/api"), captor.capture());
        assertThat(captor.getValue())
                .containsEntry("clientId", "c1")
                .containsEntry("apikey", "qk")
                .containsEntry("satoken", "from-header");
    }

    private static ServerHttpRequest request(String uri, HttpHeaders headers) {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(request.getURI()).thenReturn(URI.create(uri));
        when(request.getHeaders()).thenReturn(headers);
        return request;
    }
}