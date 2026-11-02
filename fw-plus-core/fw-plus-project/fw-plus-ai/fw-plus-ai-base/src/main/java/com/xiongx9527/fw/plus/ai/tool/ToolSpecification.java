package com.xiongx9527.fw.plus.ai.tool;

/** 工具声明（名称/描述/参数 JSON Schema），镜像 langchain4j 的 {@code ToolSpecification}。 */
public class ToolSpecification {
    private final String name;
    private final String description;
    private final String parameters;
    public ToolSpecification(String name, String description) { this(name, description, null); }
    public ToolSpecification(String name, String description, String parameters) { this.name = name; this.description = description; this.parameters = parameters; }
    public String name() { return name; }
    public String description() { return description; }
    public String parameters() { return parameters; }
}
