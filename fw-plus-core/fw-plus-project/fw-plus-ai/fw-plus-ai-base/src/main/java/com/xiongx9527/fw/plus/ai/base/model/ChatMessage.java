package com.xiongx9527.fw.plus.ai.base.model;

/**
 * 一条对话消息（角色 + 文本内容）。
 */
public final class ChatMessage {

    private final ChatRole role;

    private final String content;

    private ChatMessage(ChatRole role, String content) {
        this.role = role;
        this.content = content;
    }

    public static ChatMessage of(ChatRole role, String content) {
        return new ChatMessage(role, content);
    }

    public static ChatMessage system(String content) {
        return new ChatMessage(ChatRole.SYSTEM, content);
    }

    public static ChatMessage user(String content) {
        return new ChatMessage(ChatRole.USER, content);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage(ChatRole.ASSISTANT, content);
    }

    public ChatRole getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return "ChatMessage{role=" + role + ", content=" + content + '}';
    }
}
