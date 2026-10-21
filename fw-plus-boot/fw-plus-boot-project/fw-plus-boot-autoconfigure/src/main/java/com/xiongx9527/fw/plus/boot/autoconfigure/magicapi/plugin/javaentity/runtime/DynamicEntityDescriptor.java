package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.runtime;

import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DynamicEntityDescriptor {

    private final long version;
    private final String resourceId;
    private final String sourceDigest;
    private final Class<?> type;
    private final Map<String, Method> setters;

    DynamicEntityDescriptor(long version, String resourceId, String sourceDigest, Class<?> type) {
        this.version = version;
        this.resourceId = resourceId;
        this.sourceDigest = sourceDigest;
        this.type = type;
        this.setters = discoverSetters(type);
    }

    public String name() {
        return type.getName();
    }

    public long version() {
        return version;
    }

    public String resourceId() {
        return resourceId;
    }

    public String sourceDigest() {
        return sourceDigest;
    }

    public Class<?> javaClass() {
        return type;
    }

    public Object create() {
        try {
            Constructor<?> constructor = type.getDeclaredConstructor();
            return constructor.newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException exception) {
            throw new IllegalStateException(I18nMessageResolver.resolveKey("fw.boot.javaentity.noAccessibleNoArgConstructor", type.getName()), exception);
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException(I18nMessageResolver.resolveKey("fw.boot.javaentity.noArgConstructorFailed", type.getName()),
                    exception.getCause());
        }
    }

    public Object create(Map<String, Object> values) {
        Object instance = create();
        for (Map.Entry<String, Object> entry : values == null
                ? Collections.<String, Object>emptyMap().entrySet() : values.entrySet()) {
            assign(instance, setters, entry.getKey(), entry.getValue());
        }
        return instance;
    }

    private void assign(Object instance, Map<String, Method> setters, String property, Object value) {
        Method setter = setters.get(property);
        try {
            if (setter != null) {
                setter.invoke(instance, convert(value, setter.getParameterTypes()[0], property));
                return;
            }
            Field field = findField(type, property);
            if (field == null || Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
                throw propertyError(property, "属性不存在或不可写", null);
            }
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            field.set(instance, convert(value, field.getType(), property));
        } catch (InvocationTargetException exception) {
            throw propertyError(property, "setter 执行失败", exception.getCause());
        } catch (IllegalAccessException exception) {
            throw propertyError(property, "属性不可访问", exception);
        }
    }

    private static Map<String, Method> discoverSetters(Class<?> type) {
        Map<String, Method> result = new LinkedHashMap<>();
        for (Method method : type.getMethods()) {
            if (Modifier.isStatic(method.getModifiers()) || method.getParameterTypes().length != 1) {
                continue;
            }
            String name = method.getName();
            if (!name.startsWith("set") || name.length() <= 3) {
                continue;
            }
            String property = decapitalize(name.substring(3));
            if (!result.containsKey(property)) {
                result.put(property, method);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static String decapitalize(String name) {
        if (name.length() > 1 && Character.isUpperCase(name.charAt(0))
                && Character.isUpperCase(name.charAt(1))) {
            return name;
        }
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private Object convert(Object value, Class<?> targetType, String property) {
        if (value == null) {
            if (targetType.isPrimitive()) {
                throw propertyError(property, "基本类型不能赋值为 null", null);
            }
            return null;
        }
        Class<?> boxedType = box(targetType);
        if (boxedType.isInstance(value)) {
            return value;
        }
        try {
            if (boxedType == String.class) {
                return String.valueOf(value);
            }
            if (Number.class.isAssignableFrom(boxedType)) {
                Number number = value instanceof Number ? (Number) value : Double.valueOf(String.valueOf(value));
                if (boxedType == Byte.class) return number.byteValue();
                if (boxedType == Short.class) return number.shortValue();
                if (boxedType == Integer.class) return number.intValue();
                if (boxedType == Long.class) return number.longValue();
                if (boxedType == Float.class) return number.floatValue();
                if (boxedType == Double.class) return number.doubleValue();
            }
            if (boxedType == Boolean.class) {
                String text = String.valueOf(value);
                if ("true".equalsIgnoreCase(text) || "false".equalsIgnoreCase(text)) {
                    return Boolean.valueOf(text);
                }
            }
            if (boxedType == Character.class && String.valueOf(value).length() == 1) {
                return String.valueOf(value).charAt(0);
            }
            if (boxedType.isEnum()) {
                @SuppressWarnings({"rawtypes", "unchecked"})
                Object enumValue = Enum.valueOf((Class<? extends Enum>) boxedType, String.valueOf(value));
                return enumValue;
            }
        } catch (RuntimeException exception) {
            throw propertyError(property, "值无法转换为 " + targetType.getName(), exception);
        }
        throw propertyError(property, "值无法转换为 " + targetType.getName(), null);
    }

    private IllegalArgumentException propertyError(String property, String reason, Throwable cause) {
        String message = "动态实体 " + type.getName() + " 属性 " + property + ": " + reason;
        return cause == null ? new IllegalArgumentException(message) : new IllegalArgumentException(message, cause);
    }

    private static Field findField(Class<?> current, String name) {
        Class<?> type = current;
        while (type != null && type != Object.class) {
            try {
                return type.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        return null;
    }

    private static Class<?> box(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        if (type == char.class) return Character.class;
        return type;
    }
}
