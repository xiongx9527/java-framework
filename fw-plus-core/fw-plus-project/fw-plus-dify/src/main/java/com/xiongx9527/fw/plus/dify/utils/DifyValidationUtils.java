package com.xiongx9527.fw.plus.dify.utils;

import com.xiongx9527.fw.plus.dify.types.DifyClientException;

import java.net.URI;

/**
 * Dify 参数校验工具。
 */
public final class DifyValidationUtils {

    private DifyValidationUtils() {
    }

    public static String requireText(String value, String fieldName, String clientType, String phase) {
        if (value == null || value.trim().length() == 0) {
            throw new DifyClientException("Dify " + clientType + " client " + phase
                    + " error: required field '" + fieldName + "' is blank");
        }
        return value.trim();
    }

    public static String normalizeServer(String server, String clientType, String phase) {
        String required = requireText(server, "server", clientType, phase);
        String normalized = DifyUrlUtils.normalizeServer(required);
        try {
            URI uri = URI.create(normalized);
            String scheme = uri.getScheme();
            if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) || uri.getHost() == null) {
                throw new IllegalArgumentException();
            }
            return normalized;
        } catch (IllegalArgumentException e) {
            throw new DifyClientException("Dify " + clientType + " client " + phase
                    + " error: required field 'server' is malformed", e);
        }
    }
}
