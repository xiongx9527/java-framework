package com.xiongx9527.fw.plus.ai.message;

import com.xiongx9527.fw.plus.ai.tool.ToolExecutionRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AiMessage implements ChatMessage {
    private final String text;
    private final List<ToolExecutionRequest> toolExecutionRequests;
    public AiMessage(String text) { this(text, Collections.<ToolExecutionRequest>emptyList()); }
    public AiMessage(String text, List<ToolExecutionRequest> toolExecutionRequests) {
        this.text = text == null ? "" : text;
        this.toolExecutionRequests = toolExecutionRequests == null ? Collections.<ToolExecutionRequest>emptyList() : new ArrayList<>(toolExecutionRequests);
    }
    @Override public ChatMessageType type() { return ChatMessageType.AI; }
    @Override public List<Content> contents() { return Arrays.asList(TextContent.from(text)); }
    @Override public String text() { return text; }
    public List<ToolExecutionRequest> toolExecutionRequests() { return Collections.unmodifiableList(toolExecutionRequests); }
    public static AiMessage from(String text) { return new AiMessage(text); }
    public static AiMessage from(String text, List<ToolExecutionRequest> requests) { return new AiMessage(text, requests); }
}
