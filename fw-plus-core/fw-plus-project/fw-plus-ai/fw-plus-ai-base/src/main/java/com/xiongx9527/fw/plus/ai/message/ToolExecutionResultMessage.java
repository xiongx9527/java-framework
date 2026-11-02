package com.xiongx9527.fw.plus.ai.message;

import java.util.Arrays;
import java.util.List;

public class ToolExecutionResultMessage implements ChatMessage {
    private final String id;
    private final String toolName;
    private final String text;
    public ToolExecutionResultMessage(String id, String toolName, String text) { this.id = id; this.toolName = toolName; this.text = text; }
    @Override public ChatMessageType type() { return ChatMessageType.TOOL_EXECUTION_RESULT; }
    @Override public List<Content> contents() { return Arrays.asList(TextContent.from(text)); }
    @Override public String text() { return text; }
    public String id() { return id; }
    public String toolName() { return toolName; }
    public static ToolExecutionResultMessage from(String id, String toolName, String text) { return new ToolExecutionResultMessage(id, toolName, text); }
}
