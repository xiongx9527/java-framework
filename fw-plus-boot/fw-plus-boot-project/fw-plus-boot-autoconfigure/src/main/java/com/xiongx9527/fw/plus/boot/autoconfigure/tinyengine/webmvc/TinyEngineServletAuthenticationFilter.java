package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineCredentialExtractor;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineAccessDeniedException;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineCredential;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEnginePrincipal;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineUnauthenticatedException;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineUserSystemService;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineRequestUserScope;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class TinyEngineServletAuthenticationFilter extends OncePerRequestFilter {

	private static final String APPLICATION_JSON = "application/json;charset=UTF-8";

	private final TinyEngineCredentialExtractor credentialExtractor;
	private final TinyEngineUserSystemService userSystemService;
	private final TinyEngineRequestUserScope userScope;

	public TinyEngineServletAuthenticationFilter(TinyEngineCredentialExtractor credentialExtractor,
			TinyEngineUserSystemService userSystemService, TinyEngineRequestUserScope userScope) {
		this.credentialExtractor = credentialExtractor;
		this.userSystemService = userSystemService;
		this.userScope = userScope;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (!requiresAuthentication(request)) {
			filterChain.doFilter(request, response);
			return;
		}
		try {
			TinyEngineCredential credential = credentialExtractor.extract(request.getHeader("Authorization"),
					tenantId(request));
			TinyEnginePrincipal principal = userSystemService.currentUser(credential);
			userScope.callAs(principal, () -> {
				filterChain.doFilter(request, response);
				return null;
			});
		} catch (Throwable ex) {
			writeError(response, ex);
		}
	}

	private boolean requiresAuthentication(HttpServletRequest request) {
		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			return false;
		}
		String path = requestPath(request);
		if (isAnonymousUserApi(path)) {
			return false;
		}
		return containsApiPrefix(path, "/app-center/api/")
				|| containsApiPrefix(path, "/app-center/v1/api/")
				|| containsApiPrefix(path, "/material-center/api/")
				|| containsApiPrefix(path, "/material-center/v1/api/")
				|| containsApiPrefix(path, "/platform-center/api/")
				|| containsApiPrefix(path, "/platform-center/v1/api/");
	}

	private String requestPath(HttpServletRequest request) {
		String uri = request.getRequestURI();
		String contextPath = request.getContextPath();
		if (contextPath != null && contextPath.length() > 0 && uri.startsWith(contextPath)) {
			return uri.substring(contextPath.length());
		}
		return uri;
	}

	private boolean isAnonymousUserApi(String path) {
		return path.endsWith("/platform-center/api/user/login")
				|| path.endsWith("/platform-center/api/user/register")
				|| path.endsWith("/platform-center/api/user/forgot-password");
	}

	private boolean containsApiPrefix(String path, String apiPrefix) {
		return path.startsWith(apiPrefix) || path.contains(apiPrefix);
	}

	private String tenantId(HttpServletRequest request) {
		String tenantId = request.getHeader("X-Tenant-Id");
		if (tenantId == null || tenantId.trim().length() == 0) {
			tenantId = request.getParameter("tenantId");
		}
		if (tenantId == null || tenantId.trim().length() == 0) {
			tenantId = request.getParameter("tenant");
		}
		return tenantId;
	}

	private void writeError(HttpServletResponse response, Throwable ex) throws IOException {
		int status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
		String code = "500";
		if (ex instanceof TinyEngineUnauthenticatedException) {
			status = HttpServletResponse.SC_UNAUTHORIZED;
			code = "401";
		} else if (ex instanceof TinyEngineAccessDeniedException) {
			status = HttpServletResponse.SC_FORBIDDEN;
			code = "403";
		}
		String message = ex.getMessage() == null ? "" : ex.getMessage();
		response.setStatus(status);
		response.setContentType(APPLICATION_JSON);
		response.getWriter().write("{\"data\":null,\"code\":\"" + code + "\",\"message\":\""
				+ escapeJson(message) + "\",\"success\":false,\"error\":{\"code\":\"" + code
				+ "\",\"message\":\"" + escapeJson(message) + "\"},\"errMsg\":\"" + escapeJson(message) + "\"}");
	}

	private String escapeJson(String value) {
		StringBuilder builder = new StringBuilder(value.length());
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (c == '"' || c == '\\') {
				builder.append('\\').append(c);
			} else if (c == '\n') {
				builder.append("\\n");
			} else if (c == '\r') {
				builder.append("\\r");
			} else if (c == '\t') {
				builder.append("\\t");
			} else {
				builder.append(c);
			}
		}
		return builder.toString();
	}
}
