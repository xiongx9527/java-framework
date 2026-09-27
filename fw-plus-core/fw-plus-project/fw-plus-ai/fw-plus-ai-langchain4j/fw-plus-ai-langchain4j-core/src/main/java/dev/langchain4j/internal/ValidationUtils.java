package dev.langchain4j.internal;

import java.util.Collection;
import java.util.Map;

/** Java 8-compatible validation helpers retaining the complete 0.35 public surface. */
public final class ValidationUtils {
    private ValidationUtils() {
    }

    public static void ensureEq(Object lhs, Object rhs, String format, Object... args) {
        if (lhs == null ? rhs != null : !lhs.equals(rhs)) {
            throw new IllegalArgumentException(String.format(format, args));
        }
    }

    public static <T> T ensureNotNull(T value, String name) {
        if (value == null) throw new IllegalArgumentException(name + " must not be null");
        return value;
    }

    public static <T> T ensureNotNull(T value, String format, Object... args) {
        if (value == null) throw new IllegalArgumentException(String.format(format, args));
        return value;
    }

    public static <T extends Collection<?>> T ensureNotEmpty(T value, String name) {
        ensureNotNull(value, name);
        if (value.isEmpty()) throw new IllegalArgumentException(name + " must not be empty");
        return value;
    }

    public static <T> T[] ensureNotEmpty(T[] value, String name) {
        return ensureNotEmpty(value, "%s must not be empty", name);
    }

    public static <T> T[] ensureNotEmpty(T[] value, String format, Object... args) {
        if (value == null || value.length == 0) {
            throw new IllegalArgumentException(String.format(format, args));
        }
        return value;
    }

    public static <K, V> Map<K, V> ensureNotEmpty(Map<K, V> value, String name) {
        ensureNotNull(value, name);
        if (value.isEmpty()) throw new IllegalArgumentException(name + " must not be empty");
        return value;
    }

    public static String ensureNotEmpty(String value, String name) {
        if (value == null || value.isEmpty()) throw new IllegalArgumentException(name + " must not be empty");
        return value;
    }

    public static String ensureNotBlank(String value, String name) {
        ensureNotNull(value, name);
        if (value.trim().isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }

    public static void ensureTrue(boolean expression, String message) {
        if (!expression) throw new IllegalArgumentException(message);
    }

    public static int ensureGreaterThanZero(Integer value, String name) {
        ensureNotNull(value, name);
        if (value.intValue() <= 0) throw new IllegalArgumentException(name + " must be greater than zero");
        return value.intValue();
    }

    public static double ensureBetween(Double value, double min, double max, String name) {
        ensureNotNull(value, name);
        if (value.doubleValue() < min || value.doubleValue() > max) throw new IllegalArgumentException(name);
        return value.doubleValue();
    }

    public static int ensureBetween(Integer value, int min, int max, String name) {
        ensureNotNull(value, name);
        if (value.intValue() < min || value.intValue() > max) throw new IllegalArgumentException(name);
        return value.intValue();
    }

    public static long ensureBetween(Long value, long min, long max, String name) {
        ensureNotNull(value, name);
        if (value.longValue() < min || value.longValue() > max) throw new IllegalArgumentException(name);
        return value.longValue();
    }

    public static int ensureNotNegative(int value, String name) {
        if (value < 0) throw new IllegalArgumentException(name + " must not be negative");
        return value;
    }
}
