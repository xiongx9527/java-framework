package com.xiongx9527.fw.plus.ai.tool;

import com.xiongx9527.fw.plus.ai.internal.Json;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

/** 反射执行 {@code @Tool} 方法，镜像 langchain4j 1.23.0 的 {@code dev.langchain4j.service.tool.DefaultToolExecutor}。 */
public class DefaultToolExecutor implements ToolExecutor {
    private final Object target;
    private final Method method;
    public DefaultToolExecutor(Object target, Method method) { this.target = target; this.method = method; }
    @Override
    public String execute(ToolExecutionRequest request, Object memoryId) {
        try {
            Map<String, Object> args = Json.parseObject(request.arguments());
            Class<?>[] types = method.getParameterTypes();
            Parameter[] params = method.getParameters();
            Object[] values = new Object[types.length];
            for (int i = 0; i < types.length; i++) {
                values[i] = convert(args.get(params[i].getName()), types[i]);
            }
            method.setAccessible(true);
            Object result = method.invoke(target, values);
            return result == null ? "" : result.toString();
        } catch (Exception e) {
            throw new RuntimeException("Tool execution failed: " + method.getName(), e);
        }
    }
    private static Object convert(Object raw, Class<?> type) {
        if (raw == null) return null;
        if (type == String.class) return raw.toString();
        if (type == int.class || type == Integer.class) return ((Number) raw).intValue();
        if (type == long.class || type == Long.class) return ((Number) raw).longValue();
        if (type == double.class || type == Double.class) return ((Number) raw).doubleValue();
        if (type == float.class || type == Float.class) return ((Number) raw).floatValue();
        if (type == boolean.class || type == Boolean.class) return raw;
        return raw;
    }
}
