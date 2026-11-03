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
            throw new ToolExecutionException("tool name is required");
        }
        Method method = methods.get(request.getName());
        if (method == null) {
            throw new ToolExecutionException("unknown tool: " + request.getName());
        }
        Map<String, Object> arguments = codec.decode(request.getArguments());
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
            args[i] = coerce(arguments.get(name), types[i]);
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
