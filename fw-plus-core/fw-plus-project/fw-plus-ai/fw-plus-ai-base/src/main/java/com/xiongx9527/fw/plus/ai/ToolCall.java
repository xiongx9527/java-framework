package com.xiongx9527.fw.plus.ai;

/** 工具调用结果（回传消息用），配合 ToolExecutionResultMessage。 */
public class ToolCall {
    private final String id;
    private final String name;
    private final String result;
    public ToolCall(String id, String name, String result) { this.id = id; this.name = name; this.result = result; }
    public String id() { return id; }
    public String name() { return name; }
    public String result() { return result; }
}
