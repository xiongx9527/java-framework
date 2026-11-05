package com.xiongx9527.fw.plus.auth.social;

import cn.hutool.json.JSONObject;

/**
 * 社交/企业登录 Provider 公共基类：提供 URL 编码、空值选择、状态码检查等工具。
 */
public abstract class AbstractSocialLoginProvider implements SocialLoginProvider {

    /**
     * 全局 token 缓存实现。默认内存实现；业务侧通过 {@link #setTokenStore(SocialTokenStore)} 注入 Redis 等分布式实现。
     */
    private static volatile SocialTokenStore TOKEN_STORE = new LocalSocialTokenStore();

    /**
     * 设置全局 token 缓存实现。框架默认使用内存实现 {@link LocalSocialTokenStore}，
     * 业务侧可注入 Redis 等分布式实现以保证多实例共享。
     *
     * @param store 缓存实现，不能为 {@code null}
     */
    public static void setTokenStore(SocialTokenStore store) {
        if (store == null) {
            throw new IllegalArgumentException("SocialTokenStore must not be null");
        }
        TOKEN_STORE = store;
    }

    /**
     * 取得当前 token 缓存实现。
     */
    protected static SocialTokenStore tokenStore() {
        return TOKEN_STORE;
    }


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

    /**
     * 判断响应是否表示 token 失效/无效（用于触发「从缓存拿到失效 token 时，自动从远程重取一次」）。
     * 同时兼容 errcode（企业微信/钉钉）与 code（飞书 open-apis）两种字段。
     *
     * @param resp  远端响应
     * @param codes 代表 token 失效的码集合（如企业微信 40014/41001/42001，飞书 99991663 等）
     */
    protected static boolean isTokenInvalid(JSONObject resp, int... codes) {
        if (resp == null || codes == null) {
            return false;
        }
        Object ec = resp.getByPath("errcode");
        if (ec instanceof Number) {
            int c = ((Number) ec).intValue();
            for (int x : codes) {
                if (x == c) {
                    return true;
                }
            }
        }
        Object c2 = resp.getByPath("code");
        if (c2 instanceof Number) {
            int c = ((Number) c2).intValue();
            for (int x : codes) {
                if (x == c) {
                    return true;
                }
            }
        }
        return false;
    }
}
