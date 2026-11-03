package com.xiongx9527.fw.plus.ai.base.tool;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 从带 {@code @Tool} 注解的方法反射出工具规格。
 *
 * <p>参数结构按 JSON Schema 生成：{@code {type: object, properties: {...}, required: [...]}}，
 * adapter 可直接序列化进上游请求，不需要再加工。</p>
 */
public final class ToolSpecifications {

    private ToolSpecifications() {
    }

    /** 扫描目标对象的全部 {@code @Tool} 方法（含继承的 public 方法）。 */
    public static List<ToolSpecification> from(Object target) {
        List<ToolSpecification> specs = new ArrayList<ToolSpecification>();
        if (target == null) {
            return specs;
        }
        for (Method method : target.getClass().getMethods()) {
            Tool tool = method.getAnnotation(Tool.class);
            if (tool == null) {
                continue;
            }
            specs.add(of(method, tool));
        }
        return specs;
    }

    /** 单个方法的规格。 */
    public static ToolSpecification of(Method method, Tool tool) {
        String name = tool.name() == null || tool.name().trim().isEmpty() ? method.getName() : tool.name().trim();
        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        List<String> required = new ArrayList<String>();
        Parameter[] parameters = method.getParameters();
        for (Parameter parameter : parameters) {
            P annotation = parameter.getAnnotation(P.class);
            String paramName = annotation != null && annotation.name() != null && !annotation.name().trim().isEmpty()
                    ? annotation.name().trim() : parameter.getName();
            Map<String, Object> property = new LinkedHashMap<String, Object>();
            property.put("type", jsonType(parameter.getType()));
            if (annotation != null && annotation.value() != null && !annotation.value().trim().isEmpty()) {
                property.put("description", annotation.value());
            }
            properties.put(paramName, property);
            required.add(paramName);
        }
        Map<String, Object> schema = new LinkedHashMap<String, Object>();
        schema.put("type", "object");
        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }
        return ToolSpecification.builder()
                .name(name)
                .description(tool.value())
                .parameters(schema)
                .build();
    }

    private static String jsonType(Class<?> type) {
        if (type == String.class || type == char.class || type == Character.class) {
            return "string";
        }
        if (type == int.class || type == Integer.class || type == long.class || type == Long.class
                || type == short.class || type == Short.class || type == byte.class || type == Byte.class) {
            return "integer";
        }
        if (type == double.class || type == Double.class || type == float.class || type == Float.class) {
            return "number";
        }
        if (type == boolean.class || type == Boolean.class) {
            return "boolean";
        }
        if (type.isArray() || Iterable.class.isAssignableFrom(type)) {
            return "array";
        }
        return "object";
    }
}
