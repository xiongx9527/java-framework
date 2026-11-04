package com.xiongx9527.fw.plus.ai.base.tool;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 反射式默认执行器：按工具名找到 {@code @Tool} 方法，解析参数后调用。
 *
 * <p>只做「参数 JSON → 方法入参」的最小转换（String / 整数 / 浮点 / 布尔），
 * 复杂对象参数应由工具方法自己接收字符串再解析——工具边界保持扁平，模型才好填。</p>
 */
public class DefaultToolExecutor implements ToolExecutor {

    private final Object target;

    private final ToolArgumentCodec codec;

    private final Map<String, Method> methods = new HashMap<String, Method>();

    public DefaultToolExecutor(Object target, ToolArgumentCodec codec) {
        if (target == null) {
            throw new IllegalArgumentException("tool target is required");
        }
        if (codec == null) {
            throw new IllegalArgumentException("tool argument codec is required");
        }
        this.target = target;
        this.codec = codec;
        for (Method method : target.getClass().getMethods()) {
            Tool tool = method.getAnnotation(Tool.class);
            if (tool == null) {
                continue;
            }
            String name = tool.name() == null || tool.name().trim().isEmpty()
                    ? method.getName() : tool.name().trim();
            methods.put(name, method);
        }
    }

    @Override
    public String execute(ToolExecutionRequest request) {
        if (request == null || request.getName() == null) {
            throw new ToolArgumentException("tool name is required");
        }
        Method method = methods.get(request.getName());
        if (method == null) {
            throw new ToolArgumentException("unknown tool: " + request.getName());
        }
        Map<String, Object> arguments;
        try {
            arguments = codec.decode(request.getArguments());
        } catch (Exception e) {
            throw new ToolArgumentException("failed to decode tool arguments: " + e.getMessage(), e);
        }
        Object[] args = bind(method, arguments == null ? new HashMap<String, Object>() : arguments);
        try {
            Object result = method.invoke(target, args);
            return codec.encode(result);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getTargetException() == null ? e : e.getTargetException();
            throw new ToolExecutionException("tool failed: " + request.getName() + " - " + cause.getMessage(), cause);
        } catch (IllegalAccessException e) {
            throw new ToolExecutionException("tool not accessible: " + request.getName(), e);
        }
    }

    private Object[] bind(Method method, Map<String, Object> arguments) {
        Class<?>[] types = method.getParameterTypes();
        java.lang.reflect.Parameter[] parameters = method.getParameters();
        Object[] args = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            P annotation = parameters[i].getAnnotation(P.class);
            String name = annotation != null && annotation.name() != null && !annotation.name().trim().isEmpty()
                    ? annotation.name().trim() : parameters[i].getName();
            Object raw = arguments.get(name);
            boolean required = annotation == null || annotation.required();
            if (raw == null) {
                if (required) {
                    throw new ToolArgumentException("missing required parameter: " + name);
                }
                args[i] = coerce(null, types[i]);
                continue;
            }
            if (annotation != null && annotation.pattern() != null && !annotation.pattern().trim().isEmpty()) {
                String text = String.valueOf(raw);
                if (!text.matches(annotation.pattern())) {
                    throw new ToolArgumentException("parameter " + name
                            + " does not match pattern " + annotation.pattern() + ": " + text);
                }
            }
            validateConstraints(name, raw, annotation);
            args[i] = coerce(raw, types[i]);
        }
        return args;
    }

    private Object coerce(Object value, Class<?> type) {
        if (value == null) {
            return type.isPrimitive() ? defaultPrimitive(type) : null;
        }
        if (type.isInstance(value)) {
            return value;
        }
        String text = String.valueOf(value);
        if (type == String.class) {
            return text;
        }
        if (type == int.class || type == Integer.class) {
            return Integer.valueOf(text);
        }
        if (type == long.class || type == Long.class) {
            return Long.valueOf(text);
        }
        if (type == double.class || type == Double.class) {
            return Double.valueOf(text);
        }
        if (type == float.class || type == Float.class) {
            return Float.valueOf(text);
        }
        if (type == boolean.class || type == Boolean.class) {
            return Boolean.valueOf(text);
        }
        return value;
    }


    private void validateConstraints(String name, Object raw, P annotation) {
        String text = String.valueOf(raw);
        int ml = annotation.minLength();
        if (ml >= 0 && text.length() < ml) {
            throw new ToolArgumentException("parameter " + name + " length must be >= " + ml + ": " + text);
        }
        int xl = annotation.maxLength();
        if (xl >= 0 && text.length() > xl) {
            throw new ToolArgumentException("parameter " + name + " length must be <= " + xl + ": " + text);
        }
        String minStr = annotation.min();
        if (minStr != null && !minStr.trim().isEmpty()) {
            Double bound = parseDoubleSafe(minStr);
            Double val = parseDoubleSafe(text);
            if (bound != null && val != null && val < bound) {
                throw new ToolArgumentException("parameter " + name + " must be >= " + bound + ": " + text);
            }
        }
        String maxStr = annotation.max();
        if (maxStr != null && !maxStr.trim().isEmpty()) {
            Double bound = parseDoubleSafe(maxStr);
            Double val = parseDoubleSafe(text);
            if (bound != null && val != null && val > bound) {
                throw new ToolArgumentException("parameter " + name + " must be <= " + bound + ": " + text);
            }
        }
        String shape = annotation.jsonShape();
        if (shape != null && !shape.trim().isEmpty()) {
            if ("object".equals(shape) && !isJsonObject(text)) {
                throw new ToolArgumentException("parameter " + name + " must be a JSON object: " + text);
            }
            if ("array".equals(shape) && !isJsonArray(text)) {
                throw new ToolArgumentException("parameter " + name + " must be a JSON array: " + text);
            }
        }
    }

    private static Double parseDoubleSafe(String s) {
        if (s == null) {
            return null;
        }
        try {
            return Double.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isJsonObject(String s) {
        return isJsonShape(s, '{', '}');
    }

    private static boolean isJsonArray(String s) {
        return isJsonShape(s, '[', ']');
    }

    private static boolean isJsonShape(String s, char open, char close) {
        if (s == null) {
            return false;
        }
        int start = 0;
        int end = s.length() - 1;
        while (start <= end && Character.isWhitespace(s.charAt(start))) {
            start++;
        }
        while (end >= start && Character.isWhitespace(s.charAt(end))) {
            end--;
        }
        if (start > end || s.charAt(start) != open || s.charAt(end) != close) {
            return false;
        }
        java.util.Deque<Character> stack = new java.util.ArrayDeque<Character>();
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i <= end; i++) {
            char c = s.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == (char) 92) {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == '{') {
                stack.push('}');
            } else if (c == '[') {
                stack.push(']');
            } else if (c == '}') {
                if (stack.isEmpty() || stack.pop() != '}') {
                    return false;
                }
            } else if (c == ']') {
                if (stack.isEmpty() || stack.pop() != ']') {
                    return false;
                }
            }
        }
        return stack.isEmpty() && !inString;
    }

    private Object defaultPrimitive(Class<?> type) {
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == double.class) {
            return 0.0;
        }
        if (type == float.class) {
            return 0.0f;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == char.class) {
            return ' ';
        }
        return null;
    }
}
