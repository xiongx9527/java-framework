package com.xiongx9527.fw.plus.auth.social;

import java.util.Map;

import cn.hutool.core.map.MapUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;

/**
 * 零额外依赖的 HTTP / JSON 工具（基于 Hutool，随 fw-plus-common 传递）。
 */
public final class SocialHttpUtils {

    private SocialHttpUtils() {
    }

    public static String get(String url, Map<String, String> headers) {
        HttpRequest request = HttpRequest.get(url);
        if (MapUtil.isNotEmpty(headers)) {
            request.headerMap(headers, true);
        }
        return request.execute().body();
    }

    public static String postForm(String url, Map<String, String> form, Map<String, String> headers) {
        HttpRequest request = HttpRequest.post(url);
        if (form != null) {
            for (Map.Entry<String, String> entry : form.entrySet()) {
                request.form(entry.getKey(), entry.getValue());
            }
        }
        if (MapUtil.isNotEmpty(headers)) {
            request.headerMap(headers, true);
        }
        return request.execute().body();
    }

    public static String postJson(String url, String jsonBody, Map<String, String> headers) {
        HttpRequest request = HttpRequest.post(url).body(jsonBody).contentType("application/json");
        if (MapUtil.isNotEmpty(headers)) {
            request.headerMap(headers, true);
        }
        return request.execute().body();
    }

    public static <T> T parseJson(String body, Class<T> clazz) {
        return JSONUtil.toBean(body, clazz);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseJsonMap(String body) {
        return JSONUtil.toBean(body, Map.class);
    }
}
