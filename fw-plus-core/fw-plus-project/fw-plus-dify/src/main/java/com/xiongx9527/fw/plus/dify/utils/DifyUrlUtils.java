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
        if (isAbsoluteUrl(normalizedRoute)) {
            sb.append(normalizedRoute);
        } else {
            sb.append(normalizedServer);

            if (normalizedRoute.length() > 0) {
                if (!normalizedRoute.startsWith("/")) {
                    sb.append('/');
                }
                sb.append(normalizedRoute);
            }
        }

        appendQuery(sb, query);
        return sb.toString();
    }

    public static String removeQueryAndFragment(String url) {
        if (url == null) {
            return null;
        }
        int queryIndex = url.indexOf('?');
        int fragmentIndex = url.indexOf('#');
        int endIndex = url.length();
        if (queryIndex >= 0) {
            endIndex = queryIndex;
        }
        if (fragmentIndex >= 0 && fragmentIndex < endIndex) {
            endIndex = fragmentIndex;
        }
        return url.substring(0, endIndex);
    }

    private static boolean isAbsoluteUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }

    private static void appendQuery(StringBuilder sb, Map<String, Object> query) {
        if (query == null || query.isEmpty()) {
            return;
        }

        int fragmentIndex = sb.indexOf("#");
        StringBuilder queryBuilder = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Object> entry : query.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            queryBuilder.append(first ? (hasExistingQuery(sb, fragmentIndex) ? '&' : '?') : '&');
            queryBuilder.append(encode(entry.getKey()));
            queryBuilder.append('=');
            queryBuilder.append(encode(String.valueOf(entry.getValue())));
            first = false;
        }
        if (queryBuilder.length() == 0) {
            return;
        }
        if (fragmentIndex >= 0) {
            sb.insert(fragmentIndex, queryBuilder);
        } else {
            sb.append(queryBuilder);
        }
    }

    private static boolean hasExistingQuery(StringBuilder sb, int fragmentIndex) {
        int queryIndex = sb.indexOf("?");
        return queryIndex >= 0 && (fragmentIndex < 0 || queryIndex < fragmentIndex);
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 is not supported", e);
        }
    }
}
