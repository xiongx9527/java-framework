package com.xiongx9527.fw.plus.auth.core;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.auth.core.util.StringUtils;

import java.util.Set;

/**
 * capability scope 判定：把方法名映射到能力，并判断主体携带的 scopes 是否覆盖该能力。
 * <p>
 * 只在开启 scope 裁剪时参与判定；未开启或非能力操作时不执行 scope 限制；
 * 启用时，缺少目标 capability scope（包括主体没有任何 scope）不授权。
 * </p>
 */
public final class ScopeCapabilities {

    private static final String CAPABILITY_TOOLS = "tools";
    private static final String CAPABILITY_RESOURCES = "resources";
    private static final String CAPABILITY_PROMPTS = "prompts";

    private ScopeCapabilities() {
    }

    /**
     * 方法前缀 → capability 名；{@code tools/list}、{@code tools/call} → {@code tools}。
     * 非能力操作（initialize、completion/complete、ping 等）返回 {@code null}，表示不参与裁剪。
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
     */
    public static String ofBody(String body) {
        return ofMethod(methodOfBody(body));
    }

    /**
     * 主体 scopes 是否覆盖目标 capability。
     * <p>
     * 匹配规则：显式 PermitAll 主体（{@code provider=permit-all}）恒绕过 capability scope
     * 检查；scope 与 capability 名相等（忽略大小写），或以冒号分段的任一段等于
     * capability 名（兼容 {@code mcp:tools} 这类带前缀的 scope 形态）。主体未携带任何 scope
     * 时不覆盖目标 capability。
     * </p>
     */
    public static boolean covers(AuthPrincipal principal, String capability) {
        if (capability == null) {
            return true;
        }
        if (principal == null) {
            return false;
        }
        if (principal == AuthPrincipal.PERMIT_ALL) {
            // 显式选择 permit-all 的实例绕过 scope 裁剪
            return true;
        }
        Set<String> scopes = principal.getScopes();
        if (scopes == null || scopes.isEmpty()) {
            return false;
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