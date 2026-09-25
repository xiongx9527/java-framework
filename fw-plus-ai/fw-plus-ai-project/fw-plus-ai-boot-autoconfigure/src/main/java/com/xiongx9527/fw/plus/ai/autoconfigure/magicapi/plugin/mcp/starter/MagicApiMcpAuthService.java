package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.starter;

import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.auth.McpAuthService;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.auth.McpPrincipal;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.auth.McpRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.config.MagicApiMcpProperties;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 插件侧认证：按 {@code magic-api.plugin.mcp.auth.*} 校验 Bearer token 并携带 scope。
 *
 * <p>token 采用 {@code Bearer <token>.<scope1>,<scope2>} 约定（与 Sa-Token OAuth2 资源服务器
 * 的妆验衔接前，先让配置与 scope 裁剪语义在插桩层生效）：scope 以逗号或空格分隔，
 * 逐个按 {@code mcp:tools}/{@code mcp:resources} 等配置键比对。Sa-Token 接入只替换本类，
 * 不改 controller。</p>
 */
public class MagicApiMcpAuthService implements McpAuthService {

	private static final Pattern BEARER = Pattern.compile("^Bearer\\s+(.+)$", Pattern.CASE_INSENSITIVE);

	private final MagicApiMcpProperties.Auth properties;

	public MagicApiMcpAuthService(MagicApiMcpProperties.Auth properties) {
		this.properties = properties;
	}

	@Override
	public McpPrincipal authenticate(McpRequest request) {
		if (properties == null || !properties.isEnabled()) {
			return McpPrincipal.ANONYMOUS;
		}

		String authorization = header(request, "Authorization");
		if (authorization == null) {
			return McpPrincipal.ANONYMOUS;
		}

		Matcher matcher = BEARER.matcher(authorization.trim());
		if (!matcher.matches()) {
			return McpPrincipal.ANONYMOUS;
		}

		String[] parts = matcher.group(1).trim().split("\\.", 2);
		String subject = parts[0].trim();
		if (subject.isEmpty()) {
			return McpPrincipal.ANONYMOUS;
		}
		Set<String> scopes = parts.length > 1 ? parseScopes(parts[1]) : Collections.<String>emptySet();
		return new McpPrincipal(subject, scopes);
	}

	/**
	 * 当前主体是否具备访问某能力的 scope。
	 *
	 * @param principal 主体
	 * @param scope     能力 scope（如 {@code mcp:tools}）
	 * @return 具备返回 {@code true}
	 */
	public boolean hasScope(McpPrincipal principal, String scope) {
		if (principal == null || principal == McpPrincipal.ANONYMOUS) {
			return false;
		}
		if (scope == null || scope.trim().isEmpty()) {
			return true;
		}
		return principal.getScopes().contains(scope);
	}

	/**
	 * 资源能力所需 scope。
	 */
	public String resourcesScope() {
		return properties == null ? null : properties.getResourcesScope();
	}

	/**
	 * 提示词能力所需 scope。
	 */
	public String promptsScope() {
		return properties == null ? null : properties.getPromptsScope();
	}

	/**
	 * 工具能力所需 scope。
	 */
	public String toolsScope() {
		return properties == null ? null : properties.getToolsScope();
	}

	private Set<String> parseScopes(String raw) {
		Set<String> scopes = new LinkedHashSet<>();
		for (String item : raw.split("[,\\s]+")) {
			if (!item.trim().isEmpty()) {
				scopes.add(item.trim());
			}
		}
		return scopes;
	}

	private String header(McpRequest request, String name) {
		if (request == null || request.getHeaders() == null) {
			return null;
		}
		for (Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
			if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
				return entry.getValue();
			}
		}
		return null;
	}
}
