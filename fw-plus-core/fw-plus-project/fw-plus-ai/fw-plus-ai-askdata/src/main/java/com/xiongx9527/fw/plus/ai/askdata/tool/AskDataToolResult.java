package com.xiongx9527.fw.plus.ai.askdata.tool;

/** 工具结果。 */
public final class AskDataToolResult { private final String content; private AskDataToolResult(String content) { this.content = content; } public static AskDataToolResult success(String content) { return new AskDataToolResult(content); } public String getContent() { return content; } }
