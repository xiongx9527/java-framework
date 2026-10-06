package com.xiongx9527.fw.plus.auth.core.util;

/**
 * 极简字符串工具，替代 Spring {@code StringUtils}。
 * <p>
 * 避免引入 spring-core 依赖，保持 auth-core 零 Spring 依赖。
 * </p>
 */
public final class StringUtils {

    private StringUtils() {
    }

    /**
     * 判断字符串是否非空且包含非空白字符。
     */
    public static boolean hasText(CharSequence str) {
        return str != null && str.length() > 0 && containsText(str);
    }

    private static boolean containsText(CharSequence str) {
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}