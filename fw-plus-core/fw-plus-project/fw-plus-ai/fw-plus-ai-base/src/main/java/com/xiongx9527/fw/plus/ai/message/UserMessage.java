package com.xiongx9527.fw.plus.ai.message;

import java.util.Arrays;
import java.util.List;

public class UserMessage implements ChatMessage {
    private final String text;
    private final String name;
    public UserMessage(String text) { this(text, null); }
    public UserMessage(String text, String name) { this.text = text; this.name = name; }
    @Override public ChatMessageType type() { return ChatMessageType.USER; }
    @Override public List<Content> contents() { return Arrays.asList(TextContent.from(text)); }
    @Override public String text() { return text; }
    public String name() { return name; }
    public static UserMessage from(String text) { return new UserMessage(text); }
    public static UserMessage from(String text, String name) { return new UserMessage(text, name); }
}
