package com.xiongx9527.fw.plus.dify.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

/**
 * Dify URL 工具。
 */
public final class DifyUrlUtils {

    private static final String UTF_8 = "UTF-8";

    private DifyUrlUtils() {
    }

    public static String normalizeServer(String server) {
        if (server == null) {
            return null;
        }
        String normalized = server.trim();
        while (normalized.endsWith("/") && normalized.length() > "https://".length()) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    public static String buildUrl(String server, String route, Map<String, Object> query) {
        String normalizedServer = normalizeServer(server);
        String normalizedRoute = route == null ? "" : route.trim();
        StringBuilder sb = new StringBuilder();
        sb.append(normalizedServer);

        if (normalizedRoute.length() > 0) {
            if (!normalizedRoute.startsWith("/")) {
                sb.append('/');
            }
            sb.append(normalizedRoute);
        }

        appendQuery(sb, query);
        return sb.toString();
    }

    private static void appendQuery(StringBuilder sb, Map<String, Object> query) {
        if (query == null || query.isEmpty()) {
            return;
        }

        boolean first = true;
        for (Map.Entry<String, Object> entry : query.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            sb.append(first ? '?' : '&');
            sb.append(encode(entry.getKey()));
            sb.append('=');
            sb.append(encode(String.valueOf(entry.getValue())));
            first = false;
        }
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 is not supported", e);
        }
    }
}
