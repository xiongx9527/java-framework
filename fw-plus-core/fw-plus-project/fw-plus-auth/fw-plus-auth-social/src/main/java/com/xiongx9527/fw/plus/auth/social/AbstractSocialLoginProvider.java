package com.xiongx9527.fw.plus.auth.social;

import cn.hutool.json.JSONObject;

/**
 * 社交/企业登录 Provider 公共基类：提供 URL 编码、空值选择、状态码检查等工具。
 */
public abstract class AbstractSocialLoginProvider implements SocialLoginProvider {

    protected static String urlEncode(String value) {
        if (value == null) {
            return "";
        }
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return value;
        }
    }

    protected static String first(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) {
                return v;
            }
        }
        return null;
    }

    protected static String base(IdpConfig config, String defaultBase) {
        String apiBase = config.extra("api_base");
        return apiBase == null ? defaultBase : apiBase;
    }

    /**
     * 通用状态码检查：检测常见错误标记（errcode != 0、ResultCode = FAILED）时抛错。
     * 仅对显式错误码抛错，避免误判业务字段。
     */
    protected static void checkResponse(JSONObject obj, String operation) {
        if (obj == null) {
            throw new IllegalStateException(operation + " failed: empty response");
        }
        Object errcode = obj.getByPath("errcode");
        if (errcode instanceof Number && ((Number) errcode).intValue() != 0) {
            throw new IllegalStateException(operation + " failed: " + obj);
        }
        Object resultCode = obj.getByPath("ResultCode");
        if (resultCode instanceof String && "FAILED".equals(resultCode)) {
            throw new IllegalStateException(operation + " failed: " + obj);
        }
    }
}
