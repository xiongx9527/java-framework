package com.xiongx9527.fw.plus.ai.message;

import java.util.Arrays;
import java.util.List;

public class SystemMessage implements ChatMessage {
    private final String text;
    public SystemMessage(String text) { this.text = text; }
    @Override public ChatMessageType type() { return ChatMessageType.SYSTEM; }
    @Override public List<Content> contents() { return Arrays.asList(TextContent.from(text)); }
    @Override public String text() { return text; }
    public static SystemMessage from(String text) { return new SystemMessage(text); }
}
