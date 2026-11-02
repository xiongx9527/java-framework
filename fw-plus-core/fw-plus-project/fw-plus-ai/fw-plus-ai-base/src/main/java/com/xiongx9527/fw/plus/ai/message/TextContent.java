package com.xiongx9527.fw.plus.ai.message;

/** 文本消息内容，镜像 langchain4j 1.23.0 的 {@code dev.langchain4j.data.message.TextContent}。 */
public class TextContent implements Content {
    private final String text;
    public TextContent(String text) { this.text = text; }
    public String text() { return text; }
    @Override public ContentType type() { return ContentType.TEXT; }
    public static TextContent from(String text) { return new TextContent(text); }
}
