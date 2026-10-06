package com.xiongx9527.fw.plus.auth.core;

import java.util.Map;

/**
 * 请求凭据读取辅助：header 名大小写不敏感，Bearer 凭据解析。
 */
public final class AuthHeaderExtractor {

    private AuthHeaderExtractor() {
    }

    /**
     * 大小写不敏感地读取 header 值。
     *
     * @param request 请求上下文
     * @param name header 名
     * @return 去除首尾空白后的值；缺失或空白返回 {@code null}
     */
    public static String header(AuthRequest request, String name) {
        if (request == null || name == null || name.trim().isEmpty()) {
            return null;
        }
        for (Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
            if (entry.getKey() != null && name.trim().equalsIgnoreCase(entry.getKey())) {
                String value = entry.getValue();
                return value == null || value.trim().isEmpty() ? null : value.trim();
            }
        }
        return null;
    }

    /**
     * 读取 {@code Authorization: Bearer <token>}。
     *
     * @return token；scheme 非 Bearer 或缺失时返回 {@code null}
     */
    public static String bearerToken(AuthRequest request) {
        String authorization = header(request, "Authorization");
        if (authorization == null) {
            return null;
        }
        int separator = authorization.indexOf(' ');
        if (separator < 0 || !"bearer".equalsIgnoreCase(authorization.substring(0, separator))) {
            return null;
        }
        String token = authorization.substring(separator + 1).trim();
        return token.isEmpty() ? null : token;
    }

    /**
     * 读取 Authorization 原始凭据：去掉 Bearer/Basic scheme 与 Basic 凭据尾部的 {@code ':'}，
     * 供 API Key 等不区分 scheme 的场景使用。
     *
     * @return 凭据值；缺失时返回 {@code null}
     */
    public static String authorizationValue(AuthRequest request) {
        String authorization = header(request, "Authorization");
        if (authorization == null) {
            return null;
        }
        int separator = authorization.indexOf(' ');
        if (separator > 0) {
            String scheme = authorization.substring(0, separator);
            if ("bearer".equalsIgnoreCase(scheme) || "basic".equalsIgnoreCase(scheme)) {
                authorization = authorization.substring(separator + 1).trim();
            }
        }
        if (authorization.endsWith(":")) {
            authorization = authorization.substring(0, authorization.length() - 1).trim();
        }
        return authorization.isEmpty() ? null : authorization;
    }
}