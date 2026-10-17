package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.auth;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler.WsHandshakeInterceptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * webmvc 握手认证拦截器：在握手升级前校验凭据，未通过时置 HTTP 401 并返回 {@code false}
 * （不升级、不进入会话注册与订阅派发）。
 *
 * <p>凭据来源为握手 query 参数与请求头，按同一凭据名合并，同名以请求头为准；认证规则由
 * {@link WsAuthSupport} 按握手路径解析出的实例 provider 决定。</p>
 *
 * @author zzy
 */
public class WsAuthHandshakeInterceptor implements WsHandshakeInterceptor {

	private final WsAuthSupport authSupport;

	public WsAuthHandshakeInterceptor(WsAuthSupport authSupport) {
		this.authSupport = authSupport;
	}

	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Map<String, Object> attributes) {
		Map<String, String> headers = new LinkedHashMap<>();
		for (Map.Entry<String, List<String>> entry : request.getHeaders().headerSet()) {
			if (entry.getValue() != null && !entry.getValue().isEmpty()) {
				headers.put(entry.getKey(), entry.getValue().get(0));
			}
		}
		Map<String, String> credentials = WsAuthSupport.mergeCredentials(
				request.getURI() == null ? null : request.getURI().getQuery(), headers);
		String requestPath = request.getURI() == null ? null : request.getURI().getPath();
		if (!authSupport.isAuthenticated(requestPath, credentials)) {
			response.setStatusCode(HttpStatus.UNAUTHORIZED);
			return false;
		}
		return true;
	}

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Exception exception) {
	}
}