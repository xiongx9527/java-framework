package com.xiongx9527.fw.plus.ai.tool;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/** 从 {@code @Tool} 方法生成工具声明，镜像 langchain4j 1.23.0 的 {@code dev.langchain4j.agent.tool.ToolSpecifications}。 */
public final class ToolSpecifications {
    private ToolSpecifications() {}
    public static ToolSpecification toolSpecificationFrom(Method method) {
        Tool tool = method.getAnnotation(Tool.class);
        String name = (tool != null && !tool.name().isEmpty()) ? tool.name() : method.getName();
        String description = "";
        if (tool != null) {
            if (tool.value().length > 0) {
                StringBuilder d = new StringBuilder();
                for (String v : tool.value()) { if (d.length() > 0) d.append(' '); d.append(v); }
                description = d.toString();
            }
        }
        StringBuilder props = new StringBuilder();
        StringBuilder required = new StringBuilder();
        for (Parameter p : method.getParameters()) {
            P ann = p.getAnnotation(P.class);
            String pname = p.getName();
            if (props.length() > 0) props.append(',');
            props.append('"').append(pname).append("\":{\"type\":\"").append(jsonType(p.getType())).append('"');
            if (ann != null && !ann.value().isEmpty()) props.append(",\"description\":\"").append(escape(ann.value())).append('"');
            props.append('}');
            if (required.length() > 0) required.append(',');
            required.append('"').append(pname).append('"');
        }
        String parameters = "{\"type\":\"object\",\"properties\":{" + props + "},\"required\":[" + required + "]}";
        return new ToolSpecification(name, description, parameters);
    }
    private static String jsonType(Class<?> t) {
        if (t == int.class || t == Integer.class || t == long.class || t == Long.class) return "integer";
        if (t == boolean.class || t == Boolean.class) return "boolean";
        if (t == double.class || t == Double.class || t == float.class || t == Float.class) return "number";
        return "string";
    }
    private static String escape(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\""); }
}
