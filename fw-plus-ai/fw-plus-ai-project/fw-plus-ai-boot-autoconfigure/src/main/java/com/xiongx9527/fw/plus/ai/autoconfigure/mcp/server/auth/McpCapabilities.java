package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.auth;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * capability scope 判定：把 JSON-RPC 方法映射到 MCP 能力（tools/resources/prompts），
 * 并判断已认证主体携带的 token scopes 是否覆盖该能力。
 *
 * <p>只在开启 scope 裁剪（{@code fw-plus.ai.mcp.scope-check-enabled=true}）时参与判定；
 * 未开启、非能力操作（initialize 等）或主体未携带任何 scope 信息时一律放行。</p>
 */
public final class McpCapabilities {

	private static final String CAPABILITY_TOOLS = "tools";

	private static final String CAPABILITY_RESOURCES = "resources";

	private static final String CAPABILITY_PROMPTS = "prompts";

	private McpCapabilities() {
	}

	/**
	 * JSON-RPC 方法前缀 → capability 名；{@code tools/list}、{@code tools/call} → {@code tools}。
	 * 非能力操作（initialize、completion/complete、ping 等）返回 {@code null}，表示不参与裁剪。
	 *
	 * @param method JSON-RPC method
	 * @return capability 名，不适用时 {@code null}
	 */
	public static String ofMethod(String method) {
		if (!StringUtils.hasText(method)) {
			return null;
		}
		if (method.startsWith(CAPABILITY_TOOLS + "/")) {
			return CAPABILITY_TOOLS;
		}
		if (method.startsWith(CAPABILITY_RESOURCES + "/")) {
			return CAPABILITY_RESOURCES;
		}
		if (method.startsWith(CAPABILITY_PROMPTS + "/")) {
			return CAPABILITY_PROMPTS;
		}
		return null;
	}

	/**
	 * 从 JSON-RPC 请求体提取 method；body 为空或解析失败时返回 {@code null}。
	 *
	 * @param body JSON-RPC 请求体
	 * @return method，不可得时 {@code null}
	 */
	public static String methodOfBody(String body) {
		if (!StringUtils.hasText(body)) {
			return null;
		}
		try {
			JSONObject json = JSON.parseObject(body);
			return json == null ? null : json.getString("method");
		} catch (Exception ignored) {
			return null;
		}
	}

	/**
	 * 请求体对应的 capability；不可得时 {@code null}。
	 *
	 * @param body JSON-RPC 请求体
	 * @return capability 名，不适用时 {@code null}
	 */
	public static String ofBody(String body) {
		return ofMethod(methodOfBody(body));
	}

	/**
	 * 主体 scopes 是否覆盖目标 capability。
	 *
	 * <p>匹配规则：scope 与 capability 名相等（忽略大小写），或以冒号分段的任一段等于
	 * capability 名（兼容 {@code mcp:tools} 这类带前缀的 scope 形态）。主体未携带任何 scope
	 * 时视为未颁发 scope 信息，放行——不因凭证缺少 scope 声明而破坏既有部署。</p>
	 *
	 * @param principal  已认证主体
	 * @param capability 目标能力，{@code null} 表示不适用
	 * @return 覆盖返回 {@code true}
	 */
	public static boolean covers(McpPrincipal principal, String capability) {
		if (capability == null || principal == null) {
			return true;
		}
		Set<String> scopes = principal.getScopes();
		if (scopes == null || scopes.isEmpty()) {
			return true;
		}
		for (String scope : scopes) {
			if (matchesScope(scope, capability)) {
				return true;
			}
		}
		return false;
	}

	private static boolean matchesScope(String scope, String capability) {
		if (!StringUtils.hasText(scope)) {
			return false;
		}
		if (scope.equalsIgnoreCase(capability)) {
			return true;
		}
		for (String segment : scope.split(":")) {
			if (segment.equalsIgnoreCase(capability)) {
				return true;
			}
		}
		return false;
	}
}
