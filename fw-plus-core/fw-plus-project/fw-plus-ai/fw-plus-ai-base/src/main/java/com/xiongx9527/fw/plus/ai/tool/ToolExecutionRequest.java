package com.xiongx9527.fw.plus.ai.tool;

/** 工具调用请求，镜像 langchain4j 的 {@code ToolExecutionRequest}。 */
public class ToolExecutionRequest {
    private final String id;
    private final String name;
    private final String arguments;
    public ToolExecutionRequest(String id, String name, String arguments) { this.id = id; this.name = name; this.arguments = arguments; }
    public String id() { return id; }
    public String name() { return name; }
    public String arguments() { return arguments; }
    public static ToolExecutionRequest of(String name, String arguments) { return new ToolExecutionRequest(null, name, arguments); }
}
