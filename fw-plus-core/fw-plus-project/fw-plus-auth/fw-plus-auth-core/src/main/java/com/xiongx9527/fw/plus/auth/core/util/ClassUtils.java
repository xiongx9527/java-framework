package com.xiongx9527.fw.plus.auth.core.util;

/**
 * 极简类加载检查工具，替代 Spring {@code ClassUtils.isPresent}。
 * <p>
 * 避免引入 spring-core 依赖，保持 auth-core 零 Spring 依赖。
 * </p>
 */
public final class ClassUtils {

    private ClassUtils() {
    }

    /**
     * 检查指定类是否存在于给定类加载器中。
     *
     * @param className  类全限定名
     * @param classLoader 类加载器，{@code null} 表示当前类的类加载器
     * @return 存在返回 {@code true}，否则 {@code false}
     */
    public static boolean isPresent(String className, ClassLoader classLoader) {
        if (className == null || className.trim().isEmpty()) {
            return false;
        }
        ClassLoader loader = classLoader != null ? classLoader : ClassUtils.class.getClassLoader();
        try {
            Class.forName(className, false, loader);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}