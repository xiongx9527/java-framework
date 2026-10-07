package com.xiongx9527.fw.plus.auth.core.config;

import lombok.Data;

/**
 * 实例级认证配置绑定，仅含 {@code provider} 字段。
 * <p>
 * 各能力（MCP/WS/Knowledge）在各自配置前缀下复用此结构，例如：
 * {@code fw-plus.ai.mcp.auth}、{@code fw.plus.ws.servers[].auth}、
 * {@code magic-api.plugin.mcp.servers[].auth} 等。
 * </p>
 */
@Data
public class InstanceAuthProperties {

    /**
     * 认证 provider：{@code permit-all}、{@code sa-token-login}、{@code sa-token-oauth2}、
     * {@code sa-token-jwt}、{@code sa-token-apikey}、{@code sa-token-sso}。
     * <p>
     * 未配置或为空表示该实例不启用认证（请求一律 401/握手拒绝）。
     * 对应模块缺失时该 provider 不可选，配置了也会 fail-closed 拒绝。
     * </p>
     */
    private String provider;
}