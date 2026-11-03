package com.xiongx9527.fw.plus.ai.base.model.content;

/**
 * 文本片段。
 */
public final class TextContent implements ContentPart {

    private final String text;

    private TextContent(String text) {
        this.text = text;
    }

    public static TextContent of(String text) {
        return new TextContent(text);
    }

    @Override
    public ContentType type() {
        return ContentType.TEXT;
    }

    @Override
    public String text() {
        return text;
    }

    @Override
    public String toString() {
        return "TextContent{text=" + text + '}';
    }
}
