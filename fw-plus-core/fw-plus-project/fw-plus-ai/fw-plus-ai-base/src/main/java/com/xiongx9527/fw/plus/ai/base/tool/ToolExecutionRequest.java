package com.xiongx9527.fw.plus.ai.base.tool;

/**
 * 一次工具调用请求（模型返回的 tool_call）。
 */
public final class ToolExecutionRequest {

    private final String id;

    private final String name;

    private final String arguments;

    public ToolExecutionRequest(String id, String name, String arguments) {
        this.id = id;
        this.name = name;
        this.arguments = arguments;
    }

    public static ToolExecutionRequest of(String name, String arguments) {
        return new ToolExecutionRequest(null, name, arguments);
    }

    /** 上游给的调用 id，用于把结果与请求配对；可能为 {@code null}。 */
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /** 参数 JSON（模型原始输出，未解析）。 */
    public String getArguments() {
        return arguments;
    }

    @Override
    public String toString() {
        return "ToolExecutionRequest{name=" + name + ", arguments=" + arguments + '}';
    }
}
