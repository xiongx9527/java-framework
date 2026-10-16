package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.auth;

import com.xiongx9527.fw.plus.auth.core.ScopeCapabilities;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * MCP capability scope 判定：委托 {@link ScopeCapabilities} 核心实现，
 * 仅保留 MCP 专用的常量与 {@code ofMethod/ofBody} 便利方法。
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
     * 主体 scopes 是否覆盖目标 capability —— 委托核心实现。
     */
    public static boolean covers(McpPrincipal principal, String capability) {
        if (principal == null) {
            return false;
        }
        // 转换为核心主体：PERMIT_ALL 等特殊常量需要转换为核心对应的常量实例
        com.xiongx9527.fw.plus.auth.core.AuthPrincipal corePrincipal;
        if (principal == McpPrincipal.PERMIT_ALL) {
            corePrincipal = com.xiongx9527.fw.plus.auth.core.AuthPrincipal.PERMIT_ALL;
        } else if (principal == McpPrincipal.ANONYMOUS) {
            corePrincipal = com.xiongx9527.fw.plus.auth.core.AuthPrincipal.ANONYMOUS;
        } else {
            corePrincipal = new com.xiongx9527.fw.plus.auth.core.AuthPrincipal(principal.getSubject(), principal.getScopes());
        }
        return ScopeCapabilities.covers(corePrincipal, capability);
    }
}